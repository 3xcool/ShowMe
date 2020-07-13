package com.andrefilgs.showme.senders.http


object WorkManagerDTO {

  //WorkManager DTO due to "Data cannot occupy more than 10240KB when serialized [android-workmanager]"
  private var dataTransferObject = mutableMapOf<String,String?>()

  private var fifoLogList = mutableListOf<String>()


  internal fun getLogContent(key:String?):String?{
    if(key ==null) return null
//    return dataTransferObject[key]
    val output = fifoLogList[0]
    fifoLogList.removeAt(0)
    return output
  }

  internal fun putLogContent(key:String, value: String?){
//    dataTransferObject[key] = value
    value?.let{fifoLogList.add(it)}
  }

}