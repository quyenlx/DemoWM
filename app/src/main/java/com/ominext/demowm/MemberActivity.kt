package com.ominext.demowm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_member.*
import java.util.*

class MemberActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member)

        btnAdd.setOnClickListener {
            val random = Random()
            add(random.nextInt(10))
        }
    }

    private fun add(nextInt: Int) {
        val rootView = FrameLayout(this)
        val view = LayoutInflater.from(this).inflate(R.layout.item_member, rootView)
        val textView = view.findViewById<TextView>(R.id.tvMember)
        textView.text = nextInt.toString()
        viewMembers.addView(rootView)
    }
}
