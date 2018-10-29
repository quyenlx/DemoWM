package com.ominext.demowm.widget

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.ominext.demowm.R
import android.graphics.RectF


/**
 * Created by Admin on 10/18/2018.
 */
class ProgressbarMultipleColors(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        private const val BACKGROUND_COLOR = Color.GRAY
        private var STROKE_WIDTH = 20f
    }

    private var mBmpIcon: Bitmap? = null

    private val mPaintBackground = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintProgress = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintProgress2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var mCircleBounds = RectF()
    private var mCircleProgressBounds = RectF()
    private var mLayoutHeight = 0
    private var mLayoutWidth = 0
    private var mStrokeWidth = STROKE_WIDTH
    private var mBackgroundColor = Color.GRAY
    private var mProgressColor = Color.RED
    private var mProgressColor2 = Color.GREEN
    private var mProgressValue = 0
    private var mProgress2Value = 0
    private var mFontSize = 14f
    private var mText: String = ""

    init {
        val ta = context?.obtainStyledAttributes(attrs, R.styleable.RoundProgressBar, 0, 0)

        ta?.let {
            mStrokeWidth = it.getDimension(R.styleable.RoundProgressBar_pmc_strokeWidth, STROKE_WIDTH)
            mBackgroundColor = it.getColor(R.styleable.RoundProgressBar_pmc_bg_color, BACKGROUND_COLOR)
            mProgressColor = it.getColor(R.styleable.RoundProgressBar_pmc_progress_color, Color.MAGENTA)
            mProgressColor2 = it.getColor(R.styleable.RoundProgressBar_pmc_progress2_color, BACKGROUND_COLOR)

            val icon = it.getResourceId(R.styleable.RoundProgressBar_pmc_drawable, -1)
            mBmpIcon = BitmapFactory.decodeResource(resources, icon)
            mProgressValue = getProgressValue(it.getInteger(R.styleable.RoundProgressBar_pmc_progress, mProgressValue))
            mProgress2Value = getProgressValue(it.getInteger(R.styleable.RoundProgressBar_pmc_progress2, mProgress2Value))

            mFontSize = it.getDimension(R.styleable.RoundProgressBar_pmc_textSize, mFontSize)

            val textResource = it.getResourceId(R.styleable.RoundProgressBar_pmc_text, -1)
            if (textResource != -1) {
                mText = resources.getString(textResource)
            }
        }

        setupPaints()
        ta?.recycle()
    }

    private fun setupPaints() {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mFontSize, resources.displayMetrics)

        STROKE_WIDTH = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mFontSize, resources.displayMetrics)

        mPaintBackground.color = mBackgroundColor
        mPaintProgress.color = mProgressColor
        mPaintProgress2.color = mProgressColor2
        mPaintText.color = Color.GRAY
        mPaintText.textSize = px

        setupDefaultPain(mPaintBackground)
        setupDefaultPain(mPaintProgress)
        setupDefaultPain(mPaintProgress2)
    }

    private fun setupDefaultPain(paint: Paint) {
        paint.apply {
            style = Paint.Style.STROKE
            strokeWidth = mStrokeWidth
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mLayoutWidth = w
        mLayoutHeight = h
        setupBounds()
    }

    private fun setupBounds() {
        val minValue = Math.min(mLayoutWidth, mLayoutHeight)

        val xOffset = mLayoutWidth - minValue
        val yOffset = mLayoutHeight - minValue

        val paddingTop = this.paddingTop + yOffset / 2
        val paddingBottom = this.paddingBottom + yOffset / 2
        val paddingLeft = this.paddingLeft + xOffset / 2
        val paddingRight = this.paddingRight + xOffset / 2

        val width = width
        val height = height

        mCircleBounds = RectF(
                paddingLeft + STROKE_WIDTH,
                paddingTop + STROKE_WIDTH,
                width - paddingRight - STROKE_WIDTH,
                height - paddingBottom - STROKE_WIDTH)

        mCircleProgressBounds = RectF(
                paddingLeft + STROKE_WIDTH,
                paddingTop + STROKE_WIDTH,
                width - paddingRight - STROKE_WIDTH,
                height - paddingBottom - STROKE_WIDTH)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(mCircleBounds, 0f, 360f, false, mPaintBackground)
        canvas.drawArc(mCircleBounds, 270f, mProgress2Value.toFloat(), false, mPaintProgress2)
        canvas.drawArc(mCircleProgressBounds, 270f, mProgressValue.toFloat(), false, mPaintProgress2)

        mBmpIcon?.also {
            val wH = mCircleBounds.width() / 2
            canvas.drawBitmap(it,
                    mCircleBounds.left + wH - it.width / 2,
                    mCircleBounds.top + wH - it.height / 2,
                    mPaintBackground)
        }

        if (mText.isNotEmpty()) {
            this.drawMultilineText(canvas)
        }

    }

    private fun drawMultilineText(canvas: Canvas) {
        val textSplit = mText.split("\n")
        val wH = mCircleBounds.width() / 2

        for (i in textSplit.indices) {
            val txt = textSplit[i]
            val wT = mPaintText.measureText(txt)

            val pX = mCircleBounds.left + wH - wT / 2
            val pY = (mCircleBounds.top + mPaintText.fontSpacing + 20
                    + mPaintText.fontSpacing * i)

            canvas.drawText(txt, pX, pY, mPaintText)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = View.getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        val width = View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec)
        val min = Math.min(width, height)
        setMeasuredDimension((min + 2 * STROKE_WIDTH).toInt(), (min + 2 * STROKE_WIDTH).toInt())

        mCircleBounds.set(STROKE_WIDTH, STROKE_WIDTH, min + STROKE_WIDTH, min + STROKE_WIDTH)
        mCircleProgressBounds.set(STROKE_WIDTH, STROKE_WIDTH, min + STROKE_WIDTH, min + STROKE_WIDTH)
    }

    fun setProgressValue(mProgressValue: Int) {
        validateProgressValue(mProgressValue)
        this.mProgressValue = getProgressValue(mProgressValue)
        postInvalidate()
    }

    fun setProgressValue2(mProgressValue: Int) {
        validateProgressValue(mProgressValue)
        this.mProgress2Value = getProgressValue(mProgressValue)
        postInvalidate()
    }

    private fun validateProgressValue(mProgressValue: Int) {
        if (mProgressValue < 0 || mProgressValue > 100) {
            throw IllegalArgumentException("Value must be between 0 and 100")
        }
    }


    private fun getProgressValue(mProgressValue: Int): Int {
        return 360 * mProgressValue / 100
    }

    fun setText(text: String) {
        this.mText = text
        postInvalidate()
    }

}