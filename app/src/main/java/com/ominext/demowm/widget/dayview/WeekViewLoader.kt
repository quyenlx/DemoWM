package com.ominext.demowm.widget.dayview

import java.util.*

/**
 * Created by Admin on 12/14/2018.
 */
interface WeekViewLoader {
    /**
     * Convert a date into a double that will be used to reference when you're loading data.
     *
     * All periods that have the same integer part, define one period. Dates that are later in time
     * should have a greater return value.
     *
     * @param instance the date
     * @return The period index in which the date falls (floating point number).
     */
    fun toWeekViewPeriodIndex(instance: Calendar): Int

    /**
     * Load the events within the period
     * @param periodIndex the period to load
     * @return A list with the events of this period
     */
    fun onLoad(periodIndex: Int): List<WeekViewEvent>
}