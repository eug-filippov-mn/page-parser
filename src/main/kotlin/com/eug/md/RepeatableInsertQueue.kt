package com.eug.md

import java.util.*
import java.util.concurrent.TimeUnit


data class Delay(val timeUnit: TimeUnit, val duration: Long)

class RepeatableInsertQueue<E>(private val queue: Queue<E>, private val insertionTryDelay: Delay) : Queue<E> by queue {

    fun tryToInsertUntil(element: E, untilCondition: Boolean) {
        var elementInserted = queue.offer(element)
        while (!elementInserted && !untilCondition) {
            insertionTryDelay.timeUnit.sleep(insertionTryDelay.duration)
            elementInserted = queue.offer(element)
        }
    }
}