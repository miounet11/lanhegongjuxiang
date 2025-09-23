package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.help_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
