package com.eug.md

import java.lang.RuntimeException

sealed class PageParserException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)

class NotValidOptionsException(msg: String, cause: Throwable? = null) : PageParserException(msg)