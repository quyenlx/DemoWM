package com.ominext.demowm.widget.dayview

import java.util.*

/**
 * Created by Admin on 12/11/2018.
 */
interface DateTimeInterpreter {
    fun interpretDate(date: Calendar): String
    fun interpretTime(hour: Int): String
}