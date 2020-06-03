package com.example.showme


import android.content.Context
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.andrefilgs.fileman.Fileman
import com.andrefilgs.fileman.FilemanDrivers
import com.andrefilgs.fileman.auxiliar.FilemanLogger
import com.andrefilgs.fileman.auxiliar.orDefault
import com.andrefilgs.fileman.workmanager.FilemanWM
import com.example.showme.utils.Utils
import java.util.*
import kotlin.math.min


//chars: https://www.compart.com/en/unicode/category/So


/**
 * @author André Filgueiras
 *
 * To improve readability and appearance:
 * Go to Settings -> Editor -> Color Scheme -> Console Fonts -> Line Spacing = 0.9
 *
 * Go to Settings -> Editor -> Color Scheme -> Android Logcat -> Uncheck "Inherit values from" and change the colors as you wish
 * I'm using this Foreground colors provided by this blog post: https://plus.google.com/+Matou%C5%A1Sk%C3%A1la/posts/VJhgiXmTM3f
 * Debug : 6897BB
 * Info : 6A8759
 * Warn : BBB529
 * Error : FF6B68
 * Assert : 9876AA
 *
 * @param mShowMeStatus Turn On/Off ShowMe lib
 * @param mLogTypeMode  For Production = Warning, For Development = Debug
 * @param mWatcherTypeMode  For Production = Public, For Development = Dev
 * @param mShowTimeInterval  Is Deprecated
 * @see  setTimeIntervalStatus() for Time Interval logs
 */
class ShowMe(var mShowMeStatus: Boolean = true, var mTAG: String = "ShowMe", private val mLogTypeMode: Int = LogType.DEBUG.type, private val mWatcherTypeMode: Int = WatcherType.DEV.type,
             @Deprecated("Use setTimeIntervalStatus() to control the 4 types of Time Interval") private val mShowTimeInterval: Boolean = false) {

  //region local variables
  var mTAGPrefix = ""
  var mMaxWrapLogSize = 1500

  private var summaryList: MutableList<Pair<LogcatType,String>> = mutableListOf()

  //For Time control
  var mPrecisionAbsoluteTime = 7
  var mPrecisionRelativeTime = 5
  var mPrecisionRelativeByIdTime = 5

  var mCurrentTimeActive: Boolean = false             //this will be useful for storing log in a file or server.
  var mAbsoluteTimeIntervalActive: Boolean = false
  var mRelativeTimeIntervalActive: Boolean = false
  var mRelativeByIdTimeIntervalActive: Boolean = mShowTimeInterval  //to be backward compatible
  var mCurrentTime = getNow()
  var mAbsoluteTimeInterval: Long = mCurrentTime                 //time difference between now and begin (startLog())
  var mRelativeTimeInterval: Long = mCurrentTime                 //time difference between now and last log
  private var mLogsId: MutableMap<Int, Long> =
    mutableMapOf<Int, Long>() //key is just a number for identification and value is the timestamp. Will be used for time difference between now and last log with the same ID

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
  var defaultLogType = LogType.ALL.type
  var defaultWatcherType = WatcherType.PUBLIC.type
  var defaultWrapMsg = false
  var defaultAddSummary = false
  var defaultGeneralLogCatType = LogcatType.DEBUG
  var defaultTitleLogCatType = LogcatType.VERBOSE
  var defaultSummaryLogCatType = LogcatType.VERBOSE

  //Fileman
  private var mWriteLog = false
  private var mUseWorkManager = false  //Set to true in buildFilemane() if you want to use WorkManager + Coroutine while writing your logs

  //endregion


  fun enableShowMe() {
    mShowMeStatus = true
  }

  fun disableShowMe() {
    mShowMeStatus = false
  }


  fun setTimeIntervalStatus(showCurrentTime: Boolean? = false, showAbsolute: Boolean? = false, showRelative: Boolean? = false, showRelativeById: Boolean? = false) {
    showCurrentTime?.let { mCurrentTimeActive = it }
    showAbsolute?.let { mAbsoluteTimeIntervalActive = it }
    showRelative?.let { mRelativeTimeIntervalActive = it }
    showRelativeById?.let { mRelativeByIdTimeIntervalActive = it }
  }

  fun setTimeIntervalPrecision(absolute:Int?=mPrecisionAbsoluteTime, relative:Int?=mPrecisionRelativeTime, relativeById:Int?=mPrecisionRelativeByIdTime){
    absolute?.let { mPrecisionAbsoluteTime = it }
    relative?.let { mPrecisionRelativeTime = it }
    relativeById?.let { mPrecisionRelativeByIdTime = it }
  }

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

  private fun getNow(): Long {
    return System.currentTimeMillis()
  }

  /**
   * Start log timer for time interval control
   */
  fun startLog() {
    val now = getNow()
    mAbsoluteTimeInterval = now
    mRelativeTimeInterval = now
    mLogsId.clear()
  }


  @Deprecated("Use clearLog() instead.")
  fun finishLog() {
    mLogsId.clear()
    summaryList.clear()
  }

  /**
   * Clear all logs control
   */
  fun clearLog() {
    mLogsId.clear()
    summaryList.clear()
  }

  fun clearAndStartLog() {
    clearLog()
    startLog()
  }


  /**
   * Design By Contract
   */
  fun dbc(rule: Boolean, msg: String, logType: Int? = LogType.ERROR.type, watcherType: Int? = WatcherType.PUBLIC.type, showMeId:Int?=0, logcatType: LogcatType?=LogcatType.WARNING): String {
    if (rule) return ""
    //    skipLine()
    return showMeLog(logcatType,"⛔⛔⛔ Broken Contract: $msg", logType = logType!!, watcherType = watcherType!!, logId = showMeId?:0)
    //    skipLine()
  }


  private fun prepareLogMsg(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg,
                            showMeId: Int = 0, logcatType: LogcatType?=defaultSummaryLogCatType, withTimePrefix:Boolean?=true): String? {
    if (!mShowMeStatus) return null //don't show
    if (watcherType < mWatcherTypeMode) return null //don't show
    if (logType < mLogTypeMode) return null //don't show

    var outputMsg = if (wrapMsg!!) wrapMessage(msg) else msg

    outputMsg = when (logType) {
      LogType.ALL.type -> outputMsg
      LogType.SUCCESS.type -> "$defaultCharSuccess $outputMsg"
      LogType.ERROR.type -> "$defaultCharError $outputMsg"
      LogType.WARNING.type -> "$defaultCharWarning $outputMsg"
      LogType.EVENT.type -> "$defaultCharEvent $outputMsg"
      LogType.INFO.type -> "$defaultCharInfo $outputMsg"
      LogType.DETAIL.type -> "$defaultCharDetail $outputMsg"
      LogType.DEBUG.type -> "$defaultCharDebug $outputMsg"
      else -> outputMsg
    }

    var timePrefix = ""
    val currentTime = getNow()

    if(withTimePrefix.orDefault(true)){
      if (mCurrentTimeActive.orDefault()) timePrefix = "now:${Utils.convertTime(currentTime, Utils.getNowFormat())}"

      if (mAbsoluteTimeIntervalActive.orDefault()) {
        val absoluteTime = currentTime - mAbsoluteTimeInterval
        timePrefix = "${addTimeDelimiter(timePrefix)}abs:${Utils.convertToSeconds(absoluteTime, mPrecisionAbsoluteTime)}"
      }

      if (mRelativeTimeIntervalActive.orDefault()) {
        val relativeTime = currentTime - mRelativeTimeInterval
        timePrefix = "${addTimeDelimiter(timePrefix)}rel:${Utils.convertToMilliseconds(relativeTime, mPrecisionRelativeTime)}"
      }

      if (mRelativeByIdTimeIntervalActive.orDefault()) {
        val interval = currentTime - (mLogsId[showMeId] ?: currentTime)
        mLogsId[showMeId] = currentTime  //update last time
        //      timePrefix = "${addTimeDelimiter(timePrefix)}ID:$showMeId-rel:${Utils.convertTime(interval, Utils.getMilliseconds())}"
        timePrefix = "${addTimeDelimiter(timePrefix)}ID:$showMeId-rel:${Utils.convertToMilliseconds(interval, mPrecisionRelativeByIdTime)}"
      }
    }

    //between timePrefix (if exists) and log message
    if (timePrefix.isNotEmpty()) timePrefix = "$timePrefix ║ "

    outputMsg = "$timePrefix$outputMsg"

    if (addSummary!!){
      summaryList.add(summaryList.size, Pair(logcatType ?:defaultSummaryLogCatType, outputMsg))
    }

    mRelativeTimeInterval = currentTime  //always updating last log time for relative time interval

    return outputMsg
  }

  private fun addTimeDelimiter(timePrefix: String): String {
    return if (timePrefix.isEmpty()) timePrefix else "$timePrefix │"
  }


  /**
   * Show only important Logs
   *
   * Each log stored in summary is a Log snapshot with respective timePrefix and logcatType
   */
  fun showSummary(logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, logcatType: LogcatType?=defaultSummaryLogCatType) {
    skipLine(1, "─", 100, logcatType =  logcatType)
    title("SUMMARY", logType, watcherType, logcatType = logcatType)
    summaryList.forEach {
      showMeLog(it.first, it.second, logType, watcherType, addSummary = false, withTimePrefix = false) //summary logs don't need timePrefix, we are using the respective log time prefix
    }
  }

  fun title(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = false, logId: Int = 0, logcatType: LogcatType?=defaultTitleLogCatType): String {
//    var vertical ="║"
    val horiz = "═"
    val topL = "╔"
    val topR = "╗"
    val botL = "╚"
    val botR = "╝"
    //Only for LogCat clean view
    showMeLog(logcatType,"$topL${horiz.repeat(msg.length + 8 )}$topR", addSummary = false, writeLog = false)
    showMeLog(logcatType, "╠═══ ${msg.toUpperCase(Locale.ROOT)} ═══╣", logType, watcherType, addSummary = false, logId = logId, writeLog = false)
    showMeLog(logcatType,"$botL${horiz.repeat(msg.length + 8 )}$botR", addSummary = false, writeLog = false)
    return prepareLogMsg(msg, logType, watcherType, addSummary, showMeId= logId) ?: ""  //main title ShowMe log
  }

  fun skipLine(qty: Int = skipLineQty, repeatableChar: String = skipLineRepeatableChar, repeatQty: Int = skipLineRepeatableCharQty, watcherType: Int = defaultWatcherType, logcatType: LogcatType?=defaultGeneralLogCatType) {
    for (i in 1..qty) {
      showMeLog(logcatType, msg = repeatableChar.repeat(repeatQty), watcherType = watcherType)
    }
  }


  private fun wrapMessage(veryLongString: String): String {
    val sb = StringBuilder()
    for (i in 0..veryLongString.length / mMaxWrapLogSize) {
      val start = i * mMaxWrapLogSize
      val end = min((i + 1) * mMaxWrapLogSize, veryLongString.length)
      sb.append(veryLongString.substring(start, end))
      sb.append("\n")
    }
    return sb.toString()
  }


  /**
   * MAIN Log Debug
   * You can use v(), d(), i(), w(), e() methods or use this method with LogCatType as parameter
   * @param msg -> Log Message
   * @param logType -> See LogType class (e.g. Success, Error, Warning...)
   * @param watcherType -> See WatcherType class (Public, Guest, Dev)
   * @param addSummary -> Call showSummary() to see all logs stored as important
   * @param wrapMsg -> Wrap or not the Log Message
   * @param logId -> For Time Interval calculation by ID
   * @param withTimePrefix -> if you want to enable/disable timePrefix. Best approach is to use setTimeIntervalStatus()
   */
  fun showMeLog(logcatType: LogcatType?=LogcatType.VERBOSE, msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary,
                wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0, writeLog:Boolean?=mWriteLog, withTimePrefix: Boolean?=true): String {
    prepareLogMsg(msg, logType, watcherType, addSummary, wrapMsg, logId,logcatType= logcatType, withTimePrefix = withTimePrefix)?.let {
      when (logcatType) {
        LogcatType.VERBOSE -> Log.v(mTAGPrefix + mTAG, it)
        LogcatType.DEBUG -> Log.d(mTAGPrefix + mTAG, it)
        LogcatType.INFO -> Log.i(mTAGPrefix + mTAG, it)
        LogcatType.WARNING -> Log.w(mTAGPrefix + mTAG, it)
        LogcatType.ERROR -> Log.e(mTAGPrefix + mTAG, it)
      }
      if (writeLog.orDefault()) writeLog(it)
      return it
    }
    return ""
  }


  /**
   * Log Debug
   * @param msg -> Log Message
   * @param logType -> See LogType class (e.g. Success, Error, Warning...)
   * @param watcherType -> See WatcherType class (Public, Guest, Dev)
   * @param addSummary -> Call showSummary() to see all logs stored as important
   * @param wrapMsg -> Wrap or not the Log Message
   * @param logId -> For Time Interval calculation by ID
   */
  fun d(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    return showMeLog(LogcatType.DEBUG, msg, logType, watcherType, addSummary, wrapMsg, logId)
  }


  fun i(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    return showMeLog(LogcatType.INFO, msg, logType, watcherType, addSummary, wrapMsg, logId)
  }


  fun w(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    return showMeLog(LogcatType.WARNING, msg, logType, watcherType, addSummary, wrapMsg, logId)
  }

  fun e(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    return showMeLog(LogcatType.ERROR, msg, logType, watcherType, addSummary, wrapMsg, logId)
  }


  fun v(msg: String, logType: Int = defaultLogType, watcherType: Int = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int = 0): String {
    return showMeLog(LogcatType.VERBOSE, msg, logType, watcherType, addSummary, wrapMsg, logId)
  }


  companion object {
    fun getSpecialChars(): String =
      "☕ ⭐⭕✨❌⚡❎✅ 🎃 ⛔ 🚫 🔍🐞💩🔊💡✋⛔⌚⏰⌛⏳❓❔❗❕ " + "✊✋ ↻ ↺ ⏩⏪⏫⏬ ☣☢☠ ⓘ " + "🔧 💣 🔒 🔓🔔🏁🏆🎯  " +
          "🚩 🎌 ⛳ " + "💉🔮🎊🎉🎂 💰 💱 💲 💳 💴 💵 💶 💷 💸 " + "🚪 📨 📤 📥 📩 🔥☁ 👀 🌁⛅ 🎦🌋 " + "📌 📍📎 📜 📃 📄 📅 📆 📇🔃  ➿  ☔⚓ " + " ⚽⛄⛅⛎ ⛪⛲⛵⛺⛽ 💀 ☠ 👻 👽 👾 " +
          "🎅 💃 💁 💬 🍰 💎 💍  🏃   "
  }

  //region ================ Fileman ================

  private lateinit var mContext: Context

  var SHOWME_DRIVE = FilemanDrivers.Internal.type
  var SHOWME_FOLDER = "ShowMe"
  var SHOWME_FILENAME = "ShowMeLogs.json"

  var filemanWM: FilemanWM? = null
  var FILE_WRITE_APPEND = true


  @Deprecated("Not in use anymore, use buildFileman() instead")
  fun injectContext(context: Context) {
    this.mContext = context
  }

  /**
   * For writing log in file
   *
   * @param showFilemanLog -> If you want to see Fileman logs
   * @param drive -> Use FilemanDrivers.SandBox / FilemanDrivers.Internal / FilemanDrivers.External
   * @param useWorkManager -> Use this if you want to use WorkManager + Coroutine for writing file (in most cases you won't need this)
   * @param viewLifecycleOwner -> To get WorkManager liveData observe output
   */
  fun buildFileman(showFilemanLog: Boolean? = mShowMeStatus, context: Context, drive: Int?, folder: String?, filename: String?, append: Boolean?, useWorkManager: Boolean? = false, viewLifecycleOwner: LifecycleOwner? = null): Boolean {
    mWriteLog = true
    useWorkManager?.let { mUseWorkManager = it }
    drive?.let { if (it <= FilemanDrivers.values().size) SHOWME_DRIVE = it }
    folder?.let { SHOWME_FOLDER = it }
    filename?.let { SHOWME_FILENAME = it }
    append?.let { FILE_WRITE_APPEND = it }
    this.mContext = context
    if (showFilemanLog.orDefault(mShowMeStatus)) FilemanLogger.enableLog() else FilemanLogger.disableLog()

    if (mUseWorkManager.orDefault(false)) {
      return if (viewLifecycleOwner != null) {
        filemanWM = FilemanWM(context, viewLifecycleOwner)
        true
      } else {
        false
      }
    }
    return true
  }

  private fun isFilemanAvailable(): Boolean {
    return if (!mUseWorkManager.orDefault(false)) {
      ::mContext.isInitialized
    } else {
      ::mContext.isInitialized && filemanWM != null
    }
  }


  fun writeLog(fileContent: String) {
    if (!isFilemanAvailable()) {
      dbc(false, "Fileman has bot been initialized properly")
      return
    }
    if (!mUseWorkManager.orDefault(false)) {
      Fileman.write("$mTAGPrefix $mTAG $fileContent\n", mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILENAME, true)
    } else {
      filemanWM?.writeLaunch("$mTAGPrefix $mTAG $fileContent\n", mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILENAME, append = FILE_WRITE_APPEND, withTimeout = false)
    }
  }


  fun deleteLog(drive:Int?=SHOWME_DRIVE, folder: String?=SHOWME_FOLDER, filename: String?=SHOWME_FILENAME): Boolean {
    if(drive!! <= FilemanDrivers.values().size){
      if (::mContext.isInitialized) {
        Fileman.delete(mContext, drive, folder!!, filename!!)
      }
      return false
    }
    return false
  }

  fun readLog(drive:Int?=SHOWME_DRIVE, folder: String?=SHOWME_FOLDER, filename: String?=SHOWME_FILENAME): String? {
    if(drive!! <= FilemanDrivers.values().size){
      if (::mContext.isInitialized) {
        Fileman.read(mContext, drive, folder?:SHOWME_FOLDER, filename?:SHOWME_FILENAME)
      }
      return ""
    }
    return ""
  }

  //endregion

}


enum class LogcatType() {
  VERBOSE,
  DEBUG,
  INFO,
  WARNING,
  ERROR,
}