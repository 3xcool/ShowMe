package com.andrefilgs.showme.senders


import android.content.Context
import androidx.work.*
import com.andrefilgs.fileman.auxiliar.orDefault
import com.andrefilgs.showme.model.ShowMeConstants
import com.andrefilgs.showme.senders.http.*
import com.andrefilgs.showme.senders.http.converters.Converters
import com.andrefilgs.showme.senders.http.converters.GsonBodyConverter
import com.andrefilgs.showme.senders.http.converters.PlainTextConverter
import com.andrefilgs.showme.utils.Utils
import kotlinx.coroutines.*
import java.util.*

sealed class Sender {
  abstract val id:String?
  abstract val name:String
  abstract var mActive:Boolean?



//  fun <T> asList(vararg ts: T): List<T> {
//    val result = ArrayList<T>()
//    for (t in ts) // ts is an Array
//      result.add(t)
//    return result
//  }

//  companion object{
//
//    fun <T:Any> Builder(type: T ):Any?{
//      return when(type){
//        is ShowMeHttpSender -> return ShowMeHttpSender(true, null, "null", "null")
//        else -> null
//      }
//    }
//  }

  fun isSenderActive():Boolean{
    return mActive.orDefault(false)
  }

  val getName:String
    get() = when (this) {
      is ShowMeHttpSender -> name
    }
}



/**
 * @param mActive    -> Activate Sender
 * @param id        -> Just for identification
 * @param mContext   -> For WorkManager
 * @param mProtocol  -> http://, https://
 * @param mHost      -> somehost.com/
 * @param mPath      -> v1/apiName/
 * @param mArguments -> Pass a HashMap that will produce the following concatenated string: "?key=value&key2=value2..."
 * @param bodyConverter -> Send as PlainText or JSON. See Converters class for more info
 */
class ShowMeHttpSender (override var mActive: Boolean?=null,
                        override val id: String?=UUID.randomUUID().toString(),
                        private var mContext: Context,
                        private var mHeaders: MutableMap<String,String?>?=null,
                        private var mProtocol: String?=null,
                        private var mHost: String?=null,
                        private var mPath:String?=null,
                        private var mArguments: MutableMap<String, String>?=null,
                        private var bodyConverter: Converters?= PlainTextConverter,
                        private var timeout:Int? = ShowMeHttp.TIMEOUT,
                        private var connectTimeout:Int? = ShowMeHttp.CONNECT_TIMEOUT,
                        private var useCache:Boolean? = ShowMeHttp.USE_CACHE,
                        private var useWorkManager: Boolean?=true,
                        private var showHttpLogs: Boolean?=false
                        ) : Sender(){



  private var mUrl :String? = ShowMeHttp.buildUrl(mProtocol, mHost, mPath, mArguments)
  var mBodyConverter : Converters? = bodyConverter

  var workManager: WorkManager = WorkManager.getInstance(mContext)
  var wmCounter:Int = 0 //for showMeWorkId

  private val baseCoroutineScope = CoroutineScope(Dispatchers.Default)


  override val name: String
    get() = ::ShowMeHttpSender.name


  fun setUrl(url:String) { this.mUrl = url }

  fun enableWorkManager() { this.useWorkManager = true }
  fun disableWorkManager(){ this.useWorkManager = false}
  fun enableHttpLogs() { this.showHttpLogs = true }
  fun disableHttpLogs() { this.showHttpLogs = false }


  fun getShowMeWorkIDCounter(): Int {
    wmCounter += 1
    return wmCounter
  }

  /**
   * User can call this fun besides ShowMe automatic sender calls
   * Be careful because this fun is using "runBlocking", so it is a synchronous way.
   * It will call the API from Dispatchers.Default because it's using Coroutine under the hood
   */
  fun sendLogSync(content:String, url:String?= mUrl ): HttpResponse? = runBlocking {
    if(!isSenderActive()) return@runBlocking null
    sendLogAsync(content, url).await()
  }

  internal fun sendLog(content:String, url:String?= mUrl ) {
    if(isSenderActive()){
      if (useWorkManager.orDefault(false)){
        sendLogWM(content, url)
      }else{
        sendLogLaunch(content, url)
      }
    }
  }

  private fun sendLogAsync(content:String, url:String?= mUrl ): Deferred<HttpResponse?> {
    val res = baseCoroutineScope.async(Dispatchers.Default) {
      ShowMeHttp.makeRequest( url, HTTP_METHODS.POST.type, convertBody(content) ?: content, mHeaders, timeout, connectTimeout, useCache, showHttpLogs)
    }
    return res //res.await()
  }


  //using WorkManager
  private fun sendLogWM(content:String, url:String?=mUrl ){
    baseCoroutineScope.launch(Dispatchers.Default) {
      val id = UUID.randomUUID().toString()
      val httpRequest = buildHttpWorker(ShowMeConstants.WORKER_TAG_HTTP, url, HTTP_METHODS.POST.type, convertBody(content)?: content, mHeaders, timeout, connectTimeout, useCache, showHttpLogs )
      workManager.beginUniqueWork(id, ExistingWorkPolicy.REPLACE, httpRequest).enqueue()
    }
  }

  private fun sendLogLaunch(content:String, url:String?=mUrl ){
    baseCoroutineScope.launch(Dispatchers.Default) {
      ShowMeHttp.makeRequest( url, HTTP_METHODS.POST.type, convertBody(content) ?: content, mHeaders, timeout, connectTimeout, useCache, showHttpLogs)
    }
  }


  //  API needs to be a JSON body
  private fun convertBody(logContent: String):String?{
    return when(mBodyConverter){
      is GsonBodyConverter<*> -> (mBodyConverter as GsonBodyConverter<*>).convertToJson(logContent)
      is PlainTextConverter -> logContent
//      is String.Companion -> logContent
      else -> logContent
    }
  }

  /**
   * Just to flag to UI the end of work
   */
  private fun buildHttpWorker(tag:String, url:String?, method:String?, body:String, headers: Map<String, String?>?,readTimeout:Int?=null, connectTimeout:Int?= null,
                              useCache:Boolean?= null, showHttpLogs:Boolean?=null): OneTimeWorkRequest {
    val inputData = Data.Builder()
    inputData.putString(ShowMeConstants.KEY_HTTP_TAG, tag)
    inputData.putString(ShowMeConstants.KEY_HTTP_URL, url)
    inputData.putString(ShowMeConstants.KEY_HTTP_METHOD, method)

//    inputData.putString(ShowMeConstants.KEY_HTTP_BODY, body) //USE DTO
    val showMeKey = Utils.getNow().toString() + getShowMeWorkIDCounter()  //some random key to avoid collision
    inputData.putString(ShowMeConstants.KEY_HTTP_SHOW_ME_ID, showMeKey)
    WorkManagerDTO.putLogContent(showMeKey, body)

    readTimeout?.let { inputData.putInt(ShowMeConstants.KEY_HTTP_TIMEOUT, readTimeout)}
    connectTimeout?.let { inputData.putInt(ShowMeConstants.KEY_HTTP_CONNECT_TIMEOUT, connectTimeout)}
    useCache?.let { inputData.putBoolean(ShowMeConstants.KEY_HTTP_USE_CACHE, useCache)}
    showHttpLogs?.let { inputData.putBoolean(ShowMeConstants.KEY_HTTP_SHOW_HTTP_LOGS, showHttpLogs)}
    headers?.let { ShowMeConstants.headers = it.toMutableMap() }
    return OneTimeWorkRequest.Builder(HttpWorker::class.java)
      .setConstraints(Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
      )
      .addTag(tag)
      .setInputData(inputData.build())
      .build()
  }



  class Builder(private var context: Context){

    private var active: Boolean?=null
    private var id: String?=null
    private var headers: MutableMap<String,String?> = mutableMapOf()
    private var protocol: String?=null
    private var host: String?=null
    private var path:String?=null
    private var arguments: MutableMap<String, String>?=null
    private var bodyConverter: Converters?= PlainTextConverter
    private var url :String? = null//MyHttp.buildUrl(mProtocol, mHost, mPath, mArguments)
    private var readTimeout :Int? = null
    private var connectTimeout :Int? = null
    private var useCache :Boolean? = null
    private var useWorkManager :Boolean? = false
    private var showHttpLogs :Boolean? = false

    fun active(value:Boolean): Builder {
      this.active = value
      return this
    }

    fun buildUrl(protocol:String?, host:String?, path:String?, mArguments: MutableMap<String, String>?): Builder {
      this.protocol = protocol
      this.host = host
      this.path = path
      this.arguments = mArguments
      this.url = ShowMeHttp.buildUrl(protocol,host, path, mArguments)
      return this
    }

    fun setId(value:String): Builder {
      this.id = value
      return this
    }

    fun addHeader(key:String, value:String): Builder {
      this.headers[key] = value
      return this
    }

    fun addHeaders(headers: Map<String, String?>): Builder {
      this.headers.putAll(headers)
      return this
    }

    fun setConverter(converter: Converters): Builder {
      this.bodyConverter = converter
      return this
    }

    fun setReadTimeout(timeout: Int?): Builder {
      this.readTimeout = timeout
      return this
    }
    fun setConnectTimeout(timeout: Int?): Builder {
      this.connectTimeout = timeout
      return this
    }

    fun setUseCache(useCache: Boolean?): Builder {
      this.useCache = useCache
      return this
    }

    fun setUseWorkManager(useWorkManager: Boolean?): Builder {
      this.useWorkManager = useWorkManager
      return this
    }

    fun showHttpLogs(value: Boolean?): Builder {
      this.showHttpLogs = value
      return this
    }

    private fun validateBuilder():Boolean{
      //      checkNotNull(mUrl) { "url == null" }
      return this.url != null
    }

    fun build(): Sender? {
      return if(validateBuilder()){
        ShowMeHttpSender(this.active,this.id ?: UUID.randomUUID().toString(), this.context, this.headers, this.protocol, this.host, this.path, this.arguments, this.bodyConverter,
          this.readTimeout, this.connectTimeout, this.useCache, this.useWorkManager, this.showHttpLogs)
      }else{
        null
      }
    }
  }
}
