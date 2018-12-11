package com.ominext.demowm

import android.app.Application
import net.danlew.android.joda.JodaTimeAndroid

/**
 * Created by Admin on 12/11/2018.
 */
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        JodaTimeAndroid.init(this)
    }
}