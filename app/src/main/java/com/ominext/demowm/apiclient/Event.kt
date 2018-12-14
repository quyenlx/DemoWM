package com.ominext.demowm.apiclient

import android.annotation.SuppressLint
import android.graphics.Color
import com.ominext.demowm.widget.dayview.WeekViewEvent
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Admin on 12/14/2018.
 */
class Event {
    var name: String? = null
    var dayOfMonth: Int? = null
    var startTime: String? = null
    var endTime: String? = null
    var color: String? = null

    @SuppressLint("SimpleDateFormat")
    fun toWeekViewEvent(): WeekViewEvent {
        // Parse time.
        val sdf = SimpleDateFormat("HH:mm")
        var start = Date()
        var end = Date()
        try {
            start = sdf.parse(startTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        try {
            end = sdf.parse(endTime)
        } catch (e: ParseException) {
            e.printStackTrace()
        }


        // Initialize start and end time.
        val now = Calendar.getInstance()
        val startTime = now.clone() as Calendar
        startTime.timeInMillis = start.time
        startTime.set(Calendar.YEAR, now.get(Calendar.YEAR))
        startTime.set(Calendar.MONTH, now.get(Calendar.MONTH))
        startTime.set(Calendar.DAY_OF_MONTH, dayOfMonth!!)
        val endTime = startTime.clone() as Calendar
        endTime.timeInMillis = end.time
        endTime.set(Calendar.YEAR, startTime.get(Calendar.YEAR))
        endTime.set(Calendar.MONTH, startTime.get(Calendar.MONTH))
        endTime.set(Calendar.DAY_OF_MONTH, startTime.get(Calendar.DAY_OF_MONTH))

        // Create an week view event.
        return WeekViewEvent().apply {
            this.mName = name
            this.mStartTime = startTime
            this.mEndTime = endTime
            this.mColor = Color.parseColor(color)
        }
    }
}