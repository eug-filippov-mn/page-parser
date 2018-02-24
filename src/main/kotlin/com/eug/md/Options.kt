package com.eug.md

import com.xenomachina.argparser.ArgParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Options(parser: ArgParser) {
    val threadsNumber : Int by parser
            .storing("-n", "--threads-number", help = "Number of threads for concurrent downloading") {
                this.toInt()
            }
            .addValidator {
                if (value < 0) {
                    throw RuntimeException("${this.errorName} can't be negative or zero")
                }
            }

    val outFilePath: Path by parser
            .storing("-o", "--links-file-path", help = "Path to out file in which links will be saved") {
                Paths.get(this)
            }
            .addValidator {
                if (Files.isDirectory(value)) {
                    throw RuntimeException("${this.errorName} point to directory")
                }
            }

    val maxLinksNumber : Long by parser
            .storing("-m", "--max-links-number", help = "Max links number") {
                this.toLong()
            }
            .addValidator {
                if (value < 0) {
                    throw RuntimeException("${this.errorName} can't be negative or zero")
                }
            }

}