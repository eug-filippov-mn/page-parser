package com.eug.md

import com.eug.md.util.threadPoolExecutor
import com.google.common.util.concurrent.MoreExecutors
import com.xenomachina.argparser.ArgParser
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.*
import java.util.function.Supplier
import kotlin.math.min
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    PageParserApp(args).run()
}

class PageParserApp(args: Array<String>) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(PageParserApp::class.java)

        private const val EXECUTOR_QUEUE_SIZE = 2000
        private const val PAGE_LINKS_TO_VISIT_QUEUE_SIZE = 100_000
    }

    private val alreadyVisitedPageLinks: MutableSet<String> = HashSet()
    private val pageLinksToVisitQueue: BlockingQueue<String> = ArrayBlockingQueue(PAGE_LINKS_TO_VISIT_QUEUE_SIZE)
    private val options: Options
    private val executor: ThreadPoolExecutor
    private val writerService: WriterService

    init {
        try {
            options = Options(ArgParser(args))
            writerService = WriterService(options.outFilePath, options.maxLinksNumber)

            val executorQueueSize = min(options.maxLinksNumber, EXECUTOR_QUEUE_SIZE)
            executor = threadPoolExecutor(
                    numberOfThreads = options.threadsNumber,
                    threadsNameFormat = "app-pool thrd-%d",
                    workQueue = ArrayBlockingQueue(executorQueueSize)
            )
        } catch (e: Exception) {
            closeAppQuietly()
            log.error(e.message, e)

            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    fun run() {
        try {
            startUp()
            while (!writerService.stopped) {

                while (executor.queue.remainingCapacity() != 0 && !writerService.stopped) {
                    val pageUrl = pageLinksToVisitQueue.take()
                    if (alreadyVisitedPageLinks.contains(pageUrl)) {
                        continue
                    }
                    alreadyVisitedPageLinks.add(pageUrl)

                    CompletableFuture
                            .supplyAsync(Supplier { processPages(pageUrl) }, executor)
                            .thenApply { pageLinks ->
                                pageLinks.filterNot(alreadyVisitedPageLinks::contains)
                                        .forEach { pageLinksToVisitQueue.offer(it) }
                            }
                }

                TimeUnit.MILLISECONDS.sleep(500)
            }

            log.debug("Links max number exceed, closing page parser")
            closeExecutor()
        } catch (e: Exception) {
            closeAppQuietly()
            log.error(e.message, e)

            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    private fun startUp() {
        writerService.start()
        pageLinksToVisitQueue.add(options.startPageUrl)
    }

    private fun processPages(pageUrl: String): List<String> {
        try {
            MDC.put("src", pageUrl)
            if (writerService.stopped) {
                return emptyList()
            }

            log.debug("Fetching {}...", pageUrl)
            val doc = Jsoup.connect(pageUrl).get()
            val (pageLinkElements, resourceLinkElements) = extractLinkElements(doc)

            for (resourceLinkElement in resourceLinkElements) {
                if (writerService.stopped) {
                    return emptyList()
                }

                val downloadTaskLine = DownloadTaskLineExtractor.extractFrom(resourceLinkElement)
                if (downloadTaskLine == null) {
                    continue
                }
                writerService.write(downloadTaskLine)
            }

            return pageLinkElements.map { element -> element.attr("abs:href") }

        } catch (e: HttpStatusException) {
            log.error("Unexpected http error - {}. {}", e.statusCode, e.message)

        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            log.error(e.message, e)
        } finally {
            MDC.clear()
        }
        return emptyList()
    }

    private fun extractLinkElements(doc: Document): Pair<Elements, Elements> {
        val pageLinks = doc.select("a[href]")
        log.debug("Page links {}", pageLinks.size)

        val resourceLinks = doc.select("img[src],link[href],script[src]")
        log.debug("Resource links {}", resourceLinks.size)

        return Pair(pageLinks, resourceLinks)
    }

    private fun closeExecutor() {
        MoreExecutors.shutdownAndAwaitTermination(executor, 1, TimeUnit.MINUTES)
    }

    private fun closeAppQuietly() {
        try {
            writerService.stop()
            closeExecutor()
        } catch (e: Exception) {
            log.error("Threads closing error", e)
        }
    }
}
