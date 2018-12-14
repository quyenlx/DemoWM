package com.ominext.demowm.widget.dayview

import com.ominext.demowm.util.isTheSameDay
import java.util.*

/**
 * Created by Admin on 12/14/2018.
 */
class WeekViewEvent {
    constructor(id: Long, name: String?, location: String?, startTime: Calendar?, endTime: Calendar?, allDay: Boolean) {
        this.mId = id
        this.mName = name
        this.mLocation = location
        this.mStartTime = startTime
        this.mEndTime = endTime
        this.mAllDay = allDay
    }

    var mId: Long = 0
    var mStartTime: Calendar? = null
    var mEndTime: Calendar? = null
    var mName: String? = null
    var mLocation: String? = null
    var mColor: Int = 0
    var mAllDay: Boolean = false

    fun splitWeekViewEvents(): List<WeekViewEvent> {
        //This function splits the WeekViewEvent in WeekViewEvents by day
        val events = ArrayList<WeekViewEvent>()
        // The first millisecond of the next day is still the same day. (no need to split events for this).
        var endTime = this.mEndTime!!.clone() as Calendar
        endTime.add(Calendar.MILLISECOND, -1)
        if (mStartTime!!.isTheSameDay(mEndTime!!)) {
            endTime = this.mStartTime!!.clone() as Calendar
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)
            val event1 = WeekViewEvent(this.mId, this.mName, this.mLocation, this.mStartTime, endTime, this.mAllDay)
            event1.mColor = this.mColor
            events.add(event1)

            // Add other days.
            val otherDay = this.mStartTime!!.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)
            while (!otherDay.isTheSameDay(this.mEndTime!!)) {
                val overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                val endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                val eventMore = WeekViewEvent(this.mId, this.mName, null, overDay, endOfOverDay, this.mAllDay)
                eventMore.mColor = this.mColor
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            // Add last day.
            val startTime = this.mEndTime!!.clone() as Calendar
            startTime.set(Calendar.HOUR_OF_DAY, 0)
            startTime.set(Calendar.MINUTE, 0)
            val event2 = WeekViewEvent(this.mId, this.mName, mLocation, startTime, this.mEndTime!!, this.mAllDay)
            event2.mColor = this.mColor
            events.add(event2)
        } else {
            events.add(this)
        }

        return events
    }
}