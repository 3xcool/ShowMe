package com.example.showme.senders.api

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.andrefilgs.fileman.auxiliar.orDefault
import com.example.showme.ShowMeConstants
import kotlinx.coroutines.coroutineScope

internal class HttpWorker(appContext: Context, workerParams: WorkerParameters)
  : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result = coroutineScope{
    try {
      val url = inputData.getString(ShowMeConstants.KEY_HTTP_URL)
      val method = inputData.getString(ShowMeConstants.KEY_HTTP_METHOD)
      val body = inputData.getString(ShowMeConstants.KEY_HTTP_BODY)
      val readTimeout = inputData.getInt(ShowMeConstants.KEY_HTTP_TIMEOUT, ShowMeHttp.TIMEOUT)
      val connectTimeout = inputData.getInt(ShowMeConstants.KEY_HTTP_CONNECT_TIMEOUT, ShowMeHttp.CONNECT_TIMEOUT)
      val useCache = inputData.getBoolean(ShowMeConstants.KEY_HTTP_USE_CACHE, ShowMeHttp.USE_CACHE)
      val showHttpLogs = inputData.getBoolean(ShowMeConstants.KEY_HTTP_SHOW_HTTP_LOGS, ShowMeHttp.showLogs)
      val httpRequest = ShowMeHttp.makeRequestAsync( url, method, body,  ShowMeConstants.headers, readTimeout, connectTimeout, useCache, showHttpLogs)

      val res = httpRequest.await()
      if(res?.success.orDefault(false)){
        return@coroutineScope Result.success()
      }else{
        return@coroutineScope Result.retry()
      }
    }catch (e: Exception){
      return@coroutineScope Result.retry()
    }
  }

}