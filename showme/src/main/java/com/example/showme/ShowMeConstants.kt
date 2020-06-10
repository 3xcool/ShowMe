package com.example.showme

internal object ShowMeConstants {


  //WorkManager
  internal val WORKER_TAG_HTTP = "WORKER_TAG_HTTP"
  internal val KEY_HTTP_TAG = "KEY_HTTP_TAG"
  internal val KEY_HTTP_URL = "KEY_HTTP_URL"
  internal val KEY_HTTP_METHOD = "KEY_HTTP_METHOD"
  internal val KEY_HTTP_HEADERS = "KEY_HTTP_HEADERS"
  internal val KEY_HTTP_BODY = "KEY_HTTP_BODY"
  internal val KEY_HTTP_TIMEOUT = "KEY_HTTP_TIMEOUT"
  internal val KEY_HTTP_CONNECT_TIMEOUT = "KEY_HTTP_CONNECT_TIMEOUT"
  internal val KEY_HTTP_USE_CACHE = "KEY_HTTP_USE_CACHE"
  internal val KEY_HTTP_SHOW_HTTP_LOGS = "KEY_HTTP_SHOW_HTTP_LOGS"

  var headers :MutableMap<String, String?>? = mutableMapOf()

}