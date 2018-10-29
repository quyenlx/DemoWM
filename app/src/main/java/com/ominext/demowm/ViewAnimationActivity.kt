package com.ominext.demowm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.android.synthetic.main.activity_view_animation.*


class ViewAnimationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_animation)

        btnStart.setOnClickListener {
            val scaleX = img1.width.toFloat() / target.width.toFloat()
            val scaleY = img1.height.toFloat() / target.height.toFloat()
            val locationDest = IntArray(2)
            val locationView = IntArray(2)

            target.getLocationOnScreen(locationView)
            img1.getLocationOnScreen(locationDest)

            var transX = locationDest[0] - locationView[0]
            var transY = locationDest[1] - locationView[1]
            transX = (transX - target.width / 2 + img1.width / 2)
            transY = transY - target.height / 2 + img1.height / 2
            target.animate()
                    .scaleX(scaleX)
                    .scaleY(scaleY)
                    .translationX(transX.toFloat())
                    .translationY(transY.toFloat())
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .setDuration(500)
                    .start()
        }
    }
}
