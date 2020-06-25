package com.andrefilgs.showme.senders.http


object WorkManagerDTO {

  //WorkManager DTO due to "Data cannot occupy more than 10240KB when serialized [android-workmanager]"
  var dataTransferObject = mutableMapOf<String,String?>()


  internal fun getWorkContent(key:String?):String?{
    if(key ==null) return null
    return dataTransferObject[key]
  }

  internal fun putWorkContent(key:String, value: String?){
    dataTransferObject[key] = value
  }

}