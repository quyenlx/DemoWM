package com.ominext.demowm.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat

/**
 * Created by Admin on 12/13/2018.
 */
object ViewUtils {
    fun getBitmapFromXml(context: Context, drawableId: Int, width: Int=-1, height: Int=-1): Bitmap? {
        var drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = DrawableCompat.wrap(drawable).mutate()
        }

        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val widthTmp = if (width == -1) canvas.width else width
        val heightTmp = if (width == -1) canvas.height else height
        drawable.setBounds(0, 0, widthTmp, heightTmp)
        drawable.draw(canvas)

        return bitmap
    }
}