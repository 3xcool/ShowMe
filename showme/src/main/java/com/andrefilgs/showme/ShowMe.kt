package com.andrefilgs.showme


import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.andrefilgs.fileman.Fileman
import com.andrefilgs.fileman.FilemanDrivers
import com.andrefilgs.fileman.auxiliar.FilemanLogger
import com.andrefilgs.fileman.auxiliar.orDefault
import com.andrefilgs.fileman.workmanager.FilemanWM
import com.andrefilgs.showme.model.LogType
import com.andrefilgs.showme.model.WatcherType
import com.andrefilgs.showme.senders.Sender
import com.andrefilgs.showme.senders.ShowMeHttpSender
import com.andrefilgs.showme.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
 */
class ShowMe(var mShowMeStatus: Boolean = true, var mTAG: String = "ShowMe", private var mLogTypeMode: LogType = LogType.DEBUG, private var mWatcherTypeMode: WatcherType = WatcherType.DEV) {

  //region local variables
  private val MAX_LOG_LENGTH = 4000
  private val MAX_TAG_LENGTH = 23

  var mTAGPrefix = ""
  var mTAGSuffix = ""
  var mShowMeTag = setShowMeTagFinal()

  fun addTagSuffix(suffix: String) {
    this.mTAGSuffix = suffix
    this.mShowMeTag = setShowMeTagFinal()
  }

  fun addTagPrefix(prefix: String) {
    this.mTAGPrefix = prefix
    this.mShowMeTag = setShowMeTagFinal()
  }

  private fun setShowMeTag(tag: String) {
    this.mTAG = tag
    this.mShowMeTag = setShowMeTagFinal()
  }

  private fun setShowMeTagFinal(): String {
    return if ("$mTAGPrefix$mTAG$mTAGSuffix".length > MAX_TAG_LENGTH) "ShowMe" else "$mTAGPrefix$mTAG$mTAGSuffix"
  }

  var mMaxWrapLogSize = 1500

  private var summaryList: MutableList<Pair<LogcatType, String>> = mutableListOf()

  //For Time control
  var mPrecisionAbsoluteTime = 7
  var mPrecisionRelativeTime = 5
  var mPrecisionRelativeByIdTime = 5

  var mCurrentTimeActive: Boolean = false             //this will be useful for storing log in a file or server.
  var mAbsoluteTimeIntervalActive: Boolean = false
  var mRelativeTimeIntervalActive: Boolean = false
  var mRelativeByIdTimeIntervalActive: Boolean = false  //to be backward compatible

  //  var mCurrentTime = getNow()
  var mCurrentTime = getEllapsedRealTime()
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
  var defaultLogType = LogType.VERBOSE
  var defaultWatcherType = WatcherType.PUBLIC
  var defaultWrapMsg = false
  var defaultAddSummary = false
  var defaultLogId = 0
  var defaultGeneralLogCatType = LogcatType.DEBUG
  var defaultTitleLogCatType = LogcatType.VERBOSE
  var defaultSummaryLogCatType = LogcatType.VERBOSE

  //Fileman
  private var mDefaultWriteLog = false
  private var mUseWorkManagerFileman = false  //Set to true if you want to use WorkManager + Coroutine while writing your logs


  //Senders
  var mDefaultSendLog: Boolean? = false
  //endregion


  fun enableShowMe() {
    mShowMeStatus = true
  }

  fun disableShowMe() {
    mShowMeStatus = false
  }

  fun setWatcherLevel(watcherType: WatcherType) {
    this.mWatcherTypeMode = watcherType
  }

  fun setLogTypeLevel(logType: LogType) {
    this.mLogTypeMode = logType
  }


  fun setTimeIntervalStatus(showCurrentTime: Boolean? = false, showAbsolute: Boolean? = false, showRelative: Boolean? = false, showRelativeById: Boolean? = false) {
    showCurrentTime?.let { mCurrentTimeActive = it }
    showAbsolute?.let { mAbsoluteTimeIntervalActive = it }
    showRelative?.let { mRelativeTimeIntervalActive = it }
    showRelativeById?.let { mRelativeByIdTimeIntervalActive = it }
  }

  fun setTimeIntervalPrecision(absolute: Int? = mPrecisionAbsoluteTime, relative: Int? = mPrecisionRelativeTime, relativeById: Int? = mPrecisionRelativeByIdTime) {
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

  //https://sangsoonam.github.io/2017/03/01/do-not-use-curenttimemillis-for-time-interval.html
  private fun getEllapsedRealTime(): Long {
    return SystemClock.elapsedRealtime()
  }

  /**
   * Start log timer for time interval control
   */
  fun startLog() {
    //    val now = getNow()
    val now = getEllapsedRealTime()
    mAbsoluteTimeInterval = now
    mRelativeTimeInterval = now
    mLogsId.clear()
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
  fun dbc(rule: Boolean, msg: String, logType: LogType? = LogType.ERROR, watcherType: WatcherType? = WatcherType.PUBLIC, showMeId: Int? = 0, logcatType: LogcatType? = LogcatType.WARNING, writeLog: Boolean?=mDefaultWriteLog): String {
    if (rule) return ""
    //    skipLine()
    return showMeLog(logcatType, "⛔⛔⛔ Broken Contract: $msg", logType = logType!!, watcherType = watcherType!!, logId = showMeId ?: 0, writeLog = writeLog)
    //    skipLine()
  }


  private fun isLoggable(logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType): Boolean {
    //    if(showMeStatus == null || logType == null || watcherType == null) return false
    if (logType == null || watcherType == null) return false
    //    if (!mShowMeStatus) return false
    if (watcherType.type < mWatcherTypeMode.type) return false
    if (logType.type < mLogTypeMode.type) return false
    return true
  }

  private fun prepareLogMsg(msg: String, logType: LogType? = defaultLogType, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, withTimePrefix: Boolean? = true): String? {

    //    if(!isLoggable(mShowMeStatus, logType, watcherType)) return null

    var outputMsg = if (wrapMsg!!) wrapMessage(msg) else msg

    outputMsg = when (logType) {
      LogType.VERBOSE -> outputMsg
      LogType.SUCCESS -> "$defaultCharSuccess $outputMsg"
      LogType.ERROR -> "$defaultCharError $outputMsg"
      LogType.WARNING -> "$defaultCharWarning $outputMsg"
      LogType.EVENT -> "$defaultCharEvent $outputMsg"
      LogType.INFO -> "$defaultCharInfo $outputMsg"
      LogType.DETAIL -> "$defaultCharDetail $outputMsg"
      LogType.DEBUG -> "$defaultCharDebug $outputMsg"
      else -> outputMsg
    }

    var timePrefix = ""
    val currentTime = getNow() //to get current time
    val ellapsedRealTime = getEllapsedRealTime()  //for time interval

    if (withTimePrefix.orDefault(true)) {
      if (mCurrentTimeActive.orDefault()) timePrefix = "now:${Utils.convertTime(currentTime, Utils.getNowFormat())}"

      if (mAbsoluteTimeIntervalActive.orDefault()) {
        val absoluteTime = ellapsedRealTime - mAbsoluteTimeInterval
        timePrefix = "${addTimeDelimiter(timePrefix)}abs:${Utils.convertToSeconds(absoluteTime, mPrecisionAbsoluteTime)}"
      }

      if (mRelativeTimeIntervalActive.orDefault()) {
        val relativeTime = ellapsedRealTime - mRelativeTimeInterval
        timePrefix = "${addTimeDelimiter(timePrefix)}rel:${Utils.convertToMilliseconds(relativeTime, mPrecisionRelativeTime)}"
      }

      if (mRelativeByIdTimeIntervalActive.orDefault()) {
        val interval = ellapsedRealTime - (mLogsId[logId] ?: ellapsedRealTime)
        mLogsId[logId!!] = ellapsedRealTime  //update last time
        //      timePrefix = "${addTimeDelimiter(timePrefix)}ID:$showMeId-rel:${Utils.convertTime(interval, Utils.getMilliseconds())}"
        timePrefix = "${addTimeDelimiter(timePrefix)}ID:$logId-rel:${Utils.convertToMilliseconds(interval, mPrecisionRelativeByIdTime)}"
      }
    }

    //between timePrefix (if exists) and log message
    if (timePrefix.isNotEmpty()) timePrefix = "$timePrefix ║ "

    outputMsg = "$timePrefix$outputMsg"

    mRelativeTimeInterval = ellapsedRealTime  //always updating last log time for relative time interval

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
  fun showSummary(logType: LogType = defaultLogType, watcherType: WatcherType = defaultWatcherType, logcatType: LogcatType? = defaultSummaryLogCatType, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog) {
    skipLine(1, "─", 100, logcatType = logcatType)
    title("SUMMARY", logType, watcherType, logcatType = logcatType, writeLog = writeLog, sendLog = sendLog)
    summaryList.forEach {
      showMeLog(it.first, it.second, logType = logType, watcherType = watcherType, addSummary = false, withTimePrefix = false, writeLog = writeLog, sendLog = sendLog) //summary logs don't need timePrefix, we are using the respective log time prefix
    }
  }

  fun title(msg: String, logType: LogType = defaultLogType, watcherType: WatcherType = defaultWatcherType, addSummary: Boolean? = false, logId: Int = 0, logcatType: LogcatType? = defaultTitleLogCatType, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    //    var vertical ="║"
    val horiz = "═"
    val topL = "╔"
    val topR = "╗"
    val botL = "╚"
    val botR = "╝"
    //Only for LogCat clean view
    showMeLog(logcatType, "$topL${horiz.repeat(msg.length + 8)}$topR", logType, watcherType, addSummary = false, logId = logId, writeLog = false, sendLog = false)
    showMeLog(logcatType, "╠═══ ${msg.toUpperCase(Locale.ROOT)} ═══╣", logType, watcherType, addSummary = false, logId = logId, writeLog = false, sendLog = false)
    showMeLog(logcatType, "$botL${horiz.repeat(msg.length + 8)}$botR", logType, watcherType, addSummary = false, logId = logId, writeLog = false, sendLog = false)
    return showMeLog(LogcatType.NONE, msg.toUpperCase(Locale.ROOT), logType, watcherType, addSummary, logId = logId, writeLog = writeLog, sendLog = sendLog)   //NONE will not print in Logcat
  }

  fun skipLine(qty: Int = skipLineQty, repeatableChar: String = skipLineRepeatableChar, repeatQty: Int = skipLineRepeatableCharQty, watcherType: WatcherType = defaultWatcherType, logcatType: LogcatType? = defaultGeneralLogCatType) {
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
   * @param writeLog -> Granular control to write or not this specific log.
   * @param sendLog -> Granular control to send or not this specific log, even if this is not Loggable
   * @param withTimePrefix -> if you want to enable/disable timePrefix. Best approach is to use setTimeIntervalStatus()
   */

  fun showMeLog(logcatType: LogcatType? = defaultGeneralLogCatType,
                msg: String,
                logType: LogType? = defaultLogType,
                watcherType: WatcherType? = defaultWatcherType,
                addSummary: Boolean? = defaultAddSummary,
                wrapMsg: Boolean? = defaultWrapMsg,
                logId: Int? = defaultLogId,
                writeLog: Boolean? = mDefaultWriteLog,
                sendLog: Boolean? = mDefaultSendLog,
                withTimePrefix: Boolean? = true): String {
    //ShowMe user may want to send log to a server without showing it at Logcat, so...


    val isLoggable = isLoggable(logType, watcherType)
    val showMeLog = prepareLogMsg(msg, logType, wrapMsg, logId, withTimePrefix = withTimePrefix)
    showMeLog?.let {showMelog ->
      if (isLoggable) {
        if (mShowMeStatus.orDefault()) {
          when (logcatType) {
            LogcatType.VERBOSE -> Log.v(mShowMeTag, showMelog)
            LogcatType.DEBUG -> Log.d(mShowMeTag, showMelog)
            LogcatType.INFO -> Log.i(mShowMeTag, showMelog)
            LogcatType.WARNING -> Log.w(mShowMeTag, showMelog)
            LogcatType.ERROR -> Log.e(mShowMeTag, showMelog)
            LogcatType.NONE -> {
            } //do nothing
          }

          addSummary?.let{
            summaryList.add(summaryList.size, Pair(logcatType ?: defaultSummaryLogCatType, showMelog))
          }
        }

        if (writeLog.orDefault()) writeLogFile(showMelog)  //write log even if it is not loggable

        if (sendLog.orDefault()) sendLog(showMelog)  //send log even if it is not loggable

      }
      return showMelog
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
   * @param writeLog -> Granular control to write or not this specific log, even if this is Loggable
   * @param sendLog -> Granular control to send or not this specific log, even if this is Loggable
   */
  fun d(msg: String, logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    return showMeLog(LogcatType.DEBUG, msg, logType, watcherType, addSummary, wrapMsg, logId, writeLog = writeLog, sendLog = sendLog)
  }


  fun i(msg: String, logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    return showMeLog(LogcatType.INFO, msg, logType, watcherType, addSummary, wrapMsg, logId, writeLog = writeLog, sendLog = sendLog)
  }


  fun w(msg: String, logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    return showMeLog(LogcatType.WARNING, msg, logType, watcherType, addSummary, wrapMsg, logId, writeLog = writeLog, sendLog = sendLog)
  }

  fun e(msg: String, logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    return showMeLog(LogcatType.ERROR, msg, logType, watcherType, addSummary, wrapMsg, logId, writeLog = writeLog, sendLog = sendLog)
  }


  fun v(msg: String, logType: LogType? = defaultLogType, watcherType: WatcherType? = defaultWatcherType, addSummary: Boolean? = defaultAddSummary, wrapMsg: Boolean? = defaultWrapMsg, logId: Int? = defaultLogId, writeLog: Boolean? = mDefaultWriteLog, sendLog: Boolean? = mDefaultSendLog): String {
    return showMeLog(LogcatType.VERBOSE, msg, logType, watcherType, addSummary, wrapMsg, logId, writeLog = writeLog, sendLog = sendLog)
  }


  companion object {
    fun getSpecialChars(): String =
      "☕ ⭐⭕✨❌⚡❎✅ 🎃 ⛔ 🚫 🔍🐞💩🔊💡✋⛔⌚⏰⌛⏳❓❔❗❕ " + "✊✋ ↻ ↺ ⏩⏪⏫⏬ ☣☢☠ ⓘ " + "🔧 💣 🔒 🔓🔔🏁🏆🎯  " + "🚩 🎌 ⛳ " + "💉🔮🎊🎉🎂 💰 💱 💲 💳 💴 💵 💶 💷 💸 " + "🚪 📨 📤 📥 📩 🔥☁ 👀 🌁⛅ 🎦🌋 " + "📌 📍📎 📜 📃 📄 📅 📆 📇🔃  ➿  ☔⚓ " + " ⚽⛄⛅⛎ ⛪⛲⛵⛺⛽ 💀 ☠ 👻 👽 👾 " + "🎅 💃 💁 💬 🍰 💎 💍  🏃   "
  }

  //region ================ Fileman ================

  private lateinit var mContext: Context

  var SHOWME_DRIVE = FilemanDrivers.Internal.type
  var SHOWME_FOLDER = "ShowMe"
  var SHOWME_FILENAME = "ShowMeLogs.json"

  var filemanActive: Boolean? = null
  var filemanWM: FilemanWM? = null
  var FILE_WRITE_APPEND = true


  /**
   * For writing log in file
   *
   * @param showFilemanLog -> If you want to see Fileman logs
   * @param drive -> Use FilemanDrivers.SandBox / FilemanDrivers.Internal / FilemanDrivers.External
   * @param useWorkManager -> Activate this if you want to use WorkManager + Coroutine for writing file
   * @param viewLifecycleOwner -> To get WorkManager liveData observe output
   */
  fun addFileman(filemanActive: Boolean, showFilemanLog: Boolean? = false, context: Context, drive: Int?, folder: String?, filename: String?, append: Boolean?,
                 useWorkManager: Boolean? = false, viewLifecycleOwner: LifecycleOwner? = null, defaultWriteLog:Boolean?=mDefaultWriteLog): Boolean {
    mDefaultWriteLog = defaultWriteLog ?: mDefaultWriteLog
    if(filemanActive) enableFileman() else disableFileman()
    useWorkManager?.let { mUseWorkManagerFileman = it }
    drive?.let { if (it <= FilemanDrivers.values().size) SHOWME_DRIVE = it }
    folder?.let { SHOWME_FOLDER = it }
    filename?.let { SHOWME_FILENAME = it }
    append?.let { FILE_WRITE_APPEND = it }
    this.mContext = context
    if (showFilemanLog.orDefault(false)) FilemanLogger.enableLog() else FilemanLogger.disableLog()

    if (mUseWorkManagerFileman.orDefault(false)) {
      filemanWM = FilemanWM(context, viewLifecycleOwner)
    }
    return true
  }

  fun setDefaultWriteLog(value:Boolean){
    mDefaultWriteLog = value
  }

  fun enableFilemanWorkManager() {
    this.mUseWorkManagerFileman = true
  }

  fun disableFilemanWorkManager() {
    this.mUseWorkManagerFileman = false
  }

  fun enableFileman() {
    this.filemanActive = true
  }

  fun disableFileman() {
    this.filemanActive = false
  }


  private fun isFilemanAvailable(): Boolean {
    return if (!mUseWorkManagerFileman.orDefault(false)) {
      ::mContext.isInitialized && this.filemanActive.orDefault(false)
    } else {
      ::mContext.isInitialized && filemanWM != null && this.filemanActive.orDefault(false)
    }
  }


  fun writeLogFile(fileContent: String) {
    if (!isFilemanAvailable()) {
      dbc(false, "Fileman is not active", writeLog = false)
      return
    }
    if (!mUseWorkManagerFileman.orDefault(false)) {
      Fileman.write("$mShowMeTag: $fileContent\n", mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILENAME, true)
    } else {
      filemanWM?.writeLaunch("$mShowMeTag $fileContent\n", mContext, SHOWME_DRIVE, SHOWME_FOLDER, SHOWME_FILENAME, append = FILE_WRITE_APPEND, withTimeout = false)
    }
  }


  fun deleteLogFile(drive: Int? = SHOWME_DRIVE, folder: String? = SHOWME_FOLDER, filename: String? = SHOWME_FILENAME): Boolean {
    if (drive!! <= FilemanDrivers.values().size) {
      if (::mContext.isInitialized) {
        Fileman.delete(mContext, drive, folder!!, filename!!)
      }
      return false
    }
    return false
  }

  fun readLogFile(drive: Int? = SHOWME_DRIVE, folder: String? = SHOWME_FOLDER, filename: String? = SHOWME_FILENAME): String? {
    if (drive!! <= FilemanDrivers.values().size) {
      if (::mContext.isInitialized) {
        return Fileman.read(mContext, drive, folder ?: SHOWME_FOLDER, filename ?: SHOWME_FILENAME)
      }
      return ""
    }
    return ""
  }

  //endregion


  //region ================ SENDER ================

  private val baseCoroutineScope = CoroutineScope(Dispatchers.Default)

  var mSenders: MutableList<Sender>? = mutableListOf()

  /**
   * @param sender              -> HTTP Sender
   */
  fun addSender(sender: Sender, defaultSendLog:Boolean?) {
    defaultSendLog?.let {  mDefaultSendLog = defaultSendLog }
    when (sender) {
      is ShowMeHttpSender -> {
        mSenders?.add(sender)
      }
      //      else -> d("Sender ${sender.getName} not available ")
    }
  }

  fun getSenderById(id: String): Sender? {
    return mSenders?.firstOrNull { sender -> sender.id == id }
  }

  fun pruneSenderWork(sender: ShowMeHttpSender) {
    sender.workManager.pruneWork()
  }

  fun cancelSenderWork(sender: ShowMeHttpSender) {
    sender.workManager.cancelAllWork()
  }

  fun pruneAllWorks() {
    mSenders?.forEach { sender ->
      if (sender is ShowMeHttpSender) {
        sender.workManager.pruneWork()
      }
    }
  }

  fun cancelAllWorks() {
    mSenders?.forEach { sender ->
      if (sender is ShowMeHttpSender) {
        sender.workManager.cancelAllWork()
      }
    }
  }

  private fun isSenderAvailable(sender: Sender?): Boolean {
    return sender != null
  }

  private fun sendLog(logContent: String) {
    baseCoroutineScope.launch(Dispatchers.Default) {
      mSenders?.forEach { sender ->
        if (isSenderAvailable(sender)) {
          when (sender) {
            is ShowMeHttpSender -> sender.sendLog(logContent)
          }
        }
      }
    }
  }


  /**
   * User can send some content to a server using ShowMeHttpSender
   */
  fun sendContent(content: String) {
    baseCoroutineScope.launch(Dispatchers.Default) {
      mSenders?.forEach { sender ->
        if (sender is ShowMeHttpSender) sender.sendLog(content)
      }
    }
  }


  //endregion


}


enum class LogcatType() {
  VERBOSE,
  DEBUG,
  INFO,
  WARNING,
  ERROR,
  NONE,
}