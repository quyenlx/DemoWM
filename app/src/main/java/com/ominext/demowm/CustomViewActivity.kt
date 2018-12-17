package com.ominext.demowm

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_view)
        weekView.mWeekViewLoader = MonthLoader(this)
    }

    override fun onMonthChange(newYear: Int, newMonth: Int): List<WeekViewEvent> {
//        // Download events from network if it hasn't been done already. To understand how events are
//        // downloaded using retrofit, visit http://square.github.io/retrofit
//        if (!calledNetwork) {
//            val retrofit = RestAdapter.Builder()
//                    .setEndpoint("https://api.myjson.com/bins")
//                    .build()
//            val service = retrofit.create<MyJsonService>(MyJsonService::class.java)
//            service.listEvents(this)
//            calledNetwork = true
//        }

        // Return only the events that matches newYear and newMonth.
//        return events.filter { eventMatches(it, newYear, newMonth) }.take(10)
        return getEvent()
    }

    private fun getEvent(): List<WeekViewEvent> {
        val calendar = Calendar.getInstance()
        return (0..1).map {
            val startTime = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, it * 10)
            }

            val endTime = (calendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, it * 10 + 10)
            }
            return@map WeekViewEvent().apply {
                this.mName = "event $it"
                this.mStartTime = startTime
                this.mEndTime = endTime
                this.mColor = Color.RED
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
