package com.example.showmelib

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import com.example.showme.ui.ShowMeSampleAct


class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val intent = Intent(this, ShowMeSampleAct::class.java)
    startActivity(intent)

  }



}
