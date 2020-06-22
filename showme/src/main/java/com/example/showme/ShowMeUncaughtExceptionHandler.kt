package com.example.showme

import android.content.Context
import com.andrefilgs.fileman.auxiliar.orDefault
import com.example.showme.utils.Utils
import java.io.*

/**
 * UEH ShowMe class
 * Use this class for writing stacktrace to uncaught exception errors
 *
 * @param addNewline -> add "\n" for readability when reading file
 * @param extraInfo -> add some key value pair if you need
 */
class ShowMeUncaughtExceptionHandler(private val context: Context, private val filename: String? = UEH_FILE_NAME, private val addNewline: Boolean? = true, private val extraInfo: Map<String, String>? = null) :
  Thread.UncaughtExceptionHandler {

  companion object {
    internal val UEH_FILE_NAME = "ShowMe_UEH"
  }

  init {
    checkUEHExists()
  }

  fun checkUEHExists(): Boolean {
    return if (getUEHcontent() != null) {
      ShowMeLogger.log("⛔ ATTENTION: Check Uncaught Error Handler file -> $filename. Delete file if this error is already known", LogcatType.WARNING, null)
      true
    } else {
      false
    }
  }

  private val defaultUEH: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

  override fun uncaughtException(t: Thread, e: Throwable) {
    val localizedMessage = e.cause?.cause?.localizedMessage
    val stacktrace = e.cause?.cause?.stackTrace
    val className = stacktrace?.let { it[0].className } ?: ""
    val method = stacktrace?.let { it[0].methodName } ?: ""
    val line = stacktrace?.let { it[0].lineNumber } ?: ""

    val sb = StringBuilder()
    sb.append("═════════ Stack trace ═════════\n")
    sb.append("Now: ${Utils.convertTime(Utils.getNow(), Utils.getNowFormat())}\n")
    sb.append("Error Message: $localizedMessage\n")
    sb.append("Class Name: $className\n")
    sb.append("Class Method: $method\n")
    sb.append("Class Line: $line\n\n")
    sb.append(getRawException(e))
    extraInfo?.let {
      sb.append("══════════ Extra Info ══════════\n")
      it.forEach { (key, value) ->
        sb.append("$key: $value\n")
      }
    }
    sb.append("═════════ ShowMe Info ═════════\n")
    sb.append("App ID: " + BuildConfig.APPLICATION_ID + "\n")
    sb.append("Version Code: " + BuildConfig.VERSION_CODE + "\n")
    sb.append("Version Name: " + BuildConfig.VERSION_NAME + "\n")
    sb.append("Build Type: " + BuildConfig.BUILD_TYPE + "\n")
    sb.append("Debug: " + BuildConfig.DEBUG + "\n")
    sb.append("Flavor: " + BuildConfig.FLAVOR + "\n")
    sb.append("═══════════════════════════\n")
    writeUEHException(sb.toString())
    defaultUEH?.uncaughtException(t, e)
  }

  /**
   * For uncaught Errors indeed
   */
  private fun getRawException(e: Throwable) :String{
    val sb = java.lang.StringBuilder()
    sb.append("═══════════ RAW EXCEPTION ═══════════\n\n")
    sb.append("$e".trimIndent())
    val arr = e.stackTrace
    for (i in arr.indices) {
      sb.append("${arr[i]}\n")
    }

    // If the exception was thrown in a background thread inside AsyncTask, then the actual exception can be found with getCause
    sb.append("\n═══════════ CAUSE ═══════════\n\n")
    //    Throwable cause = e.getCause();
    val cause = System.err //printStackTrace()
    if (cause != null) {
      sb.append("$cause".trimIndent())
      //      arr = cause.getStackTrace();
      for (i in arr.indices) {
          sb.append("${arr[i]}")
      }
    }
    sb.append("═══════════════════════════\n\n")
    return sb.toString()
  }


  //  private fun writeUEHExceptionOld(content:String){
  //    try {
  //      val trace: FileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)
  //      trace.write(content.toByteArray())
  //      trace.flush()
  //      trace.close()
  //    } catch (ioe: IOException) {
  //      ShowMeLogger.log("${ioe.message}", LogcatType.DEBUG,null)
  //    }
  //  }

  private fun writeUEHException(content: String) {
    val file = File(context.getFilesDir(), "")
    if (!file.exists()) {
      file.mkdir()
    }
    try {
      val gpxfile = File(file, filename ?: UEH_FILE_NAME)
      val writer = FileWriter(gpxfile)
      writer.append("\n\n$content")
      writer.flush()
      writer.close()
    } catch (e: Exception) {
      ShowMeLogger.log("${e.message}", LogcatType.ERROR, null)
    }
  }

  fun getUEHcontent(): String? {
    val sb = StringBuilder()
    try {
      val reader = BufferedReader(InputStreamReader(context.openFileInput(filename)))
      var line: String? = ""
      while (line != null) {
        line = reader.readLine()
        line?.let {
          sb.append(line)
          if (addNewline.orDefault()) sb.append("\n") //adding "\n" for readability
        }
      }
      reader.close()
    } catch (fnfe: FileNotFoundException) {
      ShowMeLogger.log("${fnfe.message}", LogcatType.ERROR, null)
      return null
    } catch (ioe: IOException) {
      ShowMeLogger.log("${ioe.message}", LogcatType.ERROR, null)
      return null
    }
    return sb.toString()
  }


  fun deleteUEHfile() {
    try {
      context.deleteFile(filename)
    } catch (e: Exception) {
      ShowMeLogger.log("${e.message}", LogcatType.ERROR, null)
    }
  }

}