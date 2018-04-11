package com.eug.md.arguments

import org.apache.commons.cli.*

object Opts {
    val threadsNumber: Option =
            Option.builder("n")
                    .longOpt("threads-number")
                    .argName("THREADS_NUMBER")
                    .desc("Number of threads for concurrent pages parsing")
                    .hasArg()
                    .required()
                    .build()

    val outFilePath: Option =
            Option.builder("o")
                    .longOpt("out-file-path")
                    .argName("OUT_FILE_PATH")
                    .desc("Path to out file in which links will be written")
                    .hasArg()
                    .required()
                    .build()

    val maxLinksNumber: Option =
            Option.builder("m")
                    .longOpt("max-links-number")
                    .argName("MAX_LINKS_NUMBER")
                    .desc("Max links number to write to file")
                    .hasArg()
                    .required()
                    .build()

    val startUrl: Option =
            Option.builder("u")
                    .longOpt("start-url")
                    .argName("START_URL")
                    .desc("Url to page from which parsing will be started")
                    .hasArg()
                    .required()
                    .build()

    val help: Option =
            Option.builder("h")
                    .longOpt("help")
                    .desc("Print help information")
                    .build()

    fun all(): Options =
            Options()
                    .addOption(threadsNumber)
                    .addOption(outFilePath)
                    .addOption(maxLinksNumber)
                    .addOption(startUrl)
                    .addOption(help)

    fun allExceptHelp(): Options =
            Options()
                    .addOption(threadsNumber)
                    .addOption(outFilePath)
                    .addOption(maxLinksNumber)
                    .addOption(startUrl)

    fun help(): Options = Options().addOption(help)

}