package com.ominext.demowm

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.ominext.demowm.apiclient.Event
import com.ominext.demowm.widget.dayview.MonthLoader
import com.ominext.demowm.widget.dayview.WeekViewEvent
import kotlinx.android.synthetic.main.activity_custom_view.*
import retrofit.Callback
import retrofit.RetrofitError
import retrofit.client.Response
import java.util.*

class CustomViewActivity : AppCompatActivity(), MonthLoader.MonthChangeListener, Callback<MutableList<Event>> {
    private val events = ArrayList<WeekViewEvent>()
    private var calledNetwork = false
    private val random = Random()
    private val max = 10
    private val min = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_view)
        weekView.mWeekViewLoader = MonthLoader(this)
    }

    override fun onMonthChange(newYear: Int, newMonth: Int): List<List<WeekViewEvent>> {
        return getEvent()
    }

    private fun getEvent(): List<List<WeekViewEvent>> {
        val calendar = Calendar.getInstance()
        return (0 until 1).map { memberIndex ->
            val color = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))
            return@map (0 until 1).map { eventIndex ->
                val start = memberIndex + eventIndex + random.nextInt(max - min + 1) + min
                val startTime = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 16)
                    set(Calendar.MINUTE, 0)
                }
                val end = if (start < 24) start + random.nextInt(max - min + 1) + min else 23
                val endTime = (calendar.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 17)
                    set(Calendar.MINUTE, 17)
                }
                return@map WeekViewEvent().apply {
                    this.mName = "event $memberIndex -  $eventIndex"
                    this.mStartTime = startTime
                    this.mEndTime = endTime
                    this.mColor = color
                }
            }
        }
    }

    private fun eventMatches(event: WeekViewEvent, year: Int, month: Int): Boolean {
        return event.mStartTime?.get(Calendar.YEAR) == year && event.mStartTime?.get(Calendar.MONTH) == month - 1 || event.mEndTime?.get(Calendar.YEAR) == year && event.mEndTime?.get(Calendar.MONTH) == month - 1
    }

    override fun success(t: MutableList<Event>, response: Response?) {
        this.events.clear()
        for (event in t) {
            this.events.add(event.toWeekViewEvent())
        }
        weekView.notifyDataSetChanged()
    }

    override fun failure(error: RetrofitError?) {
        error?.printStackTrace()
        Toast.makeText(this, R.string.async_error, Toast.LENGTH_SHORT).show()
    }
}
