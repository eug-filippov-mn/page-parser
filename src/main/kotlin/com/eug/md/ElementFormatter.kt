package com.eug.md

import org.jsoup.nodes.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URISyntaxException

object ElementFormatter {
    private val log: Logger = LoggerFactory.getLogger(ElementFormatter::class.java)

    private enum class ResourceType(
            private val directoryName: String,
            private val urlAttribute: String) {

        OTHER("other", "abs:href"),

        SCRIPT("scripts", "abs:src"),

        IMG("imgs", "abs:src") {
            override fun resolveFileNameFromUrl(url: String): String? {
                val fileNameWithExtension = super.resolveFileNameFromUrl(url)
                return when(fileNameWithExtension?.substringAfterLast(".")) {
                    "jpg" -> "jpg/$fileNameWithExtension"
                    "png" -> "png/$fileNameWithExtension"
                    else -> fileNameWithExtension
                }
            }
        },

        ICON("icons", "abs:href"),

        CSS("css", "abs:href"),

        RSS("rss", "abs:href") {
            override fun resolveFileNameFromUrl(url: String): String? {
                return try {
                    URI(url).host + ".xml"
                } catch (e: URISyntaxException) {
                    log.warn("Unable to get host for rss link {}", url)
                    null
                }
            }
        };

        open protected fun resolveFileNameFromUrl(url: String): String? {
            val fileName = url.substringAfterLast("/").substringBeforeLast("?")
            if (fileName.isEmpty()) {
                return null
            }
            return fileName
        }

        fun format(element: Element): String {
            val url = element.attr(urlAttribute)
            val fileName = resolveFileNameFromUrl(url)
            return "$url $directoryName/$fileName"
        }

        companion object {

            fun of(element: Element): ResourceType? {
                val tagName = element.tagName()
                return when(tagName) {
                    "link" -> {
                        val linkType = element.attr("type")
                        when(linkType) {
                            "application/rss+xml" -> RSS
                            "image/x-icon" -> ICON
                            "text/css" -> CSS
                            else -> OTHER
                        }
                    }
                    "img" -> {
                        IMG
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

    fun formatResourceLinkOf(element: Element): String? {
        log.debug("Formatting element {}...", element)
        return ResourceType.of(element)?.format(element)
    }
}