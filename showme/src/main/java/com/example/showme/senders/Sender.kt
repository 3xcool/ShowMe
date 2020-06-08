package com.example.showme.senders


import com.andrefilgs.fileman.auxiliar.orDefault
import com.example.showme.senders.api.HTTP_METHODS
import com.example.showme.senders.api.HttpResponse
import com.example.showme.senders.api.MyHttp
import com.example.showme.senders.api.converters.Converters
import com.example.showme.senders.api.converters.GsonBodyConverter
import com.example.showme.senders.api.converters.PlainTextConverter
import kotlinx.coroutines.*

sealed class Sender {
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



//todo 1000 add timeout, keep alive, connect timeout
/**
 *
 * @param mProtocol  -> http://, https://
 * @param mHost      -> somehost.com/
 * @param mPath      -> v1/apiName/
 * @param mArguments -> Pass a HashMap that will produce the following concatenated string: "?key=value&key2=value2..."
 */
internal class ShowMeHttpSender (override var mActive: Boolean?=null,
                                 private var mHeaders: MutableMap<String,String?>?=null,
                                 private var mProtocol: String?=null,
                                 private var mHost: String?=null,
                                 private var mPath:String?=null,
                                 private var mArguments: MutableMap<String, String>?=null,
                                 private var bodyConverter: Converters?= PlainTextConverter) : Sender(){

  private var mUrl :String? = MyHttp.buildUrl(mProtocol, mHost, mPath, mArguments)
  var mBodyConverter : Converters? = bodyConverter

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

  internal fun sendLog(content:String, url:String?=mUrl ): HttpResponse? = runBlocking {
    if(isSenderActive()){
      sendLogAsync(content, url)
    }
    null
  }

  private suspend fun sendLogAsync(content:String, url:String?=mUrl ): Deferred<HttpResponse?> {
    val res = GlobalScope.async(Dispatchers.Default) {
      MyHttp.makeRequest( url, HTTP_METHODS.POST.type, convertBody(content) ?: content, mHeaders)
    }
    return res //res.await()
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



  class Builder{

    private var active: Boolean?=null
    private var headers: MutableMap<String,String?>?=null
    private var protocol: String?=null
    private var host: String?=null
    private var path:String?=null
    private var arguments: MutableMap<String, String>?=null
    private var bodyConverter: Converters?= PlainTextConverter
    private var url :String? = null//MyHttp.buildUrl(mProtocol, mHost, mPath, mArguments)

    fun active(value:Boolean):Builder{
      this.active = value
      return this
    }

    fun buildUrl(protocol:String?, host:String?, path:String?, mArguments: MutableMap<String, String>?): Builder {
      this.protocol = protocol
      this.host = host
      this.path = path
      this.arguments = mArguments
      this.url = MyHttp.buildUrl(protocol,host, path, mArguments)
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

    private fun validateBuilder():Boolean{
      //      checkNotNull(mUrl) { "url == null" }
      return this.url != null
    }

    fun build(): Sender? {
      return if(validateBuilder()){
        ShowMeHttpSender(this.active, this.headers, this.protocol, this.host, this.path, this.arguments, this.bodyConverter )
      }else{
        null
      }
    }
  }
}
