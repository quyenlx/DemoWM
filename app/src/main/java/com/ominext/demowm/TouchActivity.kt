package com.ominext.demowm

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_touch.*
import java.util.*

class TouchActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_touch)

        val image = ImageView(this)
        image.setImageResource(R.drawable.ic_mouse)
        rootView.addView(image)
        image.x = 0F
        image.y = 0F

        btnMove.setOnClickListener {
            val xx = Random().nextInt(50)
            val yy = Random().nextInt(50)
            image.x = xx.toFloat()
            image.y = yy.toFloat()
        }
    }
}
