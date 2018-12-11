package com.ominext.demowm.util

import java.text.SimpleDateFormat
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.util.*
import kotlin.collections.HashMap

object TimeUtils {

    const val TIME_FORMAT = "HH:mm"
    const val ISO_8601_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ"
    const val ISO_8601_DATE_TIME_FORMAT_RECEIVE = "yyyy-MM-dd'T'HH:mm:ssZZ"
    const val ISO_8601_DATE_TIME_FORMAT_SEND = "yyyy-MM-dd'T'HH:mm:ssZZ"
    const val SUNDAY = 0
    const val MONDAY = 1

    private val cacheDateFormat: HashMap<String, SimpleDateFormat> by lazy {
        HashMap<String, SimpleDateFormat>()
    }

    fun clear() {
        cacheDateFormat.clear()
    }

    fun getDateFormat(pattern: String = ISO_8601_DATE_TIME_FORMAT, local: Locale = Locale.getDefault()): SimpleDateFormat {
        if (cacheDateFormat[pattern] == null) {
            val format = SimpleDateFormat(pattern, local)
            cacheDateFormat[pattern] = format
        }

        return cacheDateFormat[pattern]!!
    }

    fun today(): Calendar {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        return today
    }
}

fun Calendar.isTheSameDay(calendar: Calendar): Boolean {
    return this.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            && this.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
}

