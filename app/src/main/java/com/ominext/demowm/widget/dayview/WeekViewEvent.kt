package com.ominext.demowm.widget.dayview

import com.ominext.demowm.util.isTheSameDay
import java.util.*

/**
 * Created by Admin on 12/14/2018.
 */
class WeekViewEvent {
    constructor()
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

    var minuteStart: Int = 0
        get() {
            return mStartTime!!.get(Calendar.MINUTE)
        }

    val hoursBetween: Float
        get() {
            val offset = mEndTime!!.timeInMillis - mStartTime!!.timeInMillis
            return offset / (1000 * 60 * 60F)
        }

    fun splitEvents(): List<WeekViewEvent> {
        val events = ArrayList<WeekViewEvent>()
        var endTime = this.mEndTime!!.clone() as Calendar

        val eventFirst: WeekViewEvent
        val otherDay: Calendar

        if (!this.mStartTime!!.isTheSameDay(endTime)) {
            endTime = this.mStartTime!!.clone() as Calendar
            endTime.set(Calendar.HOUR_OF_DAY, 23)
            endTime.set(Calendar.MINUTE, 59)
            endTime.set(Calendar.SECOND, 0)

            eventFirst = WeekViewEvent()
            eventFirst.mId = this.mId
            eventFirst.mName = this.mName
            eventFirst.mStartTime = this.mStartTime
            eventFirst.mEndTime = endTime
            eventFirst.mAllDay = this.mAllDay
            eventFirst.mColor = this.mColor
            events.add(eventFirst)

            // Add other days.
            otherDay = this.mStartTime!!.clone() as Calendar
            otherDay.add(Calendar.DATE, 1)

            var overDay: Calendar
            var endOfOverDay: Calendar
            var eventMore: WeekViewEvent

            while (!otherDay.isTheSameDay(this.mEndTime!!)) {
                overDay = otherDay.clone() as Calendar
                overDay.set(Calendar.HOUR_OF_DAY, 0)
                overDay.set(Calendar.MINUTE, 0)
                overDay.set(Calendar.SECOND, 0)

                endOfOverDay = overDay.clone() as Calendar
                endOfOverDay.set(Calendar.HOUR_OF_DAY, 23)
                endOfOverDay.set(Calendar.MINUTE, 59)
                endOfOverDay.set(Calendar.SECOND, 0)

                eventMore = WeekViewEvent()
                eventMore.mId = this.mId
                eventMore.mName = this.mName
                eventMore.mStartTime = overDay
                eventMore.mEndTime = endOfOverDay
                eventMore.mAllDay = this.mAllDay
                eventMore.mColor = this.mColor
                events.add(eventMore)

                // Add next day.
                otherDay.add(Calendar.DATE, 1)
            }

            if ((this.mEndTime!!.timeInMillis - this.mEndTime!!.get(Calendar.MILLISECOND)) > this.mStartTime!!.timeInMillis - this.mStartTime!!.get(Calendar.MILLISECOND)) {
                // Add last day.
                val startTime = this.mEndTime!!.clone() as Calendar
                startTime.set(Calendar.HOUR_OF_DAY, 0)
                startTime.set(Calendar.MINUTE, 0)

                val eventLast = WeekViewEvent()
                eventLast.mId = this.mId
                eventLast.mName = this.mName
                eventLast.mStartTime = startTime
                eventLast.mEndTime = this.mEndTime
                eventLast.mAllDay = this.mAllDay
                eventLast.mColor = this.mColor
                events.add(eventLast)
            }
        } else {
            events.add(this)
        }

        return events
    }
}