package com.example.showme

enum class LogType(val type: Int) {
    @Deprecated("use Verbose instead", replaceWith = ReplaceWith("VERBOSE"))
    ALL(7),
    VERBOSE(7),
    SUCCESS(6),
    ERROR(5),
    WARNING(4),
    INFO(3),
    EVENT(2),
    DETAIL(1),
    DEBUG(0)
}

