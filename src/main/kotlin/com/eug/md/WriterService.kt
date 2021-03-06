package com.eug.md

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class WriterService(outFilePath: Path, private val maxResourceDownloadTaskNumber: Int) {

    private val resourceDownloadTaskQueue: BlockingQueue<String> = LinkedBlockingQueue()
    private val out = outFilePath.toFile().printWriter()
    private val alreadyWrittenResourceDownloadTask: MutableSet<String> = HashSet()
    private val writerThread = WriterThread()
    private var currentResourceDownloadTaskNumber = 1

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
        resourceDownloadTaskQueue.put(linkLine)
    }

    inner class WriterThread : Thread("resource-task-writing-thread") {

        override fun run() {
            try {
                out.use {
                    while (true) {
                        val link = resourceDownloadTaskQueue.take()
                        if (alreadyWrittenResourceDownloadTask.contains(link)) {
                            continue
                        }

                        log.debug("Writing {} resource download task line {} to out",
                                currentResourceDownloadTaskNumber, link)

                        out.println(link)
                        terminal.statusLine(
                                "$currentResourceDownloadTaskNumber / $maxResourceDownloadTaskNumber links written"
                        )
                        alreadyWrittenResourceDownloadTask.add(link)

                        if (++currentResourceDownloadTaskNumber == maxResourceDownloadTaskNumber + 1) {
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
            resourceDownloadTaskQueue.clear()
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WriterService::class.java)
        private val terminal = Terminal()
    }
}