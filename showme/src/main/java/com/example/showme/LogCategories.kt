package com.example.showme

enum class LogCategories(val type: Int) {
    ALL(7),
    SUCCESS(6),
    ERROR(5),
    WARNING(4),
    INFO(3),
    EVENT(2),
    DETAIL(1),
    DEBUG(0)
    ;
}

