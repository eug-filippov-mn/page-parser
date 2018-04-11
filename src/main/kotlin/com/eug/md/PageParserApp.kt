package com.eug.md

import com.eug.md.arguments.ArgParser
import com.eug.md.settings.Settings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    PageParserApp.run(args)
}

class PageParserApp private constructor(private val settings: Settings) : Closeable {

    private val writerService = WriterService(settings.outFilePath, settings.maxLinksNumber)
    private val crawlTaskService = CrawlTaskService(settings.threadsNumber)
    private val alreadyVisitedPageUrls = mutableSetOf<String>()
    private val pageUrlsToVisitQueue = ArrayBlockingQueue<String>(2000)

    fun run() {
        writerService.start()

        var pageUrlToVisit: String = settings.startPageUrl
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

        private val terminal: Terminal = Terminal()

        fun run(args: Array<String>) {
            try {

                if (ArgParser.containsHelp(args)) {
                    terminal.printHelp()
                    exitProcess(0)
                }

                terminal.statusLine("Parsings arguments")
                val commandLine = ArgParser.parse(args)
                val settings = Settings.from(commandLine)
                log.debug("App settings from parsed args {}", settings)

                terminal.statusLine("Starting application")
                PageParserApp(settings).use { it.run() }
            } catch (e: Exception) {
                terminal.printError(e.message)
                log.error(e.message, e)
                exitProcess(-1)
            }
        }
    }
}
