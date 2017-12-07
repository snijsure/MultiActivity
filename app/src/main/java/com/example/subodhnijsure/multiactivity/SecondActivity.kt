package com.example.subodhnijsure.multiactivity

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class SecondActivity : LocationAwareActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)
    }
}
