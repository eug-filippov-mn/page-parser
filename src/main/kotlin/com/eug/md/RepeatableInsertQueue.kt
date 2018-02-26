package com.eug.md

import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit


data class Delay(val timeUnit: TimeUnit, val duration: Long)

class RepeatableInsertQueue<E>(
        private val queue: BlockingQueue<E>,
        private val insertionTryDelay: Delay)
    : BlockingQueue<E> by queue {

    fun tryToInsertUntil(element: E, untilCondition: Boolean) {
        var elementInserted: Boolean
        while (true) {
            elementInserted = queue.offer(element, insertionTryDelay.duration, insertionTryDelay.timeUnit)
            if (elementInserted || untilCondition) {
                return
            }
        }
    }
}