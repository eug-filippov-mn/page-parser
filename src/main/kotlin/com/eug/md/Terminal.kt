package com.eug.md

import com.eug.md.arguments.Opts
import org.apache.commons.cli.HelpFormatter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintStream

class Terminal {
    private val out: PrintStream = System.out
    private val err: PrintStream = System.err
    private val helpFormatter = HelpFormatter()

    fun statusLine(message: String) {
        val formattedMessage = "\r> ${message.padEnd(STATUS_LINE_LENGTH)}"
        if (log.isDebugEnabled) {
            log.debug(formattedMessage)
        } else {
            out.print(formattedMessage)
        }
    }

    fun printHelp() {
        helpFormatter.printHelp("page-parser [OPTION]...", Opts.all())
    }

    fun printError(message: String?) {
        val messageToPrint = message ?: "Unexpected error"

        if (log.isDebugEnabled) {
            log.debug(messageToPrint)
        } else {
            err.print("\r")
            err.println(messageToPrint)
        }
    }

    fun close() {
        out.print("\r")
    }

    companion object {
        private const val STATUS_LINE_LENGTH = 100
        private val log: Logger = LoggerFactory.getLogger(Terminal::class.java)
    }
}