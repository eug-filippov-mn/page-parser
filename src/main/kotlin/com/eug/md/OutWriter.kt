package com.eug.md

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet

class OutWriter(options: Options, private val resourceLinksQueue: Queue<String>) : Thread() {

    companion object {
        val log: Logger = LoggerFactory.getLogger(OutWriter::class.java)
    }

    private val maxLinksNumber = options.maxLinksNumber
    private val out = options.outFilePath.toFile().printWriter()
    private val alreadyWrittenResourceLinks: MutableSet<String> = HashSet()
    private var currentLinkNumber = 1L

    @Volatile var maxLinksNumberExceed = false
        private set

    init {
        name = "app-writing-thread"
    }

    private fun writeToOut() {
        while (!maxLinksNumberExceed) {
            log.debug("Queue size {}", resourceLinksQueue.size)

            var link = resourceLinksQueue.poll()
            var queueIsEmpty = link == null

            while (!queueIsEmpty
                    && !link.isBlank()
                    && !maxLinksNumberExceed
                    && !alreadyWrittenResourceLinks.contains(link)) {

                log.debug("Write {} link {} to out", currentLinkNumber, link)

                out.println(link)
                maxLinksNumberExceed = ++currentLinkNumber > maxLinksNumber
                alreadyWrittenResourceLinks.add(link)
                link = resourceLinksQueue.poll()
                queueIsEmpty = link == null
            }

            if (queueIsEmpty) {
                TimeUnit.MILLISECONDS.sleep(100)
            }
        }
        out.flush()
        log.debug("Stopping write thread")
    }

    override fun run() {
        log.debug("Running writing thread")
        writeToOut()
    }
}