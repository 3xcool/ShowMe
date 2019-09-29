package com.example.showme.utils

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Auxiliar class to find UTF-8 chars
 */
class Utils() {

  companion object {


    fun utf8Encode(codePoint: Int) = String(intArrayOf(codePoint), 0, 1).toByteArray(Charsets.UTF_8)

    fun utf8Decode(bytes: ByteArray) = String(bytes, Charsets.UTF_8).codePointAt(0)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    //args: Array<String>
    fun example() {
      val codePoints = intArrayOf(0x0041, 0x00F6, 0x0416, 0x20AC, 0x1D11E)
      println("Char  Name                                 Unicode  UTF-8         Decoded")
      for (codePoint in codePoints) {
        var n = if (codePoint <= 0xFFFF) 4 else 5

//        System.out.printf("%-${n}c  %-35s  U+%05X  ", codePoint, Character.getName(codePoint), codePoint)
        Log.d("ShowMe" , java.lang.String.format("%-${n}c  %-35s  U+%05X  ", codePoint, Character.getName(codePoint), codePoint))

        val bytes = utf8Encode(codePoint)
        var s = ""
        for (byte in bytes) s += "%02X ".format(byte)
        val decoded = utf8Decode(bytes)
        n = if (decoded.toInt() <= 0xFFFF) 12 else 11
//        System.out.printf("%-${n}s  %c\n", s, decoded)

        Log.d("ShowMe" , java.lang.String.format("%-${n}s  %c\n", s, decoded))
      }
    }

    fun generateUtfChars(begin:Int =32, end: Int=100){
      val sb = StringBuilder()
      for (codePoint in begin..end){
        val bytes = utf8Encode(codePoint)
        val decoded = utf8Decode(bytes)
        sb.append(java.lang.String.format("%c", decoded).toString())
      }
//      Logger.debugEntire(sb.toString())
      Log.d("ShowMe" , sb.toString())
    }

    fun generateAllUtfChars(){
//      generateUtfChars(255,350)
//      generateUtfChars(351,4000)
//      generateUtfChars(4001,6000)
//      generateUtfChars(6001,8000)
//      generateUtfChars(8001,10000)
//      generateUtfChars(10001,12000)
//      generateUtfChars(12001,14000)
      //chinese
//      generateUtfChars(14001,16000)
//      generateUtfChars(16001,18000)
//      generateUtfChars(18001,20000)
//      generateUtfChars(20001,22000)
//      generateUtfChars(22001,24000)
//      generateUtfChars(24001,26000)
//      generateUtfChars(26001,28000)
//      generateUtfChars(150001,152000)

      //nice ones
//      generateUtfChars(0x2300,0x2416)
//      generateUtfChars(0x2000,0x2100)
//      generateUtfChars(0x2101,0x2201)
//      generateUtfChars(0x2201,0x2301)
//      generateUtfChars(0x2301,0x2401)
//      generateUtfChars(0x2417,0x2515)
//      generateUtfChars(0x2516,0x2615)
//      generateUtfChars(0x2617,0x2815)
//      generateUtfChars(0x2816,0x2900)
//      generateUtfChars(0x2615,0x2616)
    }
  }
}