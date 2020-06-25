package com.andrefilgs.showme

import android.util.Log
import com.andrefilgs.fileman.auxiliar.orDefault

class ShowMeLogger {


  companion object{
    private val TAG = "ShowMe-Logger"
    private var showLogs:Boolean = false

    fun enableLogs(){showLogs = true}
    fun disableLogs(){showLogs = false}

    fun log(logContent:String, logcatType: LogcatType?=LogcatType.VERBOSE, tag:String? ){
      val mTag = tag ?: TAG
      if(showLogs.orDefault()){
        when(logcatType){
          LogcatType.VERBOSE -> Log.v(mTag, logContent)
          LogcatType.DEBUG -> Log.d(mTag, logContent)
          LogcatType.INFO -> Log.i(mTag, logContent)
          LogcatType.WARNING -> Log.w(mTag, logContent)
          LogcatType.ERROR -> Log.e(mTag, logContent)
          else -> Log.v(mTag, logContent)
        }
      }
    }

  }

}