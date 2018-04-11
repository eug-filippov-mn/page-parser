package com.eug.md.settings

import com.eug.md.NotValidOptionsException
import com.eug.md.arguments.Opts
import com.eug.md.utils.getOptionValue
import org.apache.commons.cli.CommandLine
import org.apache.commons.validator.routines.UrlValidator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class Settings(
        val threadsNumber: Int,
        val outFilePath: Path,
        val maxLinksNumber: Int,
        val startPageUrl: String) {

    companion object {
        private val urlValidator = UrlValidator(arrayOf("http","https"))

        fun from(commandLine: CommandLine): Settings {
            return Settings(
                    threadsNumber = parseThreadNumber(commandLine.getOptionValue(Opts.threadsNumber)),
                    outFilePath = parseOutFilePath(commandLine.getOptionValue(Opts.outFilePath)),
                    maxLinksNumber = parseMaxLinksNumber(commandLine.getOptionValue(Opts.maxLinksNumber)),
                    startPageUrl = parseStartPageUrl(commandLine.getOptionValue(Opts.startUrl))
            )
        }

        private fun parseThreadNumber(threadNumberArgValue: String): Int =
                parsePositiveNumberArg(threadNumberArgValue, Opts.threadsNumber.argName)

        private fun parseMaxLinksNumber(maxLinksNumberArgValue: String): Int =
                parsePositiveNumberArg(maxLinksNumberArgValue, Opts.maxLinksNumber.argName)

        private fun parsePositiveNumberArg(argValue: String, argName: String): Int {
            try {
                val number = argValue.toInt()
                if (number <= 0) {
                    throw NotValidOptionsException("$argName must be a positive")
                }
                return number
            } catch (e: NumberFormatException) {
                throw NotValidOptionsException("$argName must be a number", cause = e)
            }
        }

        private fun parseOutFilePath(outFilePathArgValue: String): Path {
            val outFilePath = Paths.get(outFilePathArgValue)
            if (!Files.exists(outFilePath)) {
                throw NotValidOptionsException("${Opts.outFilePath.argName} is not exists")
            }
            if (Files.isDirectory(outFilePath)) {
                throw NotValidOptionsException("${Opts.outFilePath.argName} must be a file, not a directory")
            }
            if (!Files.isWritable(outFilePath)) {
                throw NotValidOptionsException("${Opts.outFilePath.argName} isn't writable")
            }
            return outFilePath
        }

        private fun parseStartPageUrl(startPageUrlArgValue: String): String =
            if (urlValidator.isValid(startPageUrlArgValue)) {
                startPageUrlArgValue
            } else {
                throw NotValidOptionsException("${Opts.startUrl.argName} must be a valid http or https url")
            }
    }
}