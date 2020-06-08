package com.example.showme.senders.api.converters


import com.example.showme.senders.api.model.ShowMeAPILogModel
import com.example.showme.utils.Utils
import com.google.gson.Gson


sealed class Converters(){
}


internal object PlainTextConverter : Converters() {
}


internal class GsonBodyConverter<T : Any>(
    private val gson: Gson,
    private var pojoObj: T,
    private var logField:String?=null) :Converters() {


  //  private val adapter: TypeAdapter<out Any>? = gson.getAdapter(TypeToken.get(type))

  private val classType: Class<out T> = this.pojoObj::class.java


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
      if(classType.name == ShowMeAPILogModel::class.java.name){
//        Log.d("ShowMe-Gson", "Start convert to ShowMe Json $showMeLog")
        val res = gson.toJson(ShowMeAPILogModel(showMeLog = showMeLog), ShowMeAPILogModel::class.java)
//        Log.d("ShowMe-Gson", "End convert to ShowMe Json $res'")
        return res
      }

//      Log.d("ShowMe-Gson", "Start convert to Generic Json $showMeLog")
      val list = mutableListOf<Pair<String, Any?>>()
      list.add(Pair(logField ?: ShowMeAPILogModel::showMeLog.name, showMeLog))
      Utils.setFields(pojoObj as Any, list)
      gson.toJson(pojoObj, classType)
//      val res = gson.toJson(pojoObj, classType)
//      Log.d("ShowMe-Gson", "End convert to Generic Json $res")
//      res
    }catch (e:Exception){
      null
    }
  }


  //This solution creates an instance of Class<T>, it can be useful when the object is not passed, just the Class
//  fun convertToJsonGenericReflectionOld(showMeLog: String): String? {
//    return try {
//      if(classType.name == ShowMeAPILogModel::class.java.name){
//        //        Log.d("ShowMe-Gson", "Start convert to ShowMe Json $showMeLog")
//        val res = gson.toJson(ShowMeAPILogModel(showMeLog = showMeLog), ShowMeAPILogModel::class.java)
//        //        Log.d("ShowMe-Gson", "End convert to ShowMe Json $res'")
//        return res
//      }
//
//      //      Log.d("ShowMe-Gson", "Start convert to Generic Json $showMeLog")
//      val apiObj = classType.newInstance()
//      val list = mutableListOf<Pair<String, Any?>>()
//      list.add(Pair(logField?: "showMeLog", showMeLog))
//      otherFields?.toCollection(list)
//      Utils.setFields(apiObj as Any, list)
//      gson.toJson(apiObj, classType)
//      //      val res = gson.toJson(apiObj, classType)
//      //      Log.d("ShowMe-Gson", "End convert to Generic Json $res")
//      //      res
//    }catch (e:Exception){
//      null
//    }
//  }


//  inline fun <reified T> fromJson(json: String): T {
//    return gson.fromJson(json, object: TypeToken<T>(){}.type)
//  }

}


