package com.andrefilgs.showme.senders.http.converters


import com.andrefilgs.showme.senders.http.model.ShowMeHttpLogModel
import com.andrefilgs.showme.utils.Utils
import com.google.gson.Gson


sealed class Converters(){
}


object PlainTextConverter : Converters() {
}



/**
 * @param timestampField -> Pass field name as String but field must be Long (optional)
 * @param listFieldsValue -> Pass some data to your json object besides showMe log and timestamp (optional)
 */
class GsonBodyConverter<T : Any>(
    private val gson: Gson,
    private var classType: Class<T>,
    private var logField:String?=null,
    private var timestampField:String?=null,
    private var listFieldsValue: Map<String, String?>?=null

) : Converters() {

  private val TAG = "ShowMe-GsonBodyConverter"


  //  private val adapter: TypeAdapter<out Any>? = gson.getAdapter(TypeToken.get(type))

//  private val classType: Class<out T> = this.pojoObj::class.java


  fun toJson(value: T): String? {
    return try {
      gson.toJson(value, classType)
    }catch (e:Exception){
      null
    }
  }


  fun fromJson(value: String): T? {
    return try {
      gson.fromJson(value, classType)
    }catch (e:Exception){
      null
    }
  }


  /**
   * If you are not using ShowMeAPILogModel, pay attention to Reflection time consumption (activate logs) for JSON serialization
   */
  fun convertToJson(showMeLog: String): String? {
    return try {
      if(classType.name == ShowMeHttpLogModel::class.java.name){
//        Log.d(TAG, "Start convert to ShowMe Json $showMeLog")
        val res = gson.toJson(ShowMeHttpLogModel(showMeLog = showMeLog), ShowMeHttpLogModel::class.java)
//        Log.d(TAG, "End convert to ShowMe Json $res'")
        return res
      }

//      Log.d(TAG, "Start convert to Generic Json $showMeLog")
      val list = mutableListOf<Pair<String, Any?>>()
      list.add(Pair(logField ?: ShowMeHttpLogModel::showMeLog.name, showMeLog))
      list.add(Pair(timestampField ?: ShowMeHttpLogModel::timestamp.name, Utils.getNow()))

      listFieldsValue?.let {
        for ((key, value) in listFieldsValue!!) {
          try {
            list.add(Pair(key, value))
          }catch (e:Exception){
          }
        }
      }
      val temp = classType.newInstance()  //must create a new instance to avoid concurrency with multi thread
      Utils.setFields(temp as Any, list)
      gson.toJson(temp, classType)
//      val res = gson.toJson(pojoObj, classType)
//      Log.d(TAG, "End convert to Generic Json $res")
//      res
    }catch (e:Exception){
      null
    }
  }


  //This solution creates an instance of Class<T>, it can be useful when the object is not passed, just the Class
//  fun convertToJsonGenericReflectionOld(showMeLog: String): String? {
//    return try {
//      if(classType.name == ShowMeAPILogModel::class.java.name){
//        //        Log.d(TAG, "Start convert to ShowMe Json $showMeLog")
//        val res = gson.toJson(ShowMeAPILogModel(showMeLog = showMeLog), ShowMeAPILogModel::class.java)
//        //        Log.d(TAG, "End convert to ShowMe Json $res'")
//        return res
//      }
//
//      //      Log.d(TAG, "Start convert to Generic Json $showMeLog")
//      val apiObj = classType.newInstance()
//      val list = mutableListOf<Pair<String, Any?>>()
//      list.add(Pair(logField?: "showMeLog", showMeLog))
//      otherFields?.toCollection(list)
//      Utils.setFields(apiObj as Any, list)
//      gson.toJson(apiObj, classType)
//      //      val res = gson.toJson(apiObj, classType)
//      //      Log.d(TAG, "End convert to Generic Json $res")
//      //      res
//    }catch (e:Exception){
//      null
//    }
//  }


//  inline fun <reified T> fromJson(json: String): T {
//    return gson.fromJson(json, object: TypeToken<T>(){}.type)
//  }

}


