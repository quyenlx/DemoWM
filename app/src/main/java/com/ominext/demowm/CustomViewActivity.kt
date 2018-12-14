package com.ominext.demowm

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.ominext.demowm.widget.dayview.MonthLoader
import com.ominext.demowm.widget.dayview.WeekViewEvent
import kotlinx.android.synthetic.main.activity_custom_view.*

class CustomViewActivity : AppCompatActivity(), MonthLoader.MonthChangeListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_view)
        weekView.mWeekViewLoader = MonthLoader(this)
    }

    override fun onMonthChange(newYear: Int, newMonth: Int): List<WeekViewEvent> {
        Log.e("CustomViewActivity","Year $newYear - Month $newMonth")
        return emptyList()
    }
}
