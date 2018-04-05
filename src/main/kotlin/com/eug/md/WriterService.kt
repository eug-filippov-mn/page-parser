package com.eug.md

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class WriterService(outFilePath: Path, private val maxResourceDownloadTaskNumber: Int) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(WriterService::class.java)
        private const val RESOURCE_DOWNLOAD_TASK_QUEUE = 100
    }

    private val resourceDownloadTaskQueue: BlockingQueue<String> = ArrayBlockingQueue(RESOURCE_DOWNLOAD_TASK_QUEUE)
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

    inner class WriterThread : Thread("app-writing-thread") {

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
}