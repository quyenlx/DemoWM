package com.ominext.demowm

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.util.TypedValue
import android.widget.ImageView
import android.widget.RelativeLayout

@SuppressLint("ViewConstructor")
class MouseView(context: Context?, private val colorRes: Int) : RelativeLayout(context) {
    private var img: ImageView

    init {
        layoutParams = RelativeLayout.LayoutParams(20F.dp2Px(), 35F.dp2Px())
        img = createMouse()
        val lp = RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        }
        addView(img, lp)
    }

    private val runnableNonClicked = {
        img.setImageResource(R.drawable.ic_mouse)
        DrawableCompat.setTint(img.drawable, ContextCompat.getColor(context, colorRes))
    }

    private fun createMouse(): ImageView {
        return ImageView(context)
                .apply {
                    setImageResource(R.drawable.ic_mouse)
                    DrawableCompat.setTint(this.drawable, ContextCompat.getColor(context, colorRes))
                }

    }

    fun setMouseClicked() {
        removeCallbacks(runnableNonClicked)
        postDelayed(runnableNonClicked, 200L)
        img.setImageResource(R.drawable.ic_mouse_clicked)
        DrawableCompat.setTint(img.drawable, ContextCompat.getColor(context, colorRes))
    }

    private fun Float.dp2Px(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics).toInt()
    }

}