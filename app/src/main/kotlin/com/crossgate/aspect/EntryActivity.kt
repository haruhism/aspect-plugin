package com.crossgate.aspect

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * @ClassName: EntryActivity
 * @Describe: 入口页
 * @Author: nil
 * @Date: 2024/11/27 18:14
 */
class EntryActivity : AppCompatActivity(R.layout.layout_main), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.main_start_button).setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val name = resources.getResourceEntryName(v.id)
        Log.i(TAG, "onClick, $name")
    }

    companion object {
        private const val TAG = "EntryActivity"
    }
}
