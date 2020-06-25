package com.andrefilgs.showmelib

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    goToShowMeSample()

    button.setOnClickListener {
      goToShowMeSample()
    }
  }

  private fun goToShowMeSample(){
    val intent = Intent(this, ShowMeSampleAct::class.java)
    startActivity(intent)
  }



}
