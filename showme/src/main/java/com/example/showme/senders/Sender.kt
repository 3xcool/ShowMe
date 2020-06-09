package com.example.showme.senders


import android.content.Context
import androidx.work.*
import com.andrefilgs.fileman.auxiliar.orDefault
import com.example.showme.ShowMeConstants
import com.example.showme.senders.api.HTTP_METHODS
import com.example.showme.senders.api.HttpResponse
import com.example.showme.senders.api.HttpWorker
import com.example.showme.senders.api.ShowMeHttp
import com.example.showme.senders.api.converters.Converters
import com.example.showme.senders.api.converters.GsonBodyConverter
import com.example.showme.senders.api.converters.PlainTextConverter
import kotlinx.coroutines.*

sealed class Sender {
  abstract val name:String
  abstract var mActive:Boolean?


  internal fun getNow():Long{
    return System.currentTimeMillis()
  }

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
 * @param mContext   -> For WorkManager
 * @param mProtocol  -> http://, https://
 * @param mHost      -> somehost.com/
 * @param mPath      -> v1/apiName/
 * @param mArguments -> Pass a HashMap that will produce the following concatenated string: "?key=value&key2=value2..."
 * @param bodyConverter -> Send as PlainText or JSON. See Converters class for more info
 */
class ShowMeHttpSender (override var mActive: Boolean?=null,
                                 private var mContext: Context,
                                 private var mHeaders: MutableMap<String,String?>?=null,
                                 private var mProtocol: String?=null,
                                 private var mHost: String?=null,
                                 private var mPath:String?=null,
                                 private var mArguments: MutableMap<String, String>?=null,
                                 private var bodyConverter: Converters?= PlainTextConverter,
                                 private var timeout:Int? = ShowMeHttp.TIMEOUT,
                                 private var connectTimeout:Int? = ShowMeHttp.CONNECT_TIMEOUT,
                                 private var useCache:Boolean? = ShowMeHttp.USE_CACHE) : Sender(){



  private var mUrl :String? = ShowMeHttp.buildUrl(mProtocol, mHost, mPath, mArguments)
  var mBodyConverter : Converters? = bodyConverter

  private var workManager: WorkManager = WorkManager.getInstance(mContext)

  override val name: String
    get() = ::ShowMeHttpSender.name


  /**
   * User can call this fun besides ShowMe automatic sender calls
   * Be careful because this fun is using "runBlocking", so it is a synchronous way.
   * It will call the API from Dispatchers.Default because it's using Coroutine under the hood
   */
  fun sendLogSync(content:String, url:String?=mUrl ): HttpResponse? = runBlocking {
    if(!isSenderActive()) return@runBlocking null
    sendLogAsync(content, url).await()
  }

  internal fun sendLog(content:String, url:String?=mUrl ) = runBlocking {
    if(isSenderActive()){
      sendLogWM(content, url)
    }
  }

  private suspend fun sendLogAsync(content:String, url:String?=mUrl ): Deferred<HttpResponse?> {
    val res = GlobalScope.async(Dispatchers.Default) {
      ShowMeHttp.makeRequest( url, HTTP_METHODS.POST.type, convertBody(content) ?: content, mHeaders, timeout, connectTimeout, useCache)
    }
    return res //res.await()
  }


  //using WorkManager
  private fun sendLogWM(content:String, url:String?=mUrl ){
    GlobalScope.launch(Dispatchers.Default) {
      val httpRequest = buildHttpWorker(ShowMeConstants.WORKER_TAG_HTTP, url, HTTP_METHODS.POST.type, convertBody(content)?: content, mHeaders, timeout, connectTimeout, useCache )
      workManager.beginUniqueWork(getNow().toString(), ExistingWorkPolicy.REPLACE, httpRequest).enqueue()
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
  private fun buildHttpWorker(tag:String, url:String?, method:String?, body:String, headers: Map<String, String?>?,readTimeout:Int?=null, connectTimeout:Int?= null, useCache:Boolean?= null): OneTimeWorkRequest {
    val inputData = Data.Builder()
    inputData.putString(ShowMeConstants.KEY_HTTP_TAG, tag)
    inputData.putString(ShowMeConstants.KEY_HTTP_URL, url)
    inputData.putString(ShowMeConstants.KEY_HTTP_METHOD, method)
    inputData.putString(ShowMeConstants.KEY_HTTP_BODY, body)
    readTimeout?.let { inputData.putInt(ShowMeConstants.KEY_HTTP_TIMEOUT, readTimeout)}
    connectTimeout?.let { inputData.putInt(ShowMeConstants.KEY_HTTP_CONNECT_TIMEOUT, connectTimeout)}
    useCache?.let { inputData.putBoolean(ShowMeConstants.KEY_HTTP_USE_CACHE, useCache)}
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
    private var headers: MutableMap<String,String?>?=null
    private var protocol: String?=null
    private var host: String?=null
    private var path:String?=null
    private var arguments: MutableMap<String, String>?=null
    private var bodyConverter: Converters?= PlainTextConverter
    private var url :String? = null//MyHttp.buildUrl(mProtocol, mHost, mPath, mArguments)
    private var readTimeout :Int? = null
    private var connectTimeout :Int? = null
    private var useCache :Boolean? = null

    fun active(value:Boolean):Builder{
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

    fun addHeader(key:String, value:String):Builder{
      if(this.headers == null) hashMapOf<String,String?>()
      this.headers?.put(key, value)
      return this
    }

    fun addHeaders(headers: Map<String, String?>):Builder{
      if(this.headers == null) hashMapOf<String,String?>()
      this.headers?.putAll(headers)
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

    private fun validateBuilder():Boolean{
      //      checkNotNull(mUrl) { "url == null" }
      return this.url != null
    }

    fun build(): Sender? {
      return if(validateBuilder()){
        ShowMeHttpSender(this.active, this.context, this.headers, this.protocol, this.host, this.path, this.arguments, this.bodyConverter )
      }else{
        null
      }
    }
  }
}
