package com.eug.md

import com.xenomachina.argparser.ArgParser
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    PageParserApp.run(args)
}

class PageParserApp private constructor(private val options: Options) : Closeable {

    private val writerService = WriterService(options.outFilePath, options.maxLinksNumber)
    private val crawlTaskService = CrawlTaskService(options.threadsNumber)
    private val alreadyVisitedPageUrls = mutableSetOf<String>()
    private val pageUrlsToVisitQueue = ArrayBlockingQueue<String>(2000)

    fun run() {
        writerService.start()

        var pageUrlToVisit: String = options.startPageUrl
        while (!writerService.stopped) {
            if (alreadyVisitedPageUrls.contains(pageUrlToVisit)) {
                pageUrlToVisit = pageUrlsToVisitQueue.take()
                continue
            }

            val task = CrawlTask(
                    pageUrl = pageUrlToVisit,
                    resourceDownloadTaskConsumer = writerService::write,
                    pageUrlToVisitConsumer = { pageUrlsToVisitQueue.offer(it) }
            )
            alreadyVisitedPageUrls += pageUrlToVisit
            crawlTaskService.execute(task)
            pageUrlToVisit = pageUrlsToVisitQueue.take()
        }
        log.debug("Links max number exceed, closing page parser")
    }

    override fun close() {
        writerService.stop()
        crawlTaskService.stop()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PageParserApp::class.java)

        fun run(args: Array<String>) {
            try {
                val options = Options(ArgParser(args))
                PageParserApp(options).use { it.run() }
            } catch (e: Exception) {
                log.error(e.message, e)

                System.err.println(e.message)
                exitProcess(-1)
            }
        }
    }
}
