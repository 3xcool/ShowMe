package com.andrefilgs.showmesample

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

class CoroutineContextProvider {
    val main: CoroutineContext by unsafeLazy { Dispatchers.Main }
    val io: CoroutineContext by unsafeLazy { Dispatchers.IO }
}

fun <T> unsafeLazy(initializer: () -> T): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { initializer() }