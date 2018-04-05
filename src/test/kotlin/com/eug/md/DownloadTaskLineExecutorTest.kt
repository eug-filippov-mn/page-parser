package com.eug.md

import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import org.jsoup.parser.Tag
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.net.URI

class DownloadTaskLineExecutorTest {


    @Test
    fun `should properly extract download task line from favicon link`() {
        val pageUrl = "http://example.com"
        val faviconName = "favicon.ico"
        val faviconElement = element(
                tag = Tag.valueOf("link"),
                baseUrl = pageUrl,
                attributes = Attributes().apply {
                    put("rel", "icon")
                    put("href", "/$faviconName")
                    put("type", "image/x-icon")
                }
        )

        val result = DownloadTaskLineExtractor.extractFrom(faviconElement)

        Assertions.assertEquals("$pageUrl/$faviconName icons/${extractHost(pageUrl)}_$faviconName", result)
    }

    private fun element(tag: Tag, baseUrl: String, attributes: Attributes) = Element(tag, baseUrl, attributes)

    private fun extractHost(url: String) = URI(url).host
}