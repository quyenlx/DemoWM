package com.ominext.demowm

import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.util.TypedValue
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.opentok.android.Connection
import com.opentok.android.OpentokError
import com.opentok.android.Session
import com.opentok.android.Stream
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import org.json.JSONObject

class MainActivity : AppCompatActivity(), Session.SignalListener, Session.SessionListener {
    override fun onStreamDropped(p0: Session?, p1: Stream?) {
        Log.e(TAG, "onStreamDropped")
    }

    override fun onStreamReceived(p0: Session?, p1: Stream?) {
        Log.e(TAG, "onStreamReceived")
    }

    override fun onConnected(p0: Session?) {
        Log.e(TAG, "onConnected")
    }

    override fun onDisconnected(p0: Session?) {
        Log.e(TAG, "onDisconnected")
    }

    override fun onError(p0: Session?, p1: OpentokError?) {
        Log.e(TAG, "onError")
    }

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var session: Session
    private var mouseView: MouseView? = null

    private val widthScreen: Int by lazy {
        return@lazy resources.displayMetrics.widthPixels
    }

    private val heightScreen: Int by lazy {
        return@lazy resources.displayMetrics.heightPixels
    }

    private var widthImage = 0
    private var heightImage = 0
    private var deltaX = 0
    private var deltaY = 0
    private var ratio = 0F

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadImage()

        session = Session.Builder(this, OpenTokConfig.API_KEY, OpenTokConfig.SESSION_ID)
                .build()
                .apply {
                    connect(OpenTokConfig.TOKEN)
                    setSessionListener(this@MainActivity)
                    setSignalListener(this@MainActivity)
                }

        mouseView = MouseView(this, R.color.colorAccent)
        rootView.addView(mouseView)

        btnTouch.setOnClickListener {
            mouseView?.setMouseClicked()
        }

        btnMove.setOnClickListener {
            moveMouse(mouseView, 0, 0)
        }
    }

    private fun loadImage() {
        Glide.with(this)
                .load(OpenTokConfig.IMAGE_URL)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        resource?.let {
                            widthImage = it.intrinsicWidth
                            heightImage = it.intrinsicHeight
                            ratio = widthScreen.toFloat() / heightImage

                            deltaX = (widthScreen - widthImage) / 2
                            deltaY = (heightScreen - heightImage) / 2

                            Log.e(TAG, "widthImage = $widthImage")
                            Log.e(TAG, "heightImage = $heightImage")
                            Log.e(TAG, "deltaX = $deltaX")
                            Log.e(TAG, "deltaY = $deltaY")
                        }
                        return false
                    }
                })
                .into(photoView)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            photoView.drawable.let {
                widthImage = it.intrinsicWidth
                heightImage = it.intrinsicHeight
            }
        } else {
            heightImage = widthScreen - 84
            widthImage = (heightImage * ratio).toInt()
        }
        deltaX = (heightScreen - widthImage) / 2
        deltaY = (widthScreen - heightImage) / 2
    }

    override fun onDestroy() {
        super.onDestroy()
        session.disconnect()
    }

    override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
        try {
            val data = JSONObject(p2)
            val mouse = data.getJSONObject("mouse")
            val x = mouse.getDouble("x")
            val y = mouse.getDouble("y")

            val click = data.getBoolean("click")
            Log.e(TAG, "x = $x & y = $y & click = $click")

            if (x < 0 || y < 0) return

            if (p1.equals("zoom")) {

            }
            when (p1) {
                "zoom" -> {
                    val zoom = data.getInt("zoom")
                    photoView.setScale((zoom.toFloat() / 100), (widthScreen / 2).toFloat(), (heightScreen / 2).toFloat(), true)
                }
                "cursor" -> {
                    moveMouse(mouseView, (x * widthImage / 1000).toInt(), (y * heightImage / 1000).toInt())
                }
                "clickMouse" -> {
                    mouseView?.setMouseClicked()
                }
            }
        } catch (ex: JSONException) {
            Log.e(TAG, ex.message)
        }
    }

    private fun moveMouse(mouse: MouseView?, x: Int, y: Int) {
        rootView.post {
            val params = mouse?.layoutParams as RelativeLayout.LayoutParams
            params.leftMargin = x + deltaX - 7.5F.dp2Px()
            params.topMargin = y + deltaY - 10.5F.dp2Px() * 2
            mouse.layoutParams = params
        }
    }

    private fun Float.dp2Px(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, resources.displayMetrics).toInt()
    }

}
