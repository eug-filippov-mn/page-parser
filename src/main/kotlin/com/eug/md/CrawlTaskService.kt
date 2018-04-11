package com.eug.md

import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.*

class CrawlTask(
        private val pageUrl: String,
        private val resourceDownloadTaskConsumer: (String) -> Unit,
        private val pageUrlToVisitConsumer: (String) -> Unit)
    : Runnable {

    override fun run() {
        try {
            MDC.put("uuid", UUID.randomUUID().toString())

            log.debug("Fetching {}...", pageUrl)
            val doc = Jsoup.connect(pageUrl).get()
            val (pageLinkElements, resourceLinkElements) = extractLinkElements(doc)

            log.debug("Notify about resource resource download tasks")
            resourceLinkElements
                    .mapNotNull(ResourceDownloadTaskExtractor::extractFrom)
                    .forEach(resourceDownloadTaskConsumer::invoke)

            log.debug("Notify about page urls to visit")
            pageLinkElements
                    .map { element -> element.attr("abs:href") }
                    .forEach(pageUrlToVisitConsumer::invoke)

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
    }

    private fun extractLinkElements(doc: Document): Pair<Elements, Elements> {
        val pageLinks = doc.select("a[href]")
        log.debug("Page links {}", pageLinks.size)

        val resourceLinks = doc.select("img[src],link[href],script[src]")
        log.debug("Resource links {}", resourceLinks.size)

        return Pair(pageLinks, resourceLinks)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(CrawlTask::class.java)
    }
}

class CrawlTaskService(threadsNumber: Int) {

    private val semaphore = Semaphore(TASKS_NUMBER_BOUND)
    private val crawlTasksExecutor = Executors.newFixedThreadPool(
            threadsNumber,
            ThreadFactoryBuilder().setNameFormat("crawl thrd-%d").build()
    )

    fun execute(task: CrawlTask) {
        semaphore.acquire()
        try {
            crawlTasksExecutor.submit {
                try {
                    task.run()
                } finally {
                    semaphore.release()
                }
            }
        } catch (e: RejectedExecutionException) {
            log.warn("Task was rejected {}", task, e)
            semaphore.release()
        }
    }

    fun stop() {
        MoreExecutors.shutdownAndAwaitTermination(crawlTasksExecutor, 1, TimeUnit.MINUTES)
    }

    private companion object {
        private const val TASKS_NUMBER_BOUND = 200

        private val log: Logger = LoggerFactory.getLogger(CrawlTaskService::class.java)
    }
}