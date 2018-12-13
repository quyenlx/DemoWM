package com.ominext.demowm.widget.dayview

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Created by Admin on 12/13/2018.
 */
class CusRecyclerView(context: Context, attrs: AttributeSet?) : RecyclerView(context, attrs) {

    override fun onTouchEvent(e: MotionEvent?): Boolean {
        return false
    }
}