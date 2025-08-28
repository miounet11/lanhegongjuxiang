package com.lanhe.gongjuxiang.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lanhe.gongjuxiang.R

class TestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        Toast.makeText(this, "TestActivity启动成功！", Toast.LENGTH_SHORT).show()
    }
}
