package com.ominext.demowm

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.SeekBar
import com.ominext.demowm.widget.CircleProgressBar
import com.ominext.demowm.widget.colorpicker.ColorPickerDialog

class CircleProgressBarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_circle_progress_bar)

        val seekBarProgress: SeekBar = findViewById(R.id.seekBar_progress)
        val seekBarThickness: SeekBar = findViewById(R.id.seekBar_thickness)
        val button: Button = findViewById(R.id.button)
        val circleProgressBar: CircleProgressBar = findViewById(R.id.custom_progressBar)
        //Using ColorPickerLibrary to pick color for our CustomProgressbar
        val colorPickerDialog = ColorPickerDialog()
        colorPickerDialog.initialize(
                R.string.select_color,
                intArrayOf(Color.CYAN, Color.DKGRAY, Color.BLACK, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.RED, Color.GRAY, Color.YELLOW),
                Color.DKGRAY, 3, 2)

        colorPickerDialog.setOnColorSelectedListener { color -> circleProgressBar.setColor(color) }
        button.setOnClickListener { colorPickerDialog.show(supportFragmentManager, "color_picker") }
        seekBarProgress.progress = circleProgressBar.getProgress().toInt()
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b)
                    circleProgressBar.setProgressWithAnimation(i.toFloat())
                else
                    circleProgressBar.setProgress(i.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
        seekBarThickness.progress = circleProgressBar.getStrokeWidth().toInt()
        seekBarThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                circleProgressBar.setStrokeWidth(i.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })
    }


}
