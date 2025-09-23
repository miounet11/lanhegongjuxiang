package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R

class FeedbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.feedback_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
