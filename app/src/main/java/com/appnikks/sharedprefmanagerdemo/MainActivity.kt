package com.appnikks.sharedprefmanagerdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferenceManager: Sample1SharedPreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPreferenceManager = Sample1SharedPreferenceManager.getInstance(this)
        sharedPreferenceManager.isFirstRunDone = true
    }
}
