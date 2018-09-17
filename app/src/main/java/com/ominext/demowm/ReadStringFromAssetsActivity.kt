package com.ominext.demowm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader


class ReadStringFromAssetsActivity : AppCompatActivity() {
    companion object {
        val TAG = ReadStringFromAssetsActivity::class.java.simpleName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_string_from_assets)

        val buf = StringBuilder()

        val input = assets.open("response-data-export.json")
        val br = BufferedReader(InputStreamReader(input, "UTF-8"))
        var str = br.readLine()
        while (str != null) {
            buf.append(str)
            str = br.readLine()
        }
        br.close()
        Log.i(TAG, buf.toString())
    }
}
