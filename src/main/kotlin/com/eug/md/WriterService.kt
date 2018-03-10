package com.eug.md

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class WriterService(outFilePath: Path, private val maxLinksNumber: Int) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(WriterService::class.java)
        private const val RESOURCE_LINKS_QUEUE_SIZE = 100
    }

    private val resourceLinksQueue: BlockingQueue<String> = ArrayBlockingQueue(RESOURCE_LINKS_QUEUE_SIZE)
    private val out = outFilePath.toFile().printWriter()
    private val alreadyWrittenResourceLinks: MutableSet<String> = HashSet()
    private val writerThread = WriterThread()
    private var currentLinkNumber = 1

    @Volatile var stopped = false
        private set

    fun start() {
        log.debug("Starting write service")
        writerThread.start()
    }

    fun stop() {
        log.debug("Stopping write service")
        writerThread.interrupt()
    }

    fun write(linkLine: String) {
        require(!stopped) {"Writer service already stopped"}
        resourceLinksQueue.put(linkLine)
    }

    inner class WriterThread : Thread("app-writing-thread") {

        override fun run() {
            try {
                out.use {
                    while (true) {
                        val link = resourceLinksQueue.take()
                        if (alreadyWrittenResourceLinks.contains(link)) {
                            continue
                        }
                        if (link.contains(System.getProperty("line.separator"))) {
                            log.debug("Line {} contains separator", link)
                        }
                        log.debug("Write {} link {} to out", currentLinkNumber, link)

                        out.println(link)
                        //todo escape \n symbol
                        alreadyWrittenResourceLinks.add(link)

                        if (++currentLinkNumber == maxLinksNumber + 1) {
                            Thread.currentThread().interrupt()
                        }
                    }
                }
            } catch (e: InterruptedException) {
                log.debug("Stop request received. Stopping writer service and clear the queue")
                close()
            } catch (e: Exception) {
                log.error("Unexpected exception", e)
                close()
            }
        }

        private fun close() {
            stopped = true
            resourceLinksQueue.clear()
        }
    }
}