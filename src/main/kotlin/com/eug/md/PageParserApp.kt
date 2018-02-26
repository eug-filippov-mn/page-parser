package com.eug.md

import com.eug.md.util.threadPoolExecutor
import com.xenomachina.argparser.ArgParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import kotlin.collections.HashSet
import kotlin.math.min
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    PageParserApp(args).run()
}

class PageParserApp(args: Array<String>) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(PageParserApp::class.java)

        private const val RESOURCE_LINKS_QUEUE_SIZE = 100
        private const val EXECUTOR_QUEUE_SIZE = 2000
        private const val PAGE_LINKS_TO_VISIT_QUEUE_SIZE = 100_000
    }

    private val alreadyVisitedPageLinks: MutableSet<String> = HashSet()
    private val pageLinksToVisitQueue: Queue<String> = ArrayBlockingQueue(PAGE_LINKS_TO_VISIT_QUEUE_SIZE)
    private val resourceLinksQueue: RepeatableInsertQueue<String> = RepeatableInsertQueue(
            queue = ArrayBlockingQueue(RESOURCE_LINKS_QUEUE_SIZE),
            insertionTryDelay = Delay(TimeUnit.MILLISECONDS, 100)
    )

    private val options: Options
    private val executor: ThreadPoolExecutor
    private val outWriter: OutWriter

    init {
        try {
            options = Options(ArgParser(args))
            outWriter = OutWriter(options, resourceLinksQueue)

            val executorQueueSize = min(options.maxLinksNumber, EXECUTOR_QUEUE_SIZE)
            executor = threadPoolExecutor(
                    numberOfThreads = options.threadsNumber,
                    threadsNameFormat = "app-pool thrd-%d",
                    workQueue = ArrayBlockingQueue(executorQueueSize)
            )
        } catch (e: Exception) {
            log.error(e.message, e)
            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    fun run() {
        try {
            startUp()
            //TODO proper shutdown on app thread exception

            //todo simplify or extract loop logic
            while (!outWriter.maxLinksNumberExceed) {
                while (executor.queue.remainingCapacity() != 0
                        && pageLinksToVisitQueue.isNotEmpty()
                        && !outWriter.maxLinksNumberExceed) {

                    val pageUrl = pageLinksToVisitQueue.poll()
                    if (alreadyVisitedPageLinks.contains(pageUrl)) {
                        continue
                    }
                    alreadyVisitedPageLinks.add(pageUrl)
                    CompletableFuture
                            .supplyAsync(Supplier { processPages(pageUrl) }, executor)
                            .thenApply { pageLinks ->
                                log.debug("pageLinks {}", pageLinks)
                                pageLinks.filterNot(alreadyVisitedPageLinks::contains)
                                        .forEach { pageLinksToVisitQueue.offer(it) }
                            }
                }
                TimeUnit.MILLISECONDS.sleep(500)
            }

            log.debug("Links max number exceed, closing page parser")
            close()

        } catch (e: Exception) {
            log.error(e.message, e)
            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    private fun startUp() {
        val startUrl = "http://www.sp-fan.ru"
        outWriter.start()
        pageLinksToVisitQueue.add(startUrl)
    }

    private fun processPages(pageUrl: String): List<String> {
        try {
            MDC.put("src", pageUrl)

            log.debug("Fetching {}...", pageUrl)
            val doc = Jsoup.connect(pageUrl).get()
            val (pageLinkElements, resourceLinkElements) = extractLinkElements(doc)

            for (element in resourceLinkElements) {
                val resourceLink = ElementFormatter.formatResourceLinkOf(element)

                if (resourceLink == null) {
                    continue
                }

                if (outWriter.maxLinksNumberExceed) {
                    return emptyList()
                }

                resourceLinksQueue.tryToInsertUntil(resourceLink, untilCondition = outWriter.maxLinksNumberExceed)
            }

            return pageLinkElements.map { element -> element.attr("abs:href") }

        } catch (e: Exception) {
            log.error(e.message, e)
            return emptyList()
        } finally {
            MDC.clear()
        }
    }

    private fun extractLinkElements(doc: Document): Pair<Elements, Elements> {
        val pageLinks = doc.select("a[href]")
        log.debug("Page links {}", pageLinks.size)

        val resourceLinks = doc.select("img[src],link[href],script[src]")
        log.debug("Resource links {}", resourceLinks.size)

        return Pair(pageLinks, resourceLinks)
    }

    private fun close() {
        executor.shutdown()
        outWriter.join()
        executor.awaitTermination(1, TimeUnit.MINUTES)
    }
}
