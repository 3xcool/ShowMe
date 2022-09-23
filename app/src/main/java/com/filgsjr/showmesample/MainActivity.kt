package com.filgsjr.showmesample

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.andrefilgs.showme.BuildConfig
import com.filgsjr.showmesample.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity(R.layout.activity_main) {

  private var _binding: ActivityMainBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    _binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    goToShowMeSample()
    binding.button.setOnClickListener {
      goToShowMeSample()
    }
  }

  private fun goToShowMeSample(){
    val intent = Intent(this, ShowMeSampleAct::class.java)
    startActivity(intent)
  }



}
