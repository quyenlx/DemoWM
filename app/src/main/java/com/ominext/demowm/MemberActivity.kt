package com.ominext.demowm

import android.content.res.Configuration
import android.content.res.Resources
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_member.*
import java.util.*

class MemberActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member)

        val xxx = (60F * 2).dp2Px()

        btnAdd.setOnClickListener {
            val random = Random()
            add(random.nextInt(10))
           val direction1 =  ViewCompat.canScrollHorizontally(scrollView,1)
           val direction2 =  ViewCompat.canScrollHorizontally(scrollView,-1)

            Log.e("MemberActivity","$direction1 - $direction2")
        }

        btnNext.setOnClickListener {
            scrollView.smoothScrollTo(scrollView.scrollX + xxx, 0)
            val direction1 =  ViewCompat.canScrollHorizontally(scrollView,1)
            val direction2 =  ViewCompat.canScrollHorizontally(scrollView,-1)

            Log.e("MemberActivity","$direction1 - $direction2")
        }

        btnPrevious.setOnClickListener {
            scrollView.smoothScrollTo(scrollView.scrollX - xxx, 0)
            val direction1 =  ViewCompat.canScrollHorizontally(scrollView,1)
            val direction2 =  ViewCompat.canScrollHorizontally(scrollView,-1)

            Log.e("MemberActivity","$direction1 - $direction2")
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val direction1 =  ViewCompat.canScrollHorizontally(scrollView,1)
        val direction2 =  ViewCompat.canScrollHorizontally(scrollView,-1)

        Log.e("MemberActivity","$direction1 - $direction2")
    }

    private fun add(nextInt: Int) {
        val rootView = FrameLayout(this)
        val view = LayoutInflater.from(this).inflate(R.layout.item_member, rootView)
        val textView = view.findViewById<TextView>(R.id.tvMember)
        textView.text = nextInt.toString()
        viewMembers.addView(rootView)
    }

    private fun Float.dp2Px(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics).toInt()
    }
}

