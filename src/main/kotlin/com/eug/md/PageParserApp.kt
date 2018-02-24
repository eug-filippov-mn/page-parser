package com.eug.md

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.xenomachina.argparser.ArgParser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.*
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    PageParserApp(args).run()
}

class PageParserApp(args: Array<String>) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(PageParserApp::class.java)

        private const val RESOURCE_LINKS_QUEUE_SIZE = 100
    }

    private val alreadyVisitedPageLinks: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
    private val resourceLinksQueue: RepeatableInsertQueue<String> = RepeatableInsertQueue(
            queue = LinkedBlockingQueue(RESOURCE_LINKS_QUEUE_SIZE),
            insertionTryDelay = Delay(TimeUnit.MILLISECONDS, 100)
    )

    private val options: Options
    private val executor: ExecutorService
    private val outWriter: OutWriter

    init {
        try {
            options = Options(ArgParser(args))
            outWriter = OutWriter(options, resourceLinksQueue)

            val threadFactory = ThreadFactoryBuilder().setNameFormat("app-pool thrd-%d").build()
            executor = Executors.newFixedThreadPool(options.threadsNumber, threadFactory)
        } catch (e: Exception) {
            log.error(e.message, e)
            System.err.println(e.message)
            exitProcess(-1)
        }
    }

    fun run() {
        outWriter.start()

        try {
            val startUrl = "http://www.sp-fan.ru"
            processPages(startUrl)

            while (!outWriter.maxLinksNumberExceed) {
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

    private fun processPages(pageUrl: String) {
        if (alreadyVisitedPageLinks.contains(pageUrl) || outWriter.maxLinksNumberExceed) {
            return
        }

        try {
            MDC.put("src", pageUrl)

            log.debug("Fetching {}...", pageUrl)
            val doc = Jsoup.connect(pageUrl).get()
            alreadyVisitedPageLinks.add(pageUrl)

            val (pageLinkElements, resourceLinkElements) = extractLinkElements(doc)

            for (element in resourceLinkElements) {
                val resourceLink = ElementFormatter.formatResourceLinkOf(element)
                if (resourceLink == null || outWriter.maxLinksNumberExceed) {
                    return
                }

                resourceLinksQueue.tryToInsertUntil(resourceLink, untilCondition = outWriter.maxLinksNumberExceed)
            }

            pageLinkElements
                    .map { element -> element.attr("abs:href") }
                    .filterNot(alreadyVisitedPageLinks::contains)
                    .forEach {
                        executor.execute({processPages(it)})
                    }

        } catch (e: Exception) {
            log.error(e.message, e)
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
