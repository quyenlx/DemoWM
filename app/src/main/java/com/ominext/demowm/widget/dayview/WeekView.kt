@file:Suppress("MemberVisibilityCanPrivate", "LoopToCallChain", "unused")

package com.ominext.demowm.widget.dayview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.ominext.demowm.R
import com.ominext.demowm.util.TimeUtils
import com.ominext.demowm.util.isTheSameDay
import java.util.*

class WeekView : View {

    private enum class Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    companion object {
        const val DAY_PAST_ALPHA = 100
        const val PAST_ALPHA = 160
        const val NORMAL_ALPHA = 255
        const val STROKE_HIGHLIGHT_WIDTH = 4F
        const val DEFAULT_STROKE_WIDTH = 2F
        val STROKE_HIGHLIGHT_COLOR = Color.parseColor("#c7666666")
        const val DISTANCE_FROM_TOP = 45
        const val BLOCK = 15
        const val NUMBER_USER_DISPLAY = 10
    }

    private var BUFFER_DAY = 84 // total = BUFFER_DAY// * 2 + mNumberOfVisibleHours
    private var mContext: Context
    private val mTimeTextPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var mTimeTextHeight: Float = 0F
    private val mHeaderTextPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var mHeaderTextHeight: Float = 0F
    private var mHeaderRowHeight: Float = 0F
    private lateinit var mGestureDetector: GestureDetectorCompat
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private val mHeaderBackgroundPaint: Paint by lazy { Paint() }
    private var mWidthPerHour: Float = 0.toFloat()
    private val mDayBackgroundPaint: Paint by lazy { Paint() }
    private val mHourSeparatorPaint: Paint by lazy { Paint() }
    private var mHeaderMarginBottom: Float = 0F
    private val mTodayBackgroundPaint: Paint by lazy { Paint() }
    private val mEventBackgroundPaint: Paint by lazy { Paint() }

    private var mHeaderColumnWidth: Float = 60F
        set(value) {
            field = value
            invalidate()
        }
    private val mEventTextPaint: TextPaint by lazy { TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG) }
    private val mHeaderColumnBackgroundPaint: Paint by lazy { Paint() }
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.
    private var mRefreshEvents = false
    private var mCurrentFlingDirection = Direction.NONE

    private val mAllDayText = "AllDay"
    private val mAllDayTextPaint: TextPaint by lazy { TextPaint(Paint.ANTI_ALIAS_FLAG) }
    private var mAllDayTextWidth: Float = 0F
    private var mAllDayTextHeight: Float = 0F

    /**
     * The first visible day in the week view.
     */
    var mFirstVisibleDay: Calendar? = null
        private set

    var mShowFirstDayOfWeekFirst = false

    private var mMinimumFlingVelocity = 0
    private var mScaledTouchSlop = 0

    // Attributes and their default values.
    var mHourHeight = 50
        set(value) {
            field = value
            invalidate()
        }

    private var mMinHourHeight = 0 //no minimum specified (will be dynamic, based on screen)

    var mFirstDayOfWeek = Calendar.MONDAY
        set(value) {
            field = value
            invalidate()
        }

    var mTextSize = 16
        set(value) {
            field = value
            mHeaderTextPaint.textSize = mTextSize.toFloat()
            mTimeTextPaint.textSize = mTextSize.toFloat()
            invalidate()
        }

    private var mTextSizeTime = 12
    var mHeaderColumnPadding = 10
        set(value) {
            field = value
            invalidate()
        }


    var mHeaderColumnTextColor = Color.BLACK
        set(value) {
            field = value
            mHeaderTextPaint.color = mHeaderColumnTextColor
            mTimeTextPaint.color = mHeaderColumnTextColor
            invalidate()
        }

    private var mHeaderSundayColumnTextColor = Color.BLACK
    private var mHeaderSaturdayColumnTextColor = Color.BLACK
    private var mHeaderColumnTextColorTime = Color.BLACK

    /**
     * The number of visible days in a week.
     */
    private var mNumberOfVisibleHours = 5
        /**
         * Set the number of visible days in a week.
         *
         * @param value The number of visible days in a week.
         */
        set(value) {
            field = value
            mCurrentOrigin.x = 0f
            mCurrentOrigin.y = 0f
            invalidate()
        }

    var mHeaderRowPadding = 10
        set(value) {
            field = value
            invalidate()
        }

    var mHeaderRowBackgroundColor = Color.BLUE
        set(value) {
            field = value
            mHeaderBackgroundPaint.color = field
            invalidate()
        }

    var mDayBackgroundColor = Color.rgb(245, 245, 245)
        set(value) {
            field = value
            mDayBackgroundPaint.color = field
            invalidate()
        }

    var mHourSeparatorColor = Color.rgb(230, 230, 230)
        set(value) {
            field = value
            mHourSeparatorPaint.color = field
            invalidate()
        }

    var mTodayBackgroundColor = Color.rgb(239, 247, 254)
        set(value) {
            field = value
            mTodayBackgroundPaint.color = field
            invalidate()
        }

    var mHourSeparatorHeight = 2
        set(value) {
            field = value
            mHourSeparatorPaint.strokeWidth = field.toFloat()
            invalidate()
        }

    var mEventTextSize = 9
        set(value) {
            field = value
            mEventTextPaint.textSize = field.toFloat()
            invalidate()
        }

    var mEventTextColor = Color.BLACK
        set(value) {
            field = value
            mEventTextPaint.color = field
            invalidate()
        }

    var mHeaderColumnBackgroundColor = Color.WHITE
        set(value) {
            field = value
            mHeaderColumnBackgroundPaint.color = field
            invalidate()
        }

    private var mAreDimensionsInvalid = true
    var mXScrollingSpeed = 0.6f

    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0
    var mEventCornerRadius = 0
    /**
     * The flag to determine whether the week view should fling horizontally or not.
     */
    var mHorizontalFlingEnabled = true

    /**
     * The flag to determine whether the week view should fling vertically or not.
     */
    var mVerticalFlingEnabled = true

    var mAllDayEventHeight: Int = 0

    /**
     * The height of an event in all day event section
     */
    private var mAllDayEventItemHeight = DimensionUtils.dpToPx((mEventTextSize * 1.75).toFloat())

    /**
     * The maximum of number event which users can see in the all day section
     */
    private val mMaxVisibleAllDayEventNum = 3

    var mScrollDuration = 250

    private val mVisibleHeaderBackgroundPaint: Paint by lazy { Paint() }
    private val mBackgroundColor = Color.parseColor("#fffefc")

    /**
     * The actual number of events in all day event section
     */
    private var mMaxInColumn: Int = 0
    private var mMaxInColumnToShowToggleButton: Int = 0 // for displaying the toggle button

    private val mAllDayEventSeparatorPaint: Paint by lazy { Paint() }
    private val mEventSeparatorWidth = DimensionUtils.dpToPx(1F)
    private val mHolidays = HashMap<Calendar, Boolean>()
    private var mHasAllDayEvents: Boolean = false

    // Listeners.
    var mEventTouchListener: EventTouchListener? = null

    private val mDateTimeInterpreter: DateTimeInterpreter by lazy {
        object : DateTimeInterpreter {

            override fun interpretDate(date: Calendar): String {
                return try {
                    TimeUtils.getDateFormat(context.getString(R.string.TIME_FORMAT_WEEK), Locale.JAPAN).format(date.time)
                } catch (e: Exception) {
                    e.printStackTrace()
                    ""
                }
            }

            override fun interpretTime(hour: Int): String {
                return if (hour < 10) {
                    "0$hour:00"
                } else {
                    "$hour:00"
                }
            }
        }
    }
    var mScrollStateChangeListener: ScrollStateChangeListener? = null

    // Event touch screen
    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            mEventTouchListener?.onDown()
            goToNearestOrigin()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            when (mCurrentScrollDirection) {
                Direction.NONE -> {
                    // Allow scrolling only in one direction.
                    mCurrentScrollDirection = if (Math.abs(distanceX) > Math.abs(distanceY)) {
                        if (distanceX > 0) {
                            Direction.LEFT
                        } else {
                            Direction.RIGHT
                        }
                    } else {
                        Direction.VERTICAL
                    }
                }
                Direction.LEFT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX < -mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.RIGHT
                    }
                }
                Direction.RIGHT -> {
                    // Change direction if there was enough change.
                    if (Math.abs(distanceX) > Math.abs(distanceY) && distanceX > mScaledTouchSlop) {
                        mCurrentScrollDirection = Direction.LEFT
                    }
                }
                else -> {
                    //do nothing
                }
            }

            // Calculate the new origin after scroll.
            when (mCurrentScrollDirection) {
                Direction.LEFT, Direction.RIGHT -> {
                    mCurrentOrigin.x -= distanceX * mXScrollingSpeed
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                Direction.VERTICAL -> {
                    if (e1.y > mHeaderRowHeight) {
                        mCurrentOrigin.y -= distanceY
                        ViewCompat.postInvalidateOnAnimation(this@WeekView)
                    }
                }
                else -> {
                    //do nothing
                }
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (mCurrentFlingDirection == Direction.LEFT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.RIGHT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.VERTICAL && !mVerticalFlingEnabled) {
                return true
            }

            mScroller?.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                Direction.LEFT, Direction.RIGHT -> {
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * mXScrollingSpeed).toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (-((mHourHeight * (NUMBER_USER_DISPLAY)).toFloat() + mHeaderRowHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                Direction.VERTICAL -> if (e1.y > mHeaderRowHeight) {
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), Integer.MIN_VALUE, Integer.MAX_VALUE, (-((mHourHeight * (NUMBER_USER_DISPLAY)).toFloat() + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                else -> {
                    //do nothing
                }
            }
            return true
        }

    }

    constructor (context: Context) : this(context, null)

    constructor (context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {

        // Hold references.
        mContext = context

        // Get the attribute values (if any).
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WeekView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.WeekView_firstDayOfWeek, mFirstDayOfWeek)
            mHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourHeight, mHourHeight)
            mMinHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_minHourHeight, mMinHourHeight)
            mTextSize = a.getDimensionPixelSize(R.styleable.WeekView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mTextSizeTime = a.getDimensionPixelSize(R.styleable.WeekView_textSizeTime, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSizeTime.toFloat(), context.resources.displayMetrics).toInt())
            mHeaderColumnWidth = a.getDimensionPixelSize(R.styleable.WeekView_headerColumnWidth, mHeaderColumnWidth.toInt()).toFloat()
            mHeaderColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor)
            mHeaderColumnTextColorTime = a.getColor(R.styleable.WeekView_headerColumnTextColorTime, mHeaderColumnTextColorTime)
            mHeaderSundayColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColorSunday, mHeaderSundayColumnTextColor)
            mHeaderSaturdayColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColorSaturday, mHeaderSaturdayColumnTextColor)
            mNumberOfVisibleHours = a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleHours)
            mShowFirstDayOfWeekFirst = a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, mShowFirstDayOfWeekFirst)
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, mHeaderRowPadding)
            mHeaderRowHeight = a.getDimensionPixelSize(R.styleable.WeekView_headerRowHeight, mHeaderRowHeight.toInt()).toFloat()
            mHeaderRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor)
            mDayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
            mHourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor)
            mTodayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor)
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight)
            mEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor)
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor)
            mXScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, mXScrollingSpeed)
            mEventCornerRadius = a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, mEventCornerRadius)
            mHorizontalFlingEnabled = a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, mHorizontalFlingEnabled)
            mVerticalFlingEnabled = a.getBoolean(R.styleable.WeekView_verticalFlingEnabled, mVerticalFlingEnabled)
            mScrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, mScrollDuration)
        } finally {
            a.recycle()
        }
        init()
    }

    private fun init() {
        mGestureDetector = GestureDetectorCompat(mContext, mGestureListener)
        mScroller = OverScroller(mContext, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(mContext).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop

        // Measure settings for time column.
        mTimeTextPaint.textAlign = Paint.Align.CENTER
        mTimeTextPaint.textSize = mTextSizeTime.toFloat()
        mTimeTextPaint.color = mHeaderColumnTextColorTime
        val rect = Rect()
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = -mTimeTextHeight / 2

        // Measure settings for header row.
        mHeaderTextPaint.color = mHeaderColumnTextColor
        mHeaderTextPaint.textAlign = Paint.Align.CENTER
        mHeaderTextPaint.textSize = mTextSize.toFloat()
        mHeaderTextPaint.typeface = Typeface.create(mHeaderTextPaint.typeface, Typeface.BOLD)   // #8651
        mHeaderTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mHeaderTextHeight = rect.height().toFloat()
        //mHeaderTextPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Measure setting for text all day
        mAllDayTextPaint.color = mHeaderColumnTextColorTime
        mAllDayTextPaint.textSize = (mEventTextSize + 2).toFloat()
        mAllDayTextPaint.getTextBounds(mAllDayText, 0, mAllDayText.length, rect)
        mAllDayTextWidth = rect.width().toFloat()
        mAllDayTextHeight = rect.height().toFloat()

        // Prepare header background paint.
        mHeaderBackgroundPaint.color = mHeaderRowBackgroundColor
        mHeaderBackgroundPaint.style = Paint.Style.STROKE
        mHeaderBackgroundPaint.strokeWidth = DEFAULT_STROKE_WIDTH

        // Prepare visible header background pain
        mVisibleHeaderBackgroundPaint.color = mBackgroundColor
        mVisibleHeaderBackgroundPaint.style = Paint.Style.FILL

        mAllDayEventSeparatorPaint.color = mHeaderRowBackgroundColor
        mAllDayEventSeparatorPaint.style = Paint.Style.STROKE
        mAllDayEventSeparatorPaint.strokeWidth = DEFAULT_STROKE_WIDTH

        // Prepare day background color paint.
        mDayBackgroundPaint.color = mHeaderRowBackgroundColor
        mDayBackgroundPaint.style = Paint.Style.STROKE
        mDayBackgroundPaint.pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 0f)
        mDayBackgroundPaint.strokeWidth = DEFAULT_STROKE_WIDTH

        // Prepare hour separator color paint.
        mHourSeparatorPaint.color = mHourSeparatorColor
        mHourSeparatorPaint.style = Paint.Style.STROKE
        mHourSeparatorPaint.pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 0f)
        mHourSeparatorPaint.strokeWidth = DEFAULT_STROKE_WIDTH

        // Prepare today background color paint.
        mTodayBackgroundPaint.color = mTodayBackgroundColor

        // Prepare event background color.
        mEventBackgroundPaint.color = Color.rgb(174, 208, 238)

        // Prepare header column background color.
        mHeaderColumnBackgroundPaint.color = mHeaderColumnBackgroundColor

        // Prepare event text size and color.
        mEventTextPaint.style = Paint.Style.FILL
        mEventTextPaint.color = mEventTextColor
        mEventTextPaint.textSize = mEventTextSize.toFloat()
    }

    // fix rotation changes
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawHeaderRowAndEvents(canvas)
    }

    private fun drawHeaderRowAndEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mWidthPerHour = width.toFloat() - mHeaderColumnWidth
        mWidthPerHour /= mNumberOfVisibleHours

        val today = TimeUtils.today()
        mFirstVisibleDay = today.clone() as Calendar

        // If the new mCurrentOrigin.y is invalid, make it valid.
        if (mCurrentOrigin.y < height.toFloat() - (mHourHeight * (NUMBER_USER_DISPLAY)).toFloat() - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2) {
            mCurrentOrigin.y = height.toFloat() - (mHourHeight * (NUMBER_USER_DISPLAY)).toFloat() - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2
        }

        if (mCurrentOrigin.y > 0) {
            mCurrentOrigin.y = 0f
        }

        // Consider scroll offset.
        val leftDaysWithGaps = (-Math.ceil(((if (mCurrentOrigin.x == 0f) mCurrentOrigin.x else mCurrentOrigin.x - 5) / mWidthPerHour).toDouble())).toInt()

        //region Draw Date Display
        if (leftDaysWithGaps < 0) {
            mFirstVisibleDay!!.add(Calendar.DATE, leftDaysWithGaps / 25 - 1)
        } else {
            mFirstVisibleDay!!.add(Calendar.DATE, leftDaysWithGaps / 24)
        }

        when (mFirstVisibleDay?.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> mHeaderTextPaint.color = mHeaderSaturdayColumnTextColor
            Calendar.SUNDAY -> mHeaderTextPaint.color = mHeaderSundayColumnTextColor
            else -> mHeaderTextPaint.color = mHeaderColumnTextColor
        }

        if (mFirstVisibleDay!!.before(today)) {
            mHeaderTextPaint.alpha = DAY_PAST_ALPHA
        } else {
            mHeaderTextPaint.alpha = NORMAL_ALPHA
        }

        if (mFirstVisibleDay!!.isTheSameDay(today)) {
            canvas.drawRect(0F, 0F, mHeaderColumnWidth, height.toFloat(), mTodayBackgroundPaint)
        }

        val text = mDateTimeInterpreter.interpretDate(mFirstVisibleDay!!)
        canvas.drawText(text, mHeaderColumnWidth / 2, mHeaderRowHeight / 2 + mHeaderTextHeight / 2, mHeaderTextPaint)
        //endregion

        val startFromPixel = mCurrentOrigin.x + mWidthPerHour * leftDaysWithGaps + mHeaderColumnWidth - DEFAULT_STROKE_WIDTH
        var startPixel = startFromPixel
        var day: Calendar

        //region Draw events + (rows , columns)
        val dashPath = Path()
        for (hourNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleHours + 1) {
            day = today.clone() as Calendar
            day.add(Calendar.HOUR_OF_DAY, hourNumber - 1)
            val hour = day.get(Calendar.HOUR_OF_DAY)

            // Draw background color for each day.
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerHour + startPixel - start > 0) {
                if (hour == 0 && hourNumber != leftDaysWithGaps + 1) {
                    mDayBackgroundPaint.color = STROKE_HIGHLIGHT_COLOR
                    mDayBackgroundPaint.strokeWidth = STROKE_HIGHLIGHT_WIDTH
                    canvas.drawLine(startPixel - DEFAULT_STROKE_WIDTH, mHeaderMarginBottom, startPixel - DEFAULT_STROKE_WIDTH, height.toFloat(), mDayBackgroundPaint)
                    mDayBackgroundPaint.color = mHeaderRowBackgroundColor
                    mDayBackgroundPaint.strokeWidth = DEFAULT_STROKE_WIDTH
                } else {
                    //Draw the line separating days in normal event section: if the day is the first day of week -> draw thick separator else draw dash separator
                    dashPath.moveTo(start, mHeaderRowHeight)
                    dashPath.lineTo(start, height.toFloat())
                    canvas.drawPath(dashPath, mDayBackgroundPaint)
                    dashPath.reset()
                }
            }

            // Draw the lines for hours.
            val path = Path()
            for (lineHour in 0 until NUMBER_USER_DISPLAY) {
                val top = mHeaderRowHeight + mCurrentOrigin.y + (mHourHeight * (lineHour + 1)).toFloat()
                if (top > mHeaderRowHeight && top < height && startPixel + mWidthPerHour - start > 0) {
                    path.moveTo(start, top)
                    path.lineTo(start + mWidthPerHour, top)
                    canvas.drawPath(path, mHourSeparatorPaint)
                    path.reset()
                }
            }
            startPixel += mWidthPerHour
        }
        //endregion

        // Draw the partial of line separating the header (not include all day event) and all-day events
        run {
            val y = mHeaderRowHeight
            canvas.drawLine(0f, y, mHeaderColumnWidth, y, mAllDayEventSeparatorPaint)
        }

        run {
            val x = mHeaderColumnWidth
            canvas.drawLine(x, 0F, x, height.toFloat(), mHeaderBackgroundPaint)
        }
        run {
            val y = height - DEFAULT_STROKE_WIDTH / 2
            canvas.drawLine(0F, y, mHeaderColumnWidth, y, mHeaderBackgroundPaint)
        }

        // Clip to paint header row only.
        canvas.clipRect(mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, 0f, width.toFloat(), mHeaderRowHeight, Region.Op.REPLACE)

        // Draw the header background.
        canvas.drawRect(0f, 0f, width.toFloat(), mHeaderRowHeight, mVisibleHeaderBackgroundPaint)

        //Draw the line separating the header (include all day event section) and normal event section
        run {
            val y: Float = mHeaderRowHeight
            canvas.drawLine(mHeaderColumnWidth - DEFAULT_STROKE_WIDTH, y, width.toFloat(), y, mHeaderBackgroundPaint)
        }

        //region Draw the header row texts START
        startPixel = startFromPixel
        for (hourNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleHours + 1) {
            // Check if the day is today.
            day = today.clone() as Calendar
            day.add(Calendar.HOUR_OF_DAY, hourNumber - 1)
            val hour = day.get(Calendar.HOUR_OF_DAY)
            // Draw the day labels.
            val hourLabel = mDateTimeInterpreter.interpretTime(hour)
            val x = startPixel
            canvas.drawText(hourLabel, x, mHeaderRowHeight / 2 + mHeaderTextHeight / 2, mTimeTextPaint)
            startPixel += mWidthPerHour
        }
        //endregion Draw the header row texts START
    }

    /////////////////////////////////////////////////////////////////
    //
    //      Functions related to scrolling.
    //
    /////////////////////////////////////////////////////////////////

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val result = mGestureDetector.onTouchEvent(event)

        // Check after call of mGestureDetector, so mCurrentFlingDirection and mCurrentScrollDirection are set.
        if (event.action == MotionEvent.ACTION_UP && mCurrentFlingDirection == Direction.NONE) {
            if (mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.LEFT) {
                goToNearestOrigin()
            } else {
                if (mScrollStateChangeListener != null) {
                    mScrollStateChangeListener?.onScrollSateChanged(true)
                }
            }
            mCurrentScrollDirection = Direction.NONE
        }

        return result
    }

    private fun goToNearestOrigin() {
        var leftDays = (mCurrentOrigin.x / mWidthPerHour).toDouble()

        leftDays = when {
            mCurrentFlingDirection != Direction.NONE -> // snap to nearest day
                Math.round(leftDays).toDouble()
            mCurrentScrollDirection == Direction.LEFT -> // snap to last day
                Math.floor(leftDays)
            mCurrentScrollDirection == Direction.RIGHT -> // snap to next day
                Math.ceil(leftDays)
            else -> // snap to nearest day
                Math.round(leftDays).toDouble()
        }

        if (mCurrentScrollDirection == Direction.NONE) {
            mScrollStateChangeListener?.onScrollSateChanged(false)
        } else if (mCurrentScrollDirection == Direction.LEFT || mCurrentScrollDirection == Direction.RIGHT || mCurrentScrollDirection == Direction.VERTICAL) {
            mScrollStateChangeListener?.onScrollSateChanged(true)
        }

        val nearestOrigin = (mCurrentOrigin.x - leftDays * mWidthPerHour).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller?.forceFinished(true)
            // Snap to date.
            mScroller?.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerHour * mScrollDuration).toInt())
            ViewCompat.postInvalidateOnAnimation(this@WeekView)
        }
        // Reset scrolling and fling direction.
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection
    }

    override fun computeScroll() {
        super.computeScroll()

        if (mScroller == null) {
            return
        }

        if (mScroller!!.isFinished) {
            if (mCurrentFlingDirection != Direction.NONE) {
                // Snap to day after fling is finished.
                goToNearestOrigin()
            }
        } else {
            if (mCurrentFlingDirection != Direction.NONE && forceFinishScroll()) {
                goToNearestOrigin()
            } else if (mScroller!!.computeScrollOffset()) {
                if (mCurrentFlingDirection == Direction.VERTICAL)
                    mCurrentOrigin.y = if (mScroller!!.currY.toFloat() == 0F) mScroller!!.currY.toFloat() else mScroller!!.currY.toFloat() - DISTANCE_FROM_TOP// #17434
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    /**
     * Check if scrolling should be stopped.
     *
     * @return true if scrolling should be stopped before reaching the end of animation.
     */
    private fun forceFinishScroll(): Boolean = false

    /////////////////////////////////////////////////////////////////
    //
    //      Interfaces.
    //
    /////////////////////////////////////////////////////////////////

    interface EventTouchListener {

        /**
         * Triggered when touched on WeekView
         */
        fun onDown()
    }

    interface EmptyViewClickListener {
        /**
         * Triggered when the users clicks on a empty space of the calendar.
         *
         * @param time: [Calendar] object set with the date and time of the clicked position on the view.
         */
        fun onEmptyViewClicked(time: Calendar)
    }

    interface EmptyViewLongPressListener {
        /**
         * Similar to [EmptyViewClickListener] but with long press.
         *
         * @param time: [Calendar] object set with the date and time of the long pressed position on the view.
         */
        fun onEmptyViewLongPress(time: Calendar)
    }

    interface ScrollListener {
        /**
         * Called when the first visible day has changed.
         *
         *
         * (this will also be called during the first draw of the weekView)
         *
         * @param newFirstVisibleDay The new first visible day
         * @param oldFirstVisibleDay The old first visible day (is null on the first call).
         */
        fun onFirstVisibleDayChanged(newFirstVisibleDay: Calendar, oldFirstVisibleDay: Calendar)
    }

    interface TitleChangeListener {
        /**
         * Called when the first visible day has changed.
         *
         * @param calendar The first visible day
         */
        fun onTitleChange(calendar: Calendar)
    }

    interface ScrollStateChangeListener {
        /**
         * Triggered when the scroll state of weekview is changed
         *
         * @param isIdle true if the state is idling and false if state is dragging or fling
         */
        fun onScrollSateChanged(isIdle: Boolean)
    }

    interface InitListener {
        /**
         * Called after the first draw of view
         */
        fun onViewCreated()
    }
}