package com.eug.md

import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException

object DownloadTaskLineExtractor {
    private val log: Logger = LoggerFactory.getLogger(DownloadTaskLineExtractor::class.java)

    private enum class TypedExtractor(
            private val directoryName: String,
            private val urlAttribute: String) {

        OTHER("other", "abs:href"),

        SCRIPT("scripts", "abs:src"),

        IMG("imgs", "abs:src") {
            override fun resolveFileName(resourceUrl: String): String? {
                val fileNameWithExtension = super.resolveFileName(resourceUrl)

                return when(fileNameWithExtension?.substringAfterLast(".")) {
                    "jpg" -> "jpg/$fileNameWithExtension"
                    "png" -> "png/$fileNameWithExtension"
                    else -> fileNameWithExtension
                }
            }
        },

        ICON("icons", "abs:href") {
            override fun resolveFileName(resourceUrl: String): String? {
                return try {
                    URI(resourceUrl).host + "_" + super.resolveFileName(resourceUrl)
                } catch (e: URISyntaxException) {
                    log.warn("Unable to get host for favicon link {}", resourceUrl)
                    null
                }
            }
        },

        CSS("css", "abs:href"),

        RSS("rss", "abs:href") {
            override fun resolveFileName(resourceUrl: String): String? {
                return try {
                    URI(resourceUrl).host + ".xml"
                } catch (e: URISyntaxException) {
                    log.warn("Unable to get host for rss link {}", resourceUrl)
                    null
                }
            }
        };

        protected open fun resolveFileName(resourceUrl: String): String? {
            val fileName = resourceUrl.substringAfterLast("/").substringBeforeLast("?")

            if (fileName.isEmpty()) {
                return null
            }
            return fileName
        }

        fun extract(element: Element): String {
            val resourceUrl = element.attr(urlAttribute)
            val fileName = resolveFileName(resourceUrl)
            if (fileName == null) {
                log.warn("Filename is null for element {}", element)
            }
            return "$resourceUrl $directoryName/$fileName"
        }

        companion object {

            fun forElement(element: Element): TypedExtractor? {
                val tagName = element.tagName()
                return when(tagName) {
                    "link" -> {
                        val linkRel = element.attr("rel")
                        if (linkRel == "alternate" || linkRel == "canonical") {
                            return null
                        }

                        val linkType = element.attr("type")
                        when(linkType) {
                            "application/rss+xml" -> RSS
                            "image/x-icon" -> ICON
                            "text/css" -> CSS
                            "" -> {
                                log.warn("Empty link type {} for element {}", linkType, element)
                                null
                            }
                            else -> OTHER
                        }
                    }
                    "img" -> {
                        val imgSrc = element.attr("src")
                        return if (imgSrc.isBlank()) {
                            null
                        } else {
                            IMG
                        }
                    }
                    "script" -> SCRIPT
                    else -> {
                        log.warn("Unsupported tag name %s", tagName)
                        null
                    }
                }
            }
        }
    }

    fun extractFrom(element: Element): String? {
        log.debug("Extracting download task line from {}...", element)
        return TypedExtractor.forElement(element)?.extract(element)
    }
}