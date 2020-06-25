package com.andrefilgs.showme.model



enum class WatcherType(val type: Int) {
    PUBLIC(2),       //anyone can see
    GUEST(1),       //only developer and guest can see
    DEV(0),         //only developer can see
}

