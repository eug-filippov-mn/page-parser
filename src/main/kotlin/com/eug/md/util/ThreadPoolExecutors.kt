package com.eug.md.util

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

fun threadPoolExecutor(
        numberOfThreads: Int,
        threadsNameFormat: String,
        keepAliveTime: Long = 0L,
        keepAliveTimeUnit: TimeUnit = TimeUnit.MILLISECONDS,
        workQueue: BlockingQueue<Runnable>
): ThreadPoolExecutor {

    val threadFactory = ThreadFactoryBuilder().setNameFormat(threadsNameFormat).build()
    return ThreadPoolExecutor(
            numberOfThreads,
            numberOfThreads,
            keepAliveTime,
            keepAliveTimeUnit,
            workQueue,
            threadFactory
    )
}