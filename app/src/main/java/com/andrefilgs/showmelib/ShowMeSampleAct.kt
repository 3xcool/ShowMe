package com.andrefilgs.showmelib

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.andrefilgs.fileman.FilemanDrivers
import com.andrefilgs.showme.LogcatType
import com.andrefilgs.showme.ShowMe
import com.andrefilgs.showme.ShowMeLogger
import com.andrefilgs.showme.ShowMeUncaughtExceptionHandler
import com.andrefilgs.showme.model.LogType
import com.andrefilgs.showme.model.WatcherType
import com.andrefilgs.showme.senders.Sender
import com.andrefilgs.showme.senders.ShowMeHttpSender
import com.andrefilgs.showme.senders.http.converters.GsonBodyConverter
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_show_me_sample.*

//ATTENTION: Put your Logcat at Verbose mode to see all logs
class ShowMeSampleAct : AppCompatActivity(), AdapterView.OnItemSelectedListener {

  private lateinit var mShowMeProduction : ShowMe
  private lateinit var mShowMeDev : ShowMe
  private lateinit var ueh : ShowMeUncaughtExceptionHandler

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_show_me_sample)
    buildShowMe()
    setSpinners()
    setClickListeners()

    //Crash Report for unhandled errors
    val extraInfo = mapOf<String, String>(Pair("some key", "some value"))
    ueh = ShowMeUncaughtExceptionHandler(this, "UEH_test_file", true, extraInfo )  //set addNewline to true for adding "\n" after each line read (default is true)
    Thread.setDefaultUncaughtExceptionHandler(ueh)
  }


  private fun buildShowMe(){
    //If you want to see ShowMe internal logs, by default they are disable
    //    ShowMeLogger.disableLogs()
    ShowMeLogger.enableLogs()

    //Don't use mShowTimeInterval parameter. Use setTimeIntervalStatus()
    mShowMeDev = ShowMe(true, "ShowMe", LogType.DEBUG, WatcherType.DEV)
//    mShowMeDev.setTimeIntervalStatus(false, false, true, true)
//    val ueh = mShowMeDev.createUncaughtExceptionHandler(this, "foo")  //use application context here


    mShowMeProduction = ShowMe(true, "ShowMe", LogType.WARNING, WatcherType.PUBLIC)
    mShowMeProduction.setTimeIntervalStatus(true, true, true, true)


    //use Fileman for writing logs
//    mShowMeDev.addFileman(true,false, this, FilemanDrivers.Internal.type, "Sample Folder", "Log Test", append = true, viewLifecycleOwner = this)
    mShowMeDev.addFileman(true,false, this, FilemanDrivers.Internal.type, "Sample Folder", "Log Test", append = true, viewLifecycleOwner = null)
    mShowMeDev.setDefaultWriteLog(true)

    //If you want to use WorkManager + Coroutine for writing files and observe LiveData response
    //    mShowMeDev.buildFileman(true, this, FilemanDrivers.Internal.type, "Sample Folder", "Log Test", append = true, useWorkManager = true, viewLifecycleOwner = this)
    //    mShowMeDev.filemanWM?.filemanFeedback?.observe(this, Observer { output->
    //      Check FilemanFeedback class
    //      output.message?.let { Log.d("ShowMe", it) }
    //    })


    //You can add Prefix and/or Suffix to ShowMe object TAG. You can add the current classname to your tags for andrefilgs
    mShowMeProduction.addTagSuffix( "-PROD")
    mShowMeDev.addTagSuffix("-DEV")

    buildSender()
  }

  //You can send ShowMe logs to a server using Sender (for now we only have one type -> ShowMeHttpSender)
  //I've no intention to add or replace any powerful REST API library inside ShowMe, so I'm using a simple barebone Http solution (please see ShowMeHttp).
  //You can send a Plain Text or JSON in HTTP Body. By using JSON you can send as ShowMeAPILogModel or any other model that you desire (using UserAPIModel as an andrefilgs).
  //ShowMe is using GSON library for JSON serialization because is the most used one. In the future I will add more options.
  //Just be aware that using your custom object requires Kotlin Reflection (see GsonBodyConverter -> convertToJson)
  private fun buildSender(){

//    val gsonConverter = GsonBodyConverter(Gson(), ShowMeAPILogModel()) //faster solution

    val listOfFieldsValues = mutableMapOf<String,String?>()
    listOfFieldsValues[UserAPIModel::project.name] = "showme"
    val gsonConverter = GsonBodyConverter(Gson(), UserAPIModel::class.java, UserAPIModel::payload.name, UserAPIModel::timestamp.name, listOfFieldsValues) //showMe logs will be add to payload field.

    //CHANGE HERE
    val protocol = "http://"
    val host = "showme.com/"
    val path = "v1/SomeAPI"

    val headers : MutableMap<String, String?> = mutableMapOf<String,String?>()
    headers.put("Content-Type", "application/json")
    headers.put("application", "manager-portal")
    headers.put("application", "web-app-portal")
    headers.put("Cache-Control", "no-cache")
    headers.put("Accept", "application/json")


    //Building first HTTP Sender
    val httpSender1 :Sender? = ShowMeHttpSender.Builder(this)
      .setId("ID-01")  //you don't need to pass this value, default is using UUID to generate random ID
      .active(false)  //todo set this to true in order to send logs to the server
      .addHeaders(headers)
      .buildUrl(protocol, host, path, null)
//      .addHeader("Connection", "keep-alive")
//      .setConverter(PlainTextConverter)
      .setConverter(gsonConverter)
      .setUseCache(false)
      .setReadTimeout(10000)
      .setConnectTimeout(10000)
      .setUseWorkManager(false)
      .showHttpLogs(true)
      .build()

    //You can also build like this
//    val httpSender1 = ShowMeHttpSender(false,null, applicationContext, headers, protocol, host, path)
//    val httpSender1 = ShowMeHttpSender(false,null, applicationContext ,headers, protocol, host, path, null, bodyConverter =  PlainTextConverter)

    //Building second HTTP Sender
      val httpSender2 = ShowMeHttpSender(true,"ID-02", applicationContext,  headers, protocol, host, path, null, gsonConverter,10000,10000,false,
        true, true)

//    val res = httpSender2.sendLogSync("Your message here")  //you can use ShowMeHttpSender

    //Add Senders
    httpSender1?.let { mShowMeDev.addSender(it, true) }
//    mShowMeDev.addSender(httpSender2, defaultSendLog = false) //logs will NOT be sent, we must set to true in each log call that we want to send
    mShowMeDev.addSender(httpSender2, defaultSendLog = true) //all logs will be sent by default

    //some extra fun
//    mShowMeDev.cancelSenderWork(mShowMeDev.getSenderById("ID-02") as ShowMeHttpSender)
//    mShowMeDev.cancelAllWorks()
//    mShowMeDev.pruneAllWorks()
  }


  private fun setSpinners() {
    val adapterLogCategory = ArrayAdapter.createFromResource(this, com.andrefilgs.showme.R.array.spinner_log_category,
      android.R.layout.simple_expandable_list_item_1)
    adapterLogCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner_log_type.adapter = adapterLogCategory
    spinner_log_type.onItemSelectedListener = this

    val adapterWatcherCategory = ArrayAdapter.createFromResource(this, com.andrefilgs.showme.R.array.spinner_watcher_category,
      android.R.layout.simple_expandable_list_item_1)
    adapterWatcherCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner_watcher.adapter = adapterWatcherCategory
    spinner_watcher.onItemSelectedListener = this
  }

  override fun onNothingSelected(p0: AdapterView<*>?) {
    //do nothing
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, p3: Long) {
    //do nothing
  }

  private lateinit var someException:String

  private fun setClickListeners() {
    btn_clear.setOnClickListener {
      tv_log.text = "Log"
      ueh.deleteUEHfile()
    }

    btn_example.setOnClickListener {
      setExampleLog()
    }

    btn_log.setOnClickListener {
      setLogText(et_log_text.text.toString(), spinner_log_type.selectedItemPosition, spinner_watcher.selectedItemPosition)
    }

    btn_read_log.setOnClickListener {
      tv_log.text = mShowMeDev.readLogFile() ?: "File is empty"
    }

    btn_get_ueh_file.setOnClickListener {
      val uehContent = ueh.getUEHcontent()
      tv_log.text = uehContent ?: "File is empty"

      uehContent?.let{
        mShowMeDev.d(uehContent)
//        mShowMeDev.sendContent(it)  //if you just want to send to server
      }
    }

    btn_throw_ueh.setOnClickListener {
      throw Exception("Some test error")
    }

    btn_throw_ueh_crash.setOnClickListener {
      tv_log.text = someException  //this variable is null. Forcing NPE
    }

  }


  private fun setLogText(msg: String?, logTypeSel : Int, watcherSel : Int) {
    val logType = LogType.values()[logTypeSel]
    val watcher = WatcherType.values()[watcherSel]

    val logMsg = ShowMe().d(msg!!, watcherType = watcher, logType = logType)
    mShowMeDev.d(msg,logType, watcher, sendLog = true)  //if you want to test Sender or Fileman
    mShowMeDev.d("$msg-again",logType, watcher, sendLog = true)  //if you want to test Sender or Fileman
    tv_log.append("\n$logMsg")
  }


  private fun setExampleLog() {
    mShowMeDev.startLog()
    mShowMeDev.deleteLogFile()
    mShowMeProduction.startLog()
    mShowMeProduction.deleteLogFile()
    mShowMeProduction.mMaxWrapLogSize = 2000
    val sb = StringBuilder()

    mShowMeDev.d("Send this message to server", sendLog = true)
    mShowMeDev.d("Don't send this message to server", sendLog = false)


    //Development Log samples
    //You can set all your log messages to this Watcher and Log types
    mShowMeDev.defaultWatcherType = WatcherType.PUBLIC
    mShowMeDev.defaultLogType = LogType.VERBOSE

    //And then just use
    mShowMeDev.d("Some default message")

    //Or just use Watcher and Log types to each log for granular control
    sb.append(mShowMeDev.title("All messages Type from Sample-Dev", LogType.VERBOSE, WatcherType.PUBLIC, logId = 0) + "\n")  //by default is VERBOSE
    sb.append(mShowMeDev.d("This is a verbose message", LogType.VERBOSE, WatcherType.PUBLIC, logId = 0)+ "\n")
    sb.append(mShowMeDev.d("This is a success message", LogType.SUCCESS, WatcherType.PUBLIC, logId = 1)+ "\n")
    sb.append(mShowMeDev.d("This is an error message", LogType.ERROR, WatcherType.PUBLIC, logId = 1 )+ "\n")
    sb.append(mShowMeDev.d("This is a warning message", LogType.WARNING, WatcherType.PUBLIC)+ "\n")
    sb.append(mShowMeDev.d("This is an event message", LogType.EVENT, WatcherType.PUBLIC)+ "\n")
    sb.append(mShowMeDev.d("This is an info message", LogType.INFO, WatcherType.PUBLIC)+ "\n")
    sb.append(mShowMeDev.d("This is a detail message", LogType.DETAIL, WatcherType.PUBLIC)+ "\n")
    sb.append(mShowMeDev.d("This is a debug message", LogType.DEBUG, WatcherType.PUBLIC)+ "\n")

    //Using Logcat Types
    mShowMeDev.v("This is a Verbose Logcat", LogType.VERBOSE, WatcherType.PUBLIC, logId = 0, sendLog = false)
    mShowMeDev.d("This is a Debug Logcat", LogType.DEBUG, WatcherType.PUBLIC, logId = 0, sendLog = false)
    mShowMeDev.i("This is an Info Logcat", LogType.INFO, WatcherType.PUBLIC, logId = 0, sendLog = false)
    mShowMeDev.w("This is a Warning Logcat", LogType.WARNING, WatcherType.PUBLIC, logId = 0, sendLog = false)
    mShowMeDev.e("This is an Error Logcat", LogType.ERROR, WatcherType.PUBLIC, logId = 0, sendLog = false)

    //Production Log samples
    mShowMeProduction.title("Only Error or higher messages from Sample-Production", LogType.VERBOSE, WatcherType.PUBLIC, logcatType = LogcatType.INFO)
    mShowMeProduction.d("This is a verbose message", LogType.VERBOSE, WatcherType.PUBLIC, logId = 0)
    mShowMeProduction.d("This is a success message", LogType.SUCCESS, WatcherType.PUBLIC, logId = 1)
    Thread.sleep(600)
    mShowMeProduction.e("This is an error message", LogType.ERROR, WatcherType.PUBLIC, logId = 1, addSummary = true)
    Thread.sleep(600)
    mShowMeProduction.w("This is a warning message", LogType.WARNING, WatcherType.PUBLIC, addSummary = true)

    //Attention: the following logs will not appear in Summary because mShowMeProduction is set toshow only Warning logs and above
    mShowMeProduction.e("This is an event message", LogType.EVENT, WatcherType.PUBLIC, addSummary = true)
    mShowMeProduction.i("This is an info message", LogType.INFO, WatcherType.PUBLIC, addSummary = true)
    mShowMeProduction.d("This is a detail message", LogType.DETAIL, WatcherType.PUBLIC, addSummary = true)
    mShowMeProduction.d("This is a debug message", LogType.DEBUG, WatcherType.PUBLIC, addSummary = true)

    mShowMeProduction.d("This is an error message from Guest Watcher", LogType.ERROR, WatcherType.GUEST, addSummary = true)
    mShowMeProduction.d("This is another error message from Guest Watcher", LogType.ERROR, WatcherType.GUEST, addSummary = true)
    mShowMeProduction.d("This is an error message from Public Watcher", LogType.ERROR, WatcherType.PUBLIC, addSummary = true)
    mShowMeProduction.d("This is another error message from Public Watcher", LogType.ERROR, WatcherType.PUBLIC, addSummary = true)


    //Wrap
    mShowMeProduction.d("Changing wrap size to 5 (don't affect Title Logs)", LogType.VERBOSE, WatcherType.PUBLIC, addSummary = false)
    mShowMeProduction.mMaxWrapLogSize = 5
    mShowMeProduction.title("Wrap examples", LogType.VERBOSE, WatcherType.PUBLIC)
    mShowMeProduction.d("1234567890", wrapMsg = false)
    mShowMeProduction.d("1234567890", wrapMsg = true)
    mShowMeProduction.d("12345678901", wrapMsg = false)
    mShowMeProduction.d("12345678901", wrapMsg = true)
    mShowMeProduction.d("123456789012", wrapMsg = false)
    mShowMeProduction.d("123456789012", wrapMsg = true)

    mShowMeProduction.title("Separators")
    mShowMeProduction.skipLine(1, "▄", 50)
    mShowMeProduction.skipLine(1, "░", 50, logcatType = LogcatType.DEBUG)
    mShowMeProduction.skipLine(1, "█", 50, logcatType = LogcatType.INFO)
    mShowMeProduction.skipLine(1, "─", 50, logcatType = LogcatType.ERROR)
    mShowMeProduction.mMaxWrapLogSize = 100
    mShowMeProduction.d("Changed wrap size to 100", LogType.VERBOSE, WatcherType.PUBLIC, addSummary = false)

    //Design by Contract
    mShowMeProduction.enableShowMe()
    mShowMeProduction.title("Design By Contract Example", LogType.VERBOSE, WatcherType.PUBLIC)
    val a = 1
    var b = 0
    mShowMeProduction.dbc(a > b, "$a is not greater than $b")  //ok, it won't crash the DBC
    b = 3
    sb.append(mShowMeProduction.dbc(a > b, "$a is not greater than $b")+ "\n")  //not ok, it will show DBC message

    //Special chars
    mShowMeProduction.title("Special chars", LogType.VERBOSE, WatcherType.PUBLIC)
    sb.append(mShowMeProduction.d(ShowMe.getSpecialChars(), addSummary = false, wrapMsg = true)+ "\n")

    //Summary
    mShowMeProduction.showSummary()

    mShowMeProduction.clearLog()
    mShowMeDev.clearLog()
    tv_log.append("\n${sb.toString()}")
    tv_log.append("\n☕ See Full Log sample in Logcat")
  }


}
