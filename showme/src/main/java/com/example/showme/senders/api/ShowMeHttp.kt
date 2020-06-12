package com.example.showme.senders.api

import android.util.Log
import com.andrefilgs.fileman.auxiliar.orDefault
import com.example.showme.LogcatType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.StandardCharsets

internal class ShowMeHttp {

  companion object{

    var showLogs:Boolean=false

    const val TIMEOUT :Int = 5000
    const val CONNECT_TIMEOUT : Int = 5000
    const val USE_CACHE = false

    private val TAG = "ShowMe-Http"

    //todo 1000 add ShowMeLogger to entire ShowMe library
    private fun log(logcatType: LogcatType?=LogcatType.VERBOSE, logContent:String, showLog:Boolean?=showLogs){
      if(showLog.orDefault()){
        when(logcatType){
          LogcatType.VERBOSE -> Log.v(TAG, logContent)
          LogcatType.DEBUG -> Log.d(TAG, logContent)
          LogcatType.INFO -> Log.i(TAG, logContent)
          LogcatType.WARNING -> Log.w(TAG, logContent)
          LogcatType.ERROR -> Log.e(TAG, logContent)
          else -> Log.v(TAG, logContent)
        }
      }
    }

    /**
     * Return the new URL object from the given URL string
     */
    fun createUrl(stringUrl: String?, showLog: Boolean?=null): URL? {
      var url: URL? = null
      try {
        url = URL(stringUrl)
      } catch (e: MalformedURLException) {
        log(LogcatType.ERROR, "Problem building the URL: ${e.message}", showLog)
      }
      return url
    }

    /**
     * Return the new URL object from the given URL string
     *
     * @param protocol  -> http://, https://
     * @param host      -> somehost.com/
     * @param path      -> v1/apiName/
     * @param arguments -> A HashMap that will produce the following concatenated string: "?key=value&key2=value2..."
     */
    fun buildUrl(protocol: String?, host: String?, path: String?, arguments: Map<String, String>?): String? {
      if(protocol ==null && host == null && path == null) return null
      var mArgs = ""
      if (arguments != null) {
        mArgs = "?"
        for ((key, value) in arguments) {
          //
          mArgs = "$mArgs$key=$value&"
        }
        mArgs = mArgs.substring(0, mArgs.length - 1)
      }
      val urlStr = "$protocol$host${path ?: ""}$mArgs"
      val url: URL = createUrl(urlStr) ?: return null
//      return "$protocol$host${path ?: ""}$mArgs"
      return url.toString()
    }


    /**
     * Convert [InputStream] into a string which holds all the server response
     */
    @Throws(IOException::class)
    fun readFromStream(inputStream: InputStream?): String? {
      val output = StringBuilder()
      if (inputStream != null) {
        val inputStreamReader = InputStreamReader(inputStream, StandardCharsets.UTF_8)
        val reader = BufferedReader(inputStreamReader)
        var line = reader.readLine()
        while (line != null) {
          output.append(line)
          line = reader.readLine()
        }
      }
      return output.toString()
    }


    suspend fun makeRequestAsync(mUrl: String?, mMethod: String?, mBody: String?, mHeaders: Map<String, String?>?, readTimeout:Int?= TIMEOUT,
                                 connectTimeout:Int?= CONNECT_TIMEOUT, useCache:Boolean?= USE_CACHE, showLog: Boolean?=null): Deferred<HttpResponse?> {
      return GlobalScope.async {
        makeRequest(mUrl, mMethod, mBody, mHeaders, readTimeout, connectTimeout, useCache, showLog)
      }
    }

    @Throws(Exception::class)
    fun makeRequest(mUrl: String?, mMethod: String?, mBody: String?, mHeaders: Map<String, String?>?, readTimeout:Int?= TIMEOUT,
                    connectTimeout:Int?= CONNECT_TIMEOUT, useCache:Boolean?= USE_CACHE, showLog: Boolean?=null): HttpResponse? {
      val httpResponse = HttpResponse()
      val url: URL = createUrl(mUrl) ?: return null

      // If the URL is null, then return early.
      requireNotNull(mUrl) { "mURL is null" }
      var urlConnection: HttpURLConnection? = null
      var inputStream: InputStream? = null
      try {
        urlConnection = url.openConnection() as HttpURLConnection
        urlConnection.readTimeout = readTimeout ?: TIMEOUT
        urlConnection.connectTimeout = connectTimeout ?: CONNECT_TIMEOUT
        urlConnection.useCaches = useCache ?: USE_CACHE //using Cache is better for production
        urlConnection.requestMethod = mMethod // GET, POST,HEAD,OPTIONS,PUT,DELETE,TRACE
        urlConnection.setRequestProperty("Accept-Encoding", "gzip") //http://www.rgagnon.com/javadetails/java-HttpUrlConnection-with-GZIP-encoding.html
        //      urlConnection.setRequestProperty("api-key", K.API_KEY);
        //Headers
        if (mHeaders != null && mHeaders.isNotEmpty()) {
          for ((key, value) in mHeaders) {
            urlConnection.setRequestProperty(key, value)
          }
        }
        //Body
        if (mBody != null && mBody != "") {
          val os = urlConnection.outputStream
          os.write(mBody.toByteArray(charset("UTF-8")))
          os.close()
        }

        urlConnection.connect()

        //Headers Response
        val headers = urlConnection.headerFields
        for ((headerName, headerValues) in headers) {
          if (headerValues.size == 1) {
            headerName?.let {
              httpResponse.headers?.set(it, headerValues[0])
            }
          } else {
            //Header may have multiple values which are concatenated by ";"
            // https://stackoverflow.com/questions/43257459/why-does-getheaderfield-return-a-string-where-as-getheaderfields-return-map
            var arValues = ""
            for (value in headerValues) {
              arValues = "$arValues$value;"
            }
            arValues.substring(0, arValues.length - 1)
            httpResponse.headers?.set(headerName, arValues)
          }
        }

        inputStream = urlConnection.inputStream
        httpResponse.bodySent = mBody
        httpResponse.bodyResponse = readFromStream(inputStream)
        httpResponse.responseCode = urlConnection.responseCode.toString()
        httpResponse.method = urlConnection.requestMethod
        httpResponse.url = urlConnection.url.toString()

        if (urlConnection.responseCode in 200..299) {
          httpResponse.success = true
        }else{
          httpResponse.success = false
          log(LogcatType.WARNING, "HTTP response code: " + urlConnection.responseCode, showLog)
        }
      } catch (e: IOException) {
        Log.e(TAG, "Problem retrieving http answer due to: ${e.message}", e)
      } finally {
        urlConnection?.disconnect()
        inputStream?.close()
      }
      log(LogcatType.DEBUG, "Url: ${urlConnection?.url}\nMethod: ${httpResponse.method}\nHttp Code = ${httpResponse.responseCode}\nBody Response: ${httpResponse.bodyResponse}\nBody Sent: ${httpResponse.bodySent}".trimIndent(), showLog)
      return httpResponse
    }

  }

}



enum class HTTP_METHODS(val type:String){
  GET("GET"),
  POST("POST"),
  PUT("PUT"),
  PATCH("PATCH"),
  DELETE("DELETE"),
}