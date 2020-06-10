package com.example.showme.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.andrefilgs.fileman.FilemanDrivers
import com.example.showme.LogType
import com.example.showme.LogcatType
import com.example.showme.ShowMe
import com.example.showme.WatcherType
import com.example.showme.senders.Sender
import com.example.showme.senders.ShowMeHttpSender
import com.example.showme.senders.api.converters.GsonBodyConverter
import com.example.showme.senders.api.model.UserAPIModel
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_show_me_sample.*


class ShowMeSampleAct : AppCompatActivity(), AdapterView.OnItemSelectedListener {

  private lateinit var mShowMeProduction : ShowMe
  private lateinit var mShowMeDev : ShowMe

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(com.example.showme.R.layout.activity_show_me_sample)
    buildShowMe()
    setSpinners()
    setClickListeners()
  }


  private fun buildShowMe(){
    //Don't use mShowTimeInterval parameter. Use setTimeIntervalStatus()
    mShowMeDev = ShowMe(true, "ShowMe", LogType.DEBUG, WatcherType.DEV)
//    mShowMeDev.setTimeIntervalStatus(false, false, true, true)

    mShowMeProduction = ShowMe(true, "ShowMe", LogType.WARNING, WatcherType.PUBLIC, mShowTimeInterval = false)
    mShowMeProduction.setTimeIntervalStatus(true, true, true, true)


    //use Fileman for writing logs
    mShowMeDev.initFileman(false, this, FilemanDrivers.Internal.type, "Sample Folder", "Log Test", append = true)

    //If you want to use WorkManager + Coroutine for writing files and observe LiveData response
    //    mShowMeDev.buildFileman(true, this, FilemanDrivers.Internal.type, "Sample Folder", "Log Test", append = true, useWorkManager = true, viewLifecycleOwner = this)
    //    mShowMeDev.filemanWM?.filemanFeedback?.observe(this, Observer { output->
    //      Check FilemanFeedback class
    //      output.message?.let { Log.d("ShowMe", it) }
    //    })


    //You can add Prefix and/or Suffix to ShowMe object TAG. You can add the current classname to your tags for example
    mShowMeProduction.addTagSuffix( "-PROD")
    mShowMeDev.addTagSuffix("-DEV")

    buildSender()
  }

  //You can send ShowMe logs to a server using Sender (for now we only have one type -> ShowMeHttpSender)
  //I've no intention to add or replace any powerful REST API library inside ShowMe, so I'm using a simple barebone Http solution (please see ShowMeHttp).
  //You can send a Plain Text or JSON in HTTP Body. By using JSON you can send as ShowMeAPILogModel or any other model that you desire.
  //I'm using GSON library for JSON serialization because is the most used one. In the future I will add more options.
  //Just be aware that using your custom object requires Kotlin Reflection (see GsonBodyConverter -> convertToJson)
  private fun buildSender(){

//    val gsonConverter = GsonBodyConverter(Gson(), ShowMeAPILogModel()) //faster solution
    val userAPIData = UserAPIModel(url = "www.showme.com", message = "some test", project = "test")  //some user pojo object
    val gsonConverter = GsonBodyConverter(Gson(), userAPIData, UserAPIModel::payload.name) //showMe logs will be add to payload field.

    //CHANGE HERE
    val protocol = "http://"
    val host = "showme.com.br/"
    val path = "v1/SomeAPI"


    val headers : MutableMap<String, String?> = mutableMapOf<String,String?>()
    headers.put("Content-Type", "application/json")
    headers.put("application", "manager-portal")
    headers.put("application", "web-app-portal")
    headers.put("Cache-Control", "no-cache")
    headers.put("Accept", "application/json")

    //Building first HTTP Sender
    val httpSender1 :Sender? = ShowMeHttpSender.Builder(this)
      .active(true)
      .addHeaders(headers)
      .buildUrl(protocol, host, path, null)
      .addHeader("Connection", "keep-alive")
//      .setConverter(PlainTextConverter)
      .setConverter(gsonConverter)
      .setUseCache(true)
      .setReadTimeout(10000)
      .setConnectTimeout(10000)
      .build()

    //You can also build like this
    //    val httpSender1 = ShowMeHttpSender(false, headers, protocol, host, path)
//    val httpSender1 = ShowMeHttpSender(false, headers, protocol, host, path, null, PlainTextConverter)

    //Building second HTTP Sender
    val httpSender2 = ShowMeHttpSender(true, applicationContext,  headers, protocol, host, path, null, gsonConverter,10000,10000,true)

//    val res = httpSender2.sendLogSync("Your message here")  //you can use ShowMeHttpSender

    //Add Senders
    httpSender1?.let { mShowMeDev.addSender(it) }
    mShowMeDev.addSender(httpSender2)
  }


  private fun setSpinners() {
    val adapterLogCategory = ArrayAdapter.createFromResource(this, com.example.showme.R.array.spinner_log_category,
      android.R.layout.simple_expandable_list_item_1)
    adapterLogCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    spinner_log_type.adapter = adapterLogCategory
    spinner_log_type.onItemSelectedListener = this

    val adapterWatcherCategory = ArrayAdapter.createFromResource(this, com.example.showme.R.array.spinner_watcher_category,
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

  private fun setClickListeners() {
    btn_clear.setOnClickListener {
      tv_log.text = "Log"
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

  }


  private fun setLogText(msg: String?, logTypeSel : Int, watcherSel : Int) {
    val logType = LogType.values()[logTypeSel]
    val watcher = WatcherType.values()[watcherSel]

    val logMsg = ShowMe().d(msg!!, watcherType = watcher, logType = logType)
    mShowMeDev.d(msg,logType, watcher)  //if you want to test Sender or Fileman
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
