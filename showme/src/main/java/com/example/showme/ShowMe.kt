package com.example.showme



import android.content.Context
import android.util.Log
import android.util.Log.d
import com.example.fileman.Fileman
import java.io.File
import java.lang.StringBuilder
import kotlin.math.min


//chars: https://www.compart.com/en/unicode/category/So

//todo write Log File

//todo Production FLAGS
//mShowMeStatus:        Turn On/Off ShowMe lib
//mLogCategoryMode:     For Production = Warning, For Development = Debug
//mWatcherCategoryMode: For Production = Public, For Development = Dev
//mShowMeId:            If you want to show TimeInterval, set a ShowMeId to calculate the same thread Interval
class ShowMe(var mShowMeStatus: Boolean = true, var mTAG: String = "ShowMe", private val mLogCategoryMode: Int = LogType.DEBUG.type,
  private val mWatcherCategoryMode: Int = WatcherType.DEV.type, private val mShowTimeInterval: Boolean = false, var mWriteLog: Boolean = false) {

  private lateinit var mContext : Context

  var SHOWME_DRIVE = Fileman.DRIVE_SDI
  var SHOWME_FOLDER = "ShowMe"
  var SHOWME_FILE  = "ShowMeLogs.json"

  /**
   * For writing log in file (mWriteLog = true)
   */
  fun injectContext(context: Context){
    this.mContext = context
  }

  fun deleteLog(){
    if(::mContext.isInitialized) Fileman.delete(mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILE)
  }

  fun readLog(): String?{
    return if(::mContext.isInitialized) Fileman.read(mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILE) else null
  }

  fun enableShowMe() {
    mShowMeStatus = true
  }

  fun disableShowMe() {
    mShowMeStatus = false
  }



  companion object {

    fun getSpecialChars(): String {
      return "☕ ⭐⭕✨❌⚡❎✅ 🎃 ⛔ 🚫 🔍🐞💩🔊💡 ✋⛔  ⌚⏰⌛⏳❓❔❗❕ " + "✊✋ ↻ ↺ ⏩⏪⏫⏬ ☣☢☠ ⓘ " + "🔧 💣 🔒 🔓🔔🏁🏆🎯  🚩 🎌 ⛳ " + "💉🔮🎊🎉🎂 💰 💱 💲 💳 💴 💵 💶 💷 💸 " + "🚪 📨 📤 📥 📩 🔥☁ 👀 🌁⛅ 🎦🌋 " + "📌 📍📎 📜 📃 📄 📅 📆 📇🔃  ➿  ☔⚓ " + " ⚽⛄⛅⛎ ⛪⛲⛵⛺⛽ 💀 ☠ 👻 👽 👾 " + "🎅 💃 💁 💬 🍰 💎 💍  🏃   "
    }

  }

  var mTAGPrefix = ""
  var mMaxLogSize = 150



  private var summaryList: MutableList<String> = mutableListOf()
  private var mLogsId : MutableMap<Int, Long> = mutableMapOf<Int, Long>() // HashMap<Int, Long>()

  //Default Values
  private var skipLineQty = 1
  private var skipLineRepeatableChar = "="
  private var skipLineRepeatableCharQty = 100

  //Default chars
  var defaultCharSuccess = "☕☕☕"
  var defaultCharError = "❌❌❌"
  var defaultCharWarning = "🎃"
  var defaultCharEvent = "⚡"
  var defaultCharInfo = "💬"
  var defaultCharDetail = "👀"
  var defaultCharDebug = "🐞"

  //Default Log and Watcher type
  var defaultLogCategory = LogType.ALL.type
  var defaultWatcherCategory = WatcherType.PUBLIC.type
  var defaultWrapMsg = false
  var defaultAddSummary = false


  /**
   * Change log default chars
   */
  fun setDefaultCharsValue(success: String?, error: String?, warning: String?, event: String?, info: String?, detail: String?, debug: String?) {
    success.let { defaultCharSuccess = it!! }
    error.let { defaultCharError = it!! }
    warning.let { defaultCharWarning = it!! }
    event.let { defaultCharEvent = it!! }
    info.let { defaultCharInfo = it!! }
    detail.let { defaultCharDetail = it!! }
    debug.let { defaultCharDebug = it!! }
  }

  /**
   * Set skip line default values
   */
  fun setSkipLineDefaultValues(qty: Int = skipLineQty, repeatableChar: String = skipLineRepeatableChar, repeatQty: Int = skipLineRepeatableCharQty) {
    skipLineQty = qty
    skipLineRepeatableChar = repeatableChar
    skipLineRepeatableCharQty = repeatQty
  }

  /**
   * Start log timer for time interval control
   */
  fun startLog() {
    mLogsId.clear()
  }

  /**
   * Clear all logs control
   */
  fun finishLog(){
    mLogsId.clear()
    summaryList.clear()
  }


  /**
   * Design By Contract
   */
  fun dbc(rule: Boolean, msg: String): String {
    if (rule) return ""
    //    skipLine()
    return d("⛔⛔⛔ Broken Contract: $msg", watcherCategory = WatcherType.PUBLIC.type)
    //    skipLine()
  }


  /**
   * Log funs
   */
  private fun prepareLogMsg(msg: String, logCategory: Int = defaultLogCategory, watcherCategory: Int = defaultWatcherCategory, addSummary: Boolean? = defaultAddSummary,
    wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String? {
    if (!mShowMeStatus) return null //don't show
    if (watcherCategory < mWatcherCategoryMode) return null //don't show
    if (logCategory < mLogCategoryMode) return null //don't show

    var outputMsg = if (wrapMsg!!) wrapMessage(msg) else msg

    outputMsg = when (logCategory) {
      LogType.ALL.type -> outputMsg
      LogType.SUCCESS.type -> "$defaultCharSuccess $outputMsg"
      LogType.ERROR.type -> "${defaultCharError} $outputMsg"
      LogType.WARNING.type -> "$defaultCharWarning $outputMsg"
      LogType.EVENT.type -> "$defaultCharEvent $outputMsg"
      LogType.INFO.type -> "$defaultCharInfo $outputMsg"
      LogType.DETAIL.type -> "$defaultCharDetail $outputMsg"
      LogType.DEBUG.type -> "$defaultCharDebug $outputMsg"
      else -> outputMsg
    }

    if (addSummary!!) summaryList.add(summaryList.size, outputMsg)

    if (mShowTimeInterval) {
      val currentTime = System.currentTimeMillis()
      val interval = currentTime - (mLogsId[logId] ?: currentTime)
      mLogsId[logId] = currentTime  //update last time
      outputMsg = "ID: $logId - Time $interval: $outputMsg"
    }
    return outputMsg
  }

  /**
   * Debug
   */
  fun d(msg: String, logCategory: Int = defaultLogCategory, watcherCategory: Int = defaultWatcherCategory, addSummary: Boolean? = defaultAddSummary,
    wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    prepareLogMsg(msg, logCategory, watcherCategory, addSummary, wrapMsg, logId)?.let {
      Log.d(mTAGPrefix + mTAG, it)
      if(mWriteLog && ::mContext.isInitialized) {
        Fileman.write("$mTAGPrefix $mTAG $it\n", mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILE, true, mShowMeStatus)
      }
      return it
    }
    return ""
  }


  /**
   * Show only important Logs
   */
  fun showSummary(logCategory: Int = defaultLogCategory, watcherCategory: Int = defaultWatcherCategory): String {
    title("SUMMARY", logCategory, watcherCategory)
    summaryList.forEach {
      return d(it, logCategory, watcherCategory)
    }
    return ""
  }

  fun title(msg: String, logCategory: Int = defaultLogCategory, watcherCategory: Int = defaultWatcherCategory, addSummary: Boolean? = false, logId: Int = 0): String {
    skipLine(1, "=")
    val outputMsg = d(msg.toUpperCase(), logCategory, watcherCategory, addSummary = addSummary, logId = logId)
    skipLine(1, "=")
    return outputMsg
  }

  fun skipLine(qty: Int = skipLineQty, repeatableChar: String = skipLineRepeatableChar, repeatQty: Int = skipLineRepeatableCharQty) {
    for (i in 1..qty) {
      d(mTAGPrefix + mTAG, "\n${repeatableChar.repeat(repeatQty)}")
    }
  }


  private fun wrapMessage(veryLongString: String): String {
    val sb = StringBuilder()
    for (i in 0..veryLongString.length / mMaxLogSize) {
      val start = i * mMaxLogSize
      val end = min((i + 1) * mMaxLogSize, veryLongString.length)
      sb.append(veryLongString.substring(start, end))
      sb.append("\n")
    }
    return sb.toString()
  }

}