@file:Suppress("MemberVisibilityCanPrivate", "LoopToCallChain", "unused")

package com.ominext.demowm.widget.dayview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.v4.util.ArraySet
import android.support.v4.view.GestureDetectorCompat
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.text.*
import android.text.format.DateUtils
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ominext.demowm.R
import com.ominext.demowm.util.TimeUtils
import com.ominext.demowm.util.ViewUtils
import com.ominext.demowm.util.isTheSameDay
import com.ominext.demowm.util.isTheSameHour
import java.util.*

class DayView : View {

    private enum class Direction {
        NONE, LEFT, RIGHT, VERTICAL
    }

    companion object {
        const val DAY_PAST_ALPHA = 100
        const val NORMAL_ALPHA = 255
        const val STROKE_HIGHLIGHT_WIDTH = 4F
        const val DEFAULT_STROKE_WIDTH = 2F
    }

    private var mContext: Context
    private lateinit var mGestureDetector: GestureDetectorCompat
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private var mCurrentFlingDirection = Direction.NONE
    private var mFirstVisibleDay: Calendar? = null
    private var mShowFirstDayOfWeekFirst = false
    private var mScaledTouchSlop = 0
    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true
    private var mXScrollingSpeed = 0.6f
    private var mScrollToDay: Calendar? = null
    private var mHorizontalFlingEnabled = true
    private var mVerticalFlingEnabled = true
    private val mMaxVisibleAllDayEventNum = 3
    private var mScrollDuration = 250
    private var mLimitedAllDayEvents = true

    // Listeners.
    var mInitListener: InitListener? = null
    var mEventTouchListener: EventTouchListener? = null
    var mWeekViewLoader: WeekViewLoader? = null

    var mTitleChangeListener: TitleChangeListener? = null
    var mScrollStateChangeListener: ScrollStateChangeListener? = null

    //region #Row Header
    private var mHeaderHeight: Float = 0F
    private var mWidthPerHour: Float = 0.toFloat()

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
    //endregion

    //region #Column Header
    private val mapAvatar = mutableMapOf<Int, Bitmap?>()

    private var countStaff = 0
        set(value) {
            field = value
            postInvalidate()
        }
    private var mHourHeight = 50F.dp2Px().toInt()
    private var mHeaderTextHeight: Float = 0F
    private var mHeaderRowPadding = 10F.dp2Px()
    private var mHeaderColumnWidth: Float = 120F.dp2Px()

    //time
    private val mTextSizeHour = 12F.dp2Px()
    private val mPaintHour: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {
                    this.textSize = mTextSizeHour
                    this.color = mTextColorWeek
                    this.textAlign = Paint.Align.CENTER
                }
    }

    //date display
    private var mTextColorSunday = Color.BLACK
    private var mTextColorSaturday = Color.BLACK
    private var mTextColorWeek = Color.BLACK
    private val mTextSizeDateDisplay = 16F.dp2Px()
    private val mPaintDateDisplay: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {
                    this.textSize = mTextSizeDateDisplay
                    this.color = mTextColorWeek
                    this.textAlign = Paint.Align.CENTER
                }
    }

    //text AllDay
    private val mAllDayText = "終日"
    private val mTextSizeAllDay = 14F.dp2Px()
    private val mPaintTextAllDay: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {
                    this.textSize = mTextSizeAllDay
                    this.color = mTextColorWeek
                    this.textAlign = Paint.Align.CENTER
                }
    }

    //avatar staff
    private val mSizeAvatarStaff = 48F.dp2Px()
    private val mMarginLeftAvatarStaff = 10F.dp2Px()
    private val mMarginTopAvatarStaff = 2F.dp2Px()
    private val mPaintAvatar: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG)
    }

    //icon delete
    private val mBitmapDelete = ViewUtils.getBitmapFromXml(context, R.drawable.ic_cancel, 22F.dp2Px().toInt(), 22F.dp2Px().toInt())
    private val mBitmapUserPlaceHolder = ViewUtils.getBitmapFromXml(context, R.drawable.thumb_user, mSizeAvatarStaff.toInt(), mSizeAvatarStaff.toInt())
    private val mMarginTopIconDelete = 5F.dp2Px()

    //name staff
    private val mTextSizeStaff = 14F.dp2Px()
    private val mMarginBottomTextStaff = 2F.dp2Px()
    private val mMarginLeftTextStaff = 10F.dp2Px()
    private val mColorTextStaff = Color.BLACK
    private val mPaintTextStaff: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG)
                .apply {
                    this.textSize = mTextSizeStaff
                    this.color = mColorTextStaff
                }
    }

    //background Today
    private var mColorBackgroundToday = Color.rgb(239, 247, 254)
    private val mTodayBackgroundPaint: Paint by lazy {
        return@lazy Paint().apply {
            color = mColorBackgroundToday
        }
    }

    private val listRectBtnCancel = mutableMapOf<Int, Rect>()

    private val mPaintCircle: Paint by lazy {
        return@lazy Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.strokeWidth = 1F.dp2Px()
            this.style = Paint.Style.STROKE
            this.color = Color.RED
        }
    }

    //endregion

    //region #Cell Body
    private var mFirstDayOfWeek = Calendar.MONDAY
        set(value) {
            field = value
            invalidate()
        }
    private var mNumberOfVisibleDays = 3
        set(value) {
            field = value
            mCurrentOrigin.x = 0f
            mCurrentOrigin.y = 0f
            invalidate()
        }
    //Event
    private var mEventCornerRadius = 0
    private var mEventPadding = 8
        set(value) {
            field = value
            invalidate()
        }

    private val mPaintEventBackground: Paint by lazy {
        return@lazy Paint().apply {
            this.color = Color.rgb(174, 208, 238)
        }
    }

    //TextEvent
    var mTextSizeEvent = 9F.dp2Px()
    var mTextColorEvent = Color.BLACK

    private val mEventTextPaint: TextPaint by lazy {
        return@lazy TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG)
                .apply {
                    textSize = mTextSizeEvent
                    style = Paint.Style.FILL
                    color = mTextColorEvent
                }
    }


    //Line
    private var mColorLine = Color.BLUE
    private var mColorLineLight = Color.RED
    private val mPaintLine: Paint by lazy {
        return@lazy Paint().apply {
            this.color = mColorLine
            this.style = Paint.Style.STROKE
            this.pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 0f)
            this.strokeWidth = DEFAULT_STROKE_WIDTH
        }
    }

    private var mMapEventRects: MutableList<MutableList<EventRect>> = mutableListOf()
    private var mCurrentPeriodEvents: List<List<WeekViewEvent>>? = null
    private var mFetchedPeriod = -1
    private var mRefreshEvents = false

    private val mEventSpace = DimensionUtils.dpToPx(1F)
    private var mHasAllDayEvents: Boolean = false

    private var BUFFER_HOUR = 24
    private var mPositionFilled: Array<MutableSet<Int>> = Array(BUFFER_HOUR + mNumberOfVisibleDays * 2, { ArraySet<Int>() })
    private var mAllDayEventNumArray: IntArray = IntArray(BUFFER_HOUR + mNumberOfVisibleDays * 2)
    private var mOriginalAllDayEvent: BooleanArray = BooleanArray(BUFFER_HOUR + mNumberOfVisibleDays * 2)

    //endregion

    //region #ScaleDetector + GestureDetector
    private var mIsZooming: Boolean = false
    private var mNewHourWidth = 0F
    private var mMinHourWidth = 0F
    private var mMaxHourWidth = 0F

    private val mGestureListener = object : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            mEventTouchListener?.onDown()
            goToNearestOrigin()
            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (mIsZooming)
                return true
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
                    ViewCompat.postInvalidateOnAnimation(this@DayView)
                }
                Direction.VERTICAL -> {
                    if (e1.y > mHeaderHeight) {
                        mCurrentOrigin.y -= distanceY
                        ViewCompat.postInvalidateOnAnimation(this@DayView)
                    }
                }
                else -> {
                    //do nothing
                }
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (mIsZooming)
                return true
            if (mCurrentFlingDirection == Direction.LEFT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.RIGHT && !mHorizontalFlingEnabled ||
                    mCurrentFlingDirection == Direction.VERTICAL && !mVerticalFlingEnabled) {
                return true
            }

            mScroller?.forceFinished(true)

            mCurrentFlingDirection = mCurrentScrollDirection
            when (mCurrentFlingDirection) {
                Direction.LEFT, Direction.RIGHT -> {
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * mXScrollingSpeed).toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, -(getMaxOverScrollVertical() + mHeaderHeight - height).toInt(), 0)
                    ViewCompat.postInvalidateOnAnimation(this@DayView)
                }
                Direction.VERTICAL -> if (e1.y > mHeaderHeight) {
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), Integer.MIN_VALUE, Integer.MAX_VALUE, -(getMaxOverScrollVertical() + mHeaderHeight - height).toInt(), 0)
                    ViewCompat.postInvalidateOnAnimation(this@DayView)
                }
                else -> {
                    //do nothing
                }
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            for (i in listRectBtnCancel) {
                val rect = i.value
                if (e.x > rect.left && e.x < rect.right && e.y > rect.top && e.y < rect.bottom) {
                    Toast.makeText(context, "Clicked! ${i.key}", Toast.LENGTH_SHORT).show()
                    return super.onSingleTapConfirmed(e)
                }
            }

            run {
                if (e.x < mHeaderColumnWidth) return false
                val mapEventRects = mMapEventRects
                mapEventRects.forEach { eventRects ->
                    for (eventRect in eventRects) {
                        if (mHasAllDayEvents && e.x <= mHeaderColumnWidth + mWidthPerHour && !eventRect.event.mAllDay) continue
                        val rectF = eventRect.rectF
                        if (rectF != null && e.x > rectF.left && e.x < rectF.right && e.y > rectF.top && e.y < rectF.bottom) {
                            playSoundEffect(SoundEffectConstants.CLICK)
                            if (eventRect.event.mAllDay && eventRect.event.mStartTime!!.isTheSameDay(mFirstVisibleDay!!)) {
                                Toast.makeText(context, eventRect.event.mName, Toast.LENGTH_SHORT).show()
                                return super.onSingleTapConfirmed(e)
                            }
                            if (!eventRect.event.mAllDay) {
                                Toast.makeText(context, eventRect.event.mName, Toast.LENGTH_SHORT).show()
                                return super.onSingleTapConfirmed(e)
                            }
                        }
                    }
                }
            }

            return super.onSingleTapConfirmed(e)
        }
    }

    private var mScaleDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mIsZooming = true
            goToNearestOrigin()
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mNewHourWidth = Math.round(mWidthPerHour * detector.scaleFactor).toFloat()
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            mIsZooming = false
        }

    })
    //endregion

    constructor (context: Context) : this(context, null)

    constructor (context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor (context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mContext = context
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.DayView, 0, 0)
        try {
            mFirstDayOfWeek = a.getInteger(R.styleable.DayView_firstDayOfWeek, mFirstDayOfWeek)
            mHourHeight = a.getDimensionPixelSize(R.styleable.DayView_hourHeight, mHourHeight)
            mTextColorSunday = a.getColor(R.styleable.DayView_headerColumnTextColorSunday, mTextColorSunday)
            mTextColorSaturday = a.getColor(R.styleable.DayView_headerColumnTextColorSaturday, mTextColorSaturday)
            mTextColorWeek = a.getColor(R.styleable.DayView_headerColumnTextColorWeek, mTextColorSaturday)
            mNumberOfVisibleDays = a.getInteger(R.styleable.DayView_noOfVisibleDays, mNumberOfVisibleDays)
            mShowFirstDayOfWeekFirst = a.getBoolean(R.styleable.DayView_showFirstDayOfWeekFirst, mShowFirstDayOfWeekFirst)
            mColorBackgroundToday = a.getColor(R.styleable.DayView_todayBackgroundColor, mColorBackgroundToday)
            mTextColorEvent = a.getColor(R.styleable.DayView_eventTextColor, mTextColorEvent)
            mEventPadding = a.getDimensionPixelSize(R.styleable.DayView_eventPadding, mEventPadding)
            mXScrollingSpeed = a.getFloat(R.styleable.DayView_xScrollingSpeed, mXScrollingSpeed)
            mEventCornerRadius = a.getDimensionPixelSize(R.styleable.DayView_eventCornerRadius, mEventCornerRadius)
            mHorizontalFlingEnabled = a.getBoolean(R.styleable.DayView_horizontalFlingEnabled, mHorizontalFlingEnabled)
            mVerticalFlingEnabled = a.getBoolean(R.styleable.DayView_verticalFlingEnabled, mVerticalFlingEnabled)
            mScrollDuration = a.getInt(R.styleable.DayView_scrollDuration, mScrollDuration)
            mColorLine = a.getInt(R.styleable.DayView_lineColor, mColorLine)
            mColorLineLight = a.getInt(R.styleable.DayView_highlightLineColor, mColorLineLight)
        } finally {
            a.recycle()
        }

        init()
    }

    private fun init() {
        mGestureDetector = GestureDetectorCompat(mContext, mGestureListener)
        mScroller = OverScroller(mContext, FastOutLinearInInterpolator())

        mScaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop

        val rect = Rect()
        mPaintDateDisplay.typeface = Typeface.create(mPaintDateDisplay.typeface, Typeface.BOLD)
        mPaintDateDisplay.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mHeaderTextHeight = rect.height().toFloat()
    }

    private fun checkHasAllDay(mFirstVisibleDay: Calendar?) {
        mHasAllDayEvents = mMapEventRects
                .firstOrNull { eventsR ->
                    val eventRect = eventsR.firstOrNull { eventRect -> (eventRect.event.mStartTime!!.isTheSameDay(mFirstVisibleDay!!) && eventRect.event.mAllDay) }
                    return@firstOrNull eventRect != null
                } != null
    }

    private fun drawStaffColumnAndAxes(canvas: Canvas) {
        // Clip to paint in time column only.
        canvas.clipRect(0f, mHeaderHeight, mHeaderColumnWidth, height.toFloat(), Region.Op.REPLACE)

        val totalLine = if (countStaff < 10) 10 else countStaff

        for (i in 0 until totalLine) {
            val top = mHeaderHeight + mCurrentOrigin.y + mHourHeight * i
            val topLine = top + mHourHeight
            if (top.inScreenVertical()) {
                if (i < countStaff) {
                    //draw avatar staff
                    val radius = mSizeAvatarStaff / 2
                    val cX = mMarginLeftAvatarStaff + radius
                    val cY = top + mMarginTopAvatarStaff + radius

                    canvas.save()
                    val path = Path()
                    path.addCircle(cX, cY, radius, Path.Direction.CCW)
                    canvas.clipPath(path)
                    canvas.drawBitmap(mBitmapUserPlaceHolder, mMarginLeftAvatarStaff, top + mMarginTopAvatarStaff, mPaintAvatar)
                    canvas.restore()
                    canvas.drawCircle(cX, cY, radius, mPaintCircle)

                    i.getBitmapByPosition()?.let {
                        canvas.drawBitmap(it, mMarginLeftAvatarStaff, top + mMarginTopAvatarStaff, mPaintAvatar)
                    }

                    //draw icon delete
                    if (i != 0) {
                        val yDelete = top + mMarginTopIconDelete
                        val xDelete = mHeaderColumnWidth - mBitmapDelete?.width!!
                        val rect = Rect(xDelete.toInt(), yDelete.toInt(), xDelete.toInt() + mBitmapDelete.width, yDelete.toInt() + mBitmapDelete.height)
                        listRectBtnCancel[i] = rect
                        canvas.drawBitmap(mBitmapDelete, xDelete, yDelete, mPaintAvatar)
                    }

                    //draw text staff
                    val marginTop = mMarginTopAvatarStaff * 5 + mSizeAvatarStaff
                    val offset = mHourHeight - marginTop
                    val yText = top + marginTop + offset / 2
                    val xText = mMarginLeftTextStaff
                    canvas.drawText("田中太郎 $i", xText, yText, mPaintTextStaff)
                }
                canvas.drawLine(0f, topLine, mHeaderColumnWidth, topLine, mPaintHour)
            }
        }
    }

    private fun drawHeaderRowAndEvents(canvas: Canvas) {

        //region #FirstDraw
        val today = TimeUtils.today()
        if (mIsFirstDraw) {
            mIsFirstDraw = false
            mInitListener?.onViewCreated()
        }
        //endregion

        //region #Zoom
        if (mNewHourWidth > 0) {
            if (mNewHourWidth < mMinHourWidth)
                mNewHourWidth = mMinHourWidth
            else if (mNewHourWidth > mMaxHourWidth)
                mNewHourWidth = mMaxHourWidth

            mCurrentOrigin.x = mCurrentOrigin.x / mWidthPerHour * mNewHourWidth
            mWidthPerHour = mNewHourWidth
            mNewHourWidth = -1F
        }
        //endregion

        //region #Validate Scroll + calculate scroll offset
        if (mCurrentOrigin.y < height - getMaxOverScrollVertical() - mHeaderHeight) {
            mCurrentOrigin.y = height - getMaxOverScrollVertical() - mHeaderHeight
        }

        if (mCurrentOrigin.y > 0) {
            mCurrentOrigin.y = 0f
        }

        var leftHoursWithGaps = (-Math.ceil(((if (mCurrentOrigin.x == 0f) mCurrentOrigin.x else mCurrentOrigin.x - 5) / mWidthPerHour).toDouble())).toInt()
        //endregion

        //region #Draw line around
        run {
            val y = mHeaderHeight
            canvas.drawLine(0f, y, mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, y, mPaintHour)
        }

        run {
            val x = mHeaderColumnWidth
            canvas.drawLine(x, 0F, x, height.toFloat(), mPaintHour)
        }
        run {
            val y = height - DEFAULT_STROKE_WIDTH / 2
            canvas.drawLine(0F, y, mHeaderColumnWidth, y, mPaintHour)
        }
        run {
            val y: Float = mHeaderHeight
            canvas.drawLine(mHeaderColumnWidth - DEFAULT_STROKE_WIDTH, y, width.toFloat(), y, mPaintHour)
        }
        //endregion

        //region #Draw Date Display
        mFirstVisibleDay = (today.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (leftHoursWithGaps < 0) {
            val tmp = leftHoursWithGaps + if (leftHoursWithGaps % 24 == 0) 1 else 0
            mFirstVisibleDay!!.add(Calendar.DATE, tmp / 24 - 1)
        } else {
            mFirstVisibleDay!!.add(Calendar.DATE, leftHoursWithGaps / 24)
        }

        when (mFirstVisibleDay?.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> mPaintDateDisplay.color = mTextColorSaturday
            Calendar.SUNDAY -> mPaintDateDisplay.color = mTextColorSunday
            else -> mPaintDateDisplay.color = mTextColorWeek
        }

        if (mFirstVisibleDay!!.before(today)) {
            mPaintDateDisplay.alpha = DAY_PAST_ALPHA
            mPaintTextAllDay.alpha = DAY_PAST_ALPHA
        } else {
            mPaintDateDisplay.alpha = NORMAL_ALPHA
            mPaintTextAllDay.alpha = NORMAL_ALPHA
        }
        if (mFirstVisibleDay!!.isTheSameDay(today)) {
            canvas.drawRect(0F, 0F, mHeaderColumnWidth, height.toFloat(), mTodayBackgroundPaint)
        }

        val text = mDateTimeInterpreter.interpretDate(mFirstVisibleDay!!)
        canvas.drawText(text, mHeaderColumnWidth / 2, mHeaderTextHeight + mHeaderRowPadding, mPaintDateDisplay)
        //endregion

        //region #Calculate mHasAllDay
        checkHasAllDay(mFirstVisibleDay)
        //endregion

        //region #Draw event allday
        if (mHasAllDayEvents) {
            val x = mHeaderColumnWidth + mWidthPerHour
            canvas.drawLine(x, 0F, x, height.toFloat(), mPaintHour)
            canvas.clipRect(mHeaderColumnWidth, mHeaderHeight, width.toFloat(), height.toFloat(), Region.Op.REPLACE)
            mMapEventRects.forEachIndexed { position, mEventRects ->
                val startTop = mHeaderHeight + mCurrentOrigin.y + (mHourHeight * position).toFloat()
                if (startTop.inScreenVertical())
                    drawEventsAllDay(mEventRects, mFirstVisibleDay, canvas, startTop)
            }
        }
        //endregion

        //#region calculate startFromPixel + init value before draw event
        val startFromPixel = mCurrentOrigin.x + mWidthPerHour * leftHoursWithGaps + mHeaderColumnWidth - DEFAULT_STROKE_WIDTH
        var startPixel = startFromPixel
        var day: Calendar
        //endregion

        //region #Draw(rows , columns)
        val dashPath = Path()
        for (hourNumber in leftHoursWithGaps + 1..leftHoursWithGaps + mNumberOfVisibleDays + 1) {
            day = today.clone() as Calendar
            day.add(Calendar.HOUR_OF_DAY, hourNumber - 1)
            val hour = day.get(Calendar.HOUR_OF_DAY)

            if (mRefreshEvents || (hourNumber == leftHoursWithGaps + 1 && mFetchedPeriod != mWeekViewLoader?.toWeekViewPeriodIndex(day))) {
                getMoreEvents(day)
                mRefreshEvents = false
                countStaff = mMapEventRects.size
            }

            val clipLeft = if (mHasAllDayEvents) {
                mHeaderColumnWidth + mWidthPerHour
            } else {
                mHeaderColumnWidth
            }
            canvas.clipRect(clipLeft, mHeaderHeight, width.toFloat(), height.toFloat(), Region.Op.REPLACE)

            // Draw line vertical
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerHour + startPixel - start > 0) {
                val offset = if (mHasAllDayEvents) 1 else 0
                if (hour == offset && hourNumber != leftHoursWithGaps + 1) {
                    mPaintLine.color = mColorLineLight
                    mPaintLine.strokeWidth = STROKE_HIGHLIGHT_WIDTH
                    canvas.drawLine(startPixel - DEFAULT_STROKE_WIDTH, mHeaderHeight, startPixel - DEFAULT_STROKE_WIDTH, height.toFloat(), mPaintLine)
                    mPaintLine.color = mColorLine
                    mPaintLine.strokeWidth = DEFAULT_STROKE_WIDTH
                } else {
                    dashPath.moveTo(start, mHeaderHeight)
                    dashPath.lineTo(start, height.toFloat())
                    canvas.drawPath(dashPath, mPaintLine)
                    dashPath.reset()
                }
            }


            // Draw line horizontal
            val path = Path()
            val totalLine = if (countStaff < 10) 10 else countStaff
            for (lineHour in 0 until totalLine) {
                canvas.clipRect(mHeaderColumnWidth, 0F, width.toFloat(), height.toFloat(), Region.Op.REPLACE)
                val top = mHeaderHeight + mCurrentOrigin.y + mHourHeight * (lineHour + 1)
                if (top > mHeaderHeight - DEFAULT_STROKE_WIDTH && top < height && startPixel + mWidthPerHour - start > 0) {
                    path.moveTo(start, top)
                    path.lineTo(start + mWidthPerHour, top)
                    canvas.drawPath(path, mPaintLine)
                    path.reset()
                }

            }
            startPixel += mWidthPerHour
        }
        //endregion

        //region #DrawHeaderRow  + TextStart
        canvas.clipRect(mHeaderColumnWidth, 0F, width.toFloat(), height.toFloat(), Region.Op.REPLACE)
        startPixel = if (mHasAllDayEvents) {
            canvas.drawText(mAllDayText, mHeaderColumnWidth + mWidthPerHour / 2, mHeaderTextHeight + mHeaderRowPadding, mPaintTextAllDay)
            canvas.clipRect(mHeaderColumnWidth + DEFAULT_STROKE_WIDTH + mWidthPerHour, 0f, width.toFloat(), mHeaderHeight, Region.Op.REPLACE)
            startFromPixel + mWidthPerHour
        } else {
            canvas.clipRect(mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, 0f, width.toFloat(), mHeaderHeight, Region.Op.REPLACE)
            startFromPixel
        }

        for (hourNumber in leftHoursWithGaps + 1..leftHoursWithGaps + mNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day = today.clone() as Calendar
            day.add(Calendar.HOUR_OF_DAY, hourNumber - 1)
            val hour = day.get(Calendar.HOUR_OF_DAY)
            // Draw the day labels.
            val hourLabel = mDateTimeInterpreter.interpretTime(hour)
            val x = startPixel
            canvas.drawText(hourLabel, x, mHeaderTextHeight + mHeaderRowPadding, mPaintHour)
            startPixel += mWidthPerHour
        }
        //endregion Draw the header row texts START

        //region #DrawEvent
        val clipLeft = if (mHasAllDayEvents) {
            mHeaderColumnWidth + mWidthPerHour
        } else {
            mHeaderColumnWidth
        }
        canvas.clipRect(clipLeft, mHeaderHeight, width.toFloat(), height.toFloat(), Region.Op.REPLACE)

        startPixel = if (mHasAllDayEvents) {
            startFromPixel - BUFFER_HOUR * mWidthPerHour + mWidthPerHour
        } else {
            startFromPixel - BUFFER_HOUR * mWidthPerHour
        }
        leftHoursWithGaps -= BUFFER_HOUR
        run {
            var hourNumber = leftHoursWithGaps + 1
            var i = 0
            while (hourNumber <= leftHoursWithGaps + BUFFER_HOUR + mNumberOfVisibleDays * 2 && i < mPositionFilled.size) {
                day = today.clone() as Calendar
                day.add(Calendar.HOUR_OF_DAY, hourNumber - 1)
                mMapEventRects
                        .asSequence()
                        .forEachIndexed { index, mEventRects ->
                            val startTop = mHeaderHeight + mCurrentOrigin.y + (mHourHeight * index).toFloat()
                            if (startTop.inScreenVertical())
                                drawEvents(mEventRects, day, startPixel, canvas, startTop)
                        }
                startPixel += mWidthPerHour
                hourNumber++
                i++
            }
        }
        //endregion
    }

    private fun getMaxOverScrollVertical() = mHourHeight * countStaff

    private fun drawEventsAllDay(mEventRects: List<EventRect>, mFirstVisibleDay: Calendar?, canvas: Canvas, startTop: Float) {
        for (i in mEventRects.indices) {
            val event = mEventRects[i].event
            val eventOriginal = mEventRects[i].originalEvent

            val isTheSameDay = mFirstVisibleDay!!.isTheSameDay(event.mStartTime!!)

            if (!event.mAllDay || !isTheSameDay) continue

            val top = startTop + mEventRects[i].top * mHourHeight
            val bottom = top + mEventRects[i].height * mHourHeight - mEventSpace
            val left = mHeaderColumnWidth
            val right = left + mWidthPerHour - mEventSpace

            mEventRects[i].rectF = null
            if (left > right || startTop > height || bottom <= 0) continue
            mEventRects[i].rectF = RectF(left, top, right, bottom)

            mPaintEventBackground.alpha = NORMAL_ALPHA
            mPaintEventBackground.color = event.mColor
            if (eventOriginal.mEndTime!!.before(TimeUtils.today())) {
                mPaintEventBackground.alpha = DAY_PAST_ALPHA
            }
            canvas.drawRoundRect(mEventRects[i].rectF, mEventCornerRadius.toFloat(), mEventCornerRadius.toFloat(), mPaintEventBackground)
            drawEventTitle(mEventRects[i], canvas, top, left)
        }
    }

    private fun drawEvents(mEventRects: List<EventRect>, day: Calendar, startFromPixel: Float, canvas: Canvas, startTop: Float) {

        for (i in mEventRects.indices) {
            val event = mEventRects[i].event
            val eventOriginal = mEventRects[i].originalEvent

            val isTheSameHour = day.isTheSameHour(event.mStartTime!!)

            if (event.mAllDay || !isTheSameHour) continue

            val top = startTop + mEventRects[i].top * mHourHeight
            val hoursBetween = event.hoursBetween
            val bottom = top + mEventRects[i].height * mHourHeight - mEventSpace
            val left = startFromPixel + event.minuteStart * mWidthPerHour / 60
            val right = left + mWidthPerHour * hoursBetween - mEventSpace
            mEventRects[i].rectF = null

            if (left > right || startTop > height || bottom <= 0) continue
            if (right < mHeaderColumnWidth - mWidthPerHour) continue
            if (left > width + mWidthPerHour) continue

            mEventRects[i].rectF = RectF(left, top, right, bottom)

            mPaintEventBackground.alpha = NORMAL_ALPHA
            mPaintEventBackground.color = event.mColor
            if (eventOriginal.mEndTime!!.timeInMillis < System.currentTimeMillis()) {
                mPaintEventBackground.alpha = DAY_PAST_ALPHA
            }
            canvas.drawRoundRect(mEventRects[i].rectF, mEventCornerRadius.toFloat(), mEventCornerRadius.toFloat(), mPaintEventBackground)
            drawEventTitle(mEventRects[i], canvas, top, left)
        }
    }

    private fun drawEventTitle(eventRect: EventRect, canvas: Canvas, originalTop: Float, originalLeft: Float) {
        val rect = eventRect.rectF!!
        if (rect.right - rect.left - (mEventPadding * 2).toFloat() < 0) return
        if (rect.bottom - rect.top - mEventPadding * 2 < 0) return
        if (rect.bottom - rect.top < 0) return

        val title = eventRect.event.mName!!

        val availableHeight = (rect.bottom - originalTop - (mEventPadding * 2))
        val availableWidth = (rect.right - originalLeft - (mEventPadding * 2))
        val rec = Rect()
        val strName = "MR"
        mEventTextPaint.getTextBounds(strName, 0, strName.length, rec)
        var textLayout = StaticLayout(title, mEventTextPaint, availableWidth.toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

        val lineHeight = textLayout.height / textLayout.lineCount
        val availableLineCount = Math.max(Math.ceil(availableHeight / lineHeight.toDouble()), 1.0).toInt()
        textLayout = StaticLayout(ellipsize(title, (availableLineCount * availableWidth)), mEventTextPaint, (rect.right - originalLeft - (mEventPadding * 2).toFloat()).toInt(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)

        canvas.save()
        canvas.clipRect(originalLeft + mEventPadding, originalTop - mEventSpace + mEventPadding, rect.right, rect.bottom + mEventPadding)
        canvas.translate(originalLeft + mEventPadding, originalTop - mEventSpace + mEventPadding)
        textLayout.draw(canvas)
        canvas.restore()
    }

    private fun getMoreEvents(day: Calendar) {
        if (mWeekViewLoader == null && !isInEditMode) {
            throw IllegalStateException("You must provide a MonthChangeListener")
        }
        if (mRefreshEvents) {
            mMapEventRects.forEach {
                it.clear()
            }
            mCurrentPeriodEvents = null
            mFetchedPeriod = -1
        }

        var countMember = 0
        if (mWeekViewLoader != null) {
            val periodToFetch = mWeekViewLoader?.toWeekViewPeriodIndex(day) ?: 0
            if (!isInEditMode && (mFetchedPeriod < 0 || mFetchedPeriod != periodToFetch || mRefreshEvents)) {
                var currentPeriodEvents: List<List<WeekViewEvent>>? = null

                if (currentPeriodEvents == null)
                    currentPeriodEvents = mWeekViewLoader?.onLoad(periodToFetch)

                // Clear events.
                mMapEventRects.forEach {
                    it.clear()
                }
                countMember = currentPeriodEvents!!.size

                (0 until countMember)
                        .forEach {
                            mMapEventRects.add(mutableListOf())
                        }

                sortAndCacheEvents(currentPeriodEvents)

                mCurrentPeriodEvents = currentPeriodEvents
                mFetchedPeriod = periodToFetch
            }
        }

        // Prepare to calculate positions of each events.
        val tempEvents = mMapEventRects
        mMapEventRects = mutableListOf()
        (0 until countMember)
                .forEach {
                    mMapEventRects.add(mutableListOf())
                }

        // Iterate through each day with events to calculate the position of the events.
        tempEvents.forEachIndexed { index, it ->
            while (it.size > 0) {
                val eventRects = ArrayList<EventRect>(it.size)

                // Get first event for a day.
                val eventRect1 = it.removeAt(0)
                eventRects.add(eventRect1)

                var i = 0
                while (i < it.size) {
                    // Collect all other events for same day.
                    val eventRect2 = it[i]
                    if (eventRect1.event.mStartTime!!.isTheSameDay(eventRect2.event.mStartTime!!)) {
                        it.removeAt(i)
                        eventRects.add(eventRect2)
                    } else {
                        i++
                    }
                }
                computePositionOfEvents(index, eventRects)
            }
        }
    }

    private fun computePositionOfEvents(index: Int, eventRects: List<EventRect>) {
        // Make "collision groups" for all events that collide with others.
        val collisionGroups = ArrayList<ArrayList<EventRect>>()
        for (eventRect in eventRects) {
            var isPlaced = false

            outerLoop@ for (collisionGroup in collisionGroups) {
                for (groupEvent in collisionGroup) {
                    if (isEventsCollide(groupEvent.event, eventRect.event) && groupEvent.event.mAllDay == eventRect.event.mAllDay) {
                        collisionGroup.add(eventRect)
                        isPlaced = true
                        break@outerLoop
                    }
                }
            }

            if (!isPlaced) {
                val newGroup = ArrayList<EventRect>()
                newGroup.add(eventRect)
                collisionGroups.add(newGroup)
            }
        }

        for (collisionGroup in collisionGroups) {
            expandEventsToMaxWidth(index, collisionGroup)
        }
    }

    private fun expandEventsToMaxWidth(index: Int, collisionGroup: List<EventRect>) {
        // Expand the events to maximum possible width.
        val columns = ArrayList<ArrayList<EventRect>>()
        columns.add(ArrayList())
        for (eventRect in collisionGroup) {
            var isPlaced = false
            for (column in columns) {
                if (column.isEmpty()) {
                    column.add(eventRect)
                    isPlaced = true
                } else if (!isEventsCollide(eventRect.event, column.last().event)) {
                    column.add(eventRect)
                    isPlaced = true
                    break
                }
            }
            if (!isPlaced) {
                val newColumn = ArrayList<EventRect>()
                newColumn.add(eventRect)
                columns.add(newColumn)
            }
        }

        // Calculate left and right position for all the events.
        // Get the maxRowCount by looking in all columns.
        val maxRowCount = columns
                .map { it.size }
                .max()
                ?: 0
        for (i in 0 until maxRowCount) {
            // Set the left and right values of the event.
            var j = 0f
            for (column in columns) {
                if (column.size >= i + 1) {
                    val eventRect = column[i]
                    eventRect.height = 1f / columns.size
                    eventRect.top = j / columns.size
                    if (!eventRect.event.mAllDay) {
                        eventRect.left = (eventRect.event.mStartTime!!.get(Calendar.HOUR_OF_DAY) * 60 + eventRect.event.mStartTime!!.get(Calendar.MINUTE)).toFloat()
                        eventRect.right = (eventRect.event.mEndTime!!.get(Calendar.HOUR_OF_DAY) * 60 + eventRect.event.mEndTime!!.get(Calendar.MINUTE)).toFloat()
                        if (eventRect.left == eventRect.right) {
                            eventRect.left--
                        }
                    } else {
                        eventRect.left = 0f
                        eventRect.right = 0F
                    }
                    mMapEventRects[index].add(eventRect)
                }
                j++
            }
        }
    }

    private fun isEventsCollide(event1: WeekViewEvent, event2: WeekViewEvent): Boolean {
        val start1 = event1.mStartTime?.zeroSECONDAndMILLSECOND()
        val end1 = event1.mEndTime?.zeroSECONDAndMILLSECOND()
        val start2 = event2.mStartTime?.zeroSECONDAndMILLSECOND()
        val end2 = event2.mEndTime?.zeroSECONDAndMILLSECOND()

        return !(start1!! >= end2!! || end1!! <= start2!!)
    }

    private fun ellipsize(original: CharSequence, avail: Float): CharSequence {
        return ellipsize(original, avail, mEventTextPaint)
    }

    private fun ellipsize(original: CharSequence, avail: Float, textPaint: TextPaint): CharSequence {
        if (avail <= 0) {
            return ""
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                val c = Class.forName("android.text.TextUtils")
                val m = c.getMethod("ellipsize", CharSequence::class.java, TextPaint::class.java, Float::class.javaPrimitiveType, TextUtils.TruncateAt::class.java, Boolean::class.javaPrimitiveType, TextUtils.EllipsizeCallback::class.java, TextDirectionHeuristic::class.java, String::class.java)
                return m.invoke(null, original, textPaint, avail + 14, TextUtils.TruncateAt.END, false, null, TextDirectionHeuristics.FIRSTSTRONG_LTR, "") as CharSequence
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        return TextUtils.ellipsize(original, textPaint, avail, TextUtils.TruncateAt.END)
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
            ViewCompat.postInvalidateOnAnimation(this@DayView)
        }
        // Reset scrolling and fling direction.
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection
    }

    private fun forceFinishScroll(): Boolean = false

    //region Cache + Sort Event
    /**
     * Cache the event for smooth scrolling functionality.
     * @param event The event to cache.
     */
    private fun cacheEvent(index: Int, event: WeekViewEvent) {
        if (!event.mAllDay && event.mStartTime!! > event.mEndTime!!)
            return

        event.splitEvents()
                .map { EventRect(it, event, null) }
                .forEach { mMapEventRects[index].add(it) }
    }


    /**
     * Sort and cache events.
     * @param mapEvents The events to be sorted and cached.
     */
    private fun sortAndCacheEvents(mapEvents: List<List<WeekViewEvent>>) {
        mapEvents.forEachIndexed { index, events ->
            val sortedEvents = sortEvents(events)
            for (event in sortedEvents) {
                cacheEvent(index, event)
            }
        }
    }

    /**
     * Sorts the events in ascending order.
     * @param events The events to be sorted.
     */
    private fun sortEvents(events: List<WeekViewEvent>): List<WeekViewEvent> {
        val allDayEventList = ArrayList<WeekViewEvent>()
        val normalEventList = ArrayList<WeekViewEvent>()

        for (event in events) {
            if (event.mAllDay) {
                allDayEventList.add(event)
            } else {
                normalEventList.add(event)
            }
        }
        normalEventList.sortWith(Comparator { event1, event2 ->
            val start1 = event1.mStartTime!!.timeInMillis
            val start2 = event2.mStartTime!!.timeInMillis
            var comparator = if (start1 > start2) 1 else if (start1 < start2) -1 else 0
            if (comparator == 0) {
                val end1 = event1.mEndTime!!.timeInMillis
                val end2 = event2.mEndTime!!.timeInMillis
                comparator = if (end1 > end2) 1 else if (end1 < end2) -1 else 0
            }
            return@Comparator comparator
        })
        normalEventList.addAll(allDayEventList)
        return normalEventList
    }

    inner class EventRect(var event: WeekViewEvent, var originalEvent: WeekViewEvent, var rectF: RectF?) {
        var left: Float = 0.toFloat()
        var height: Float = 0.toFloat()
        var top: Float = 0.toFloat()
        var right: Float = 0.toFloat()

    }
    //endregion

    //--------------------------------------Public Method-------------------------------------------

    fun notifyDataSetChanged() {
        mRefreshEvents = true
        postInvalidate()
    }

    fun goToDate(calendar: Calendar) {
        mScroller?.forceFinished(true)
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        mRefreshEvents = true

        val today = TimeUtils.today()

        val day = DateUtils.DAY_IN_MILLIS
        val dateInMillis = calendar.timeInMillis + calendar.timeZone.getOffset(calendar.timeInMillis)
        val todayInMillis = today.timeInMillis + today.timeZone.getOffset(today.timeInMillis)
        val hourDifference = (dateInMillis - todayInMillis) * 24 / day
        mCurrentOrigin.x = -hourDifference * mWidthPerHour
        invalidate()
    }


    //--------------------------------------Override------------------------------------------------

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mAreDimensionsInvalid = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        //region #Init value width : header, hour; height : header
        mWidthPerHour = width.toFloat() - mHeaderColumnWidth
        mWidthPerHour /= mNumberOfVisibleDays
        mHeaderHeight = mHeaderTextHeight + mHeaderRowPadding * 2 - DEFAULT_STROKE_WIDTH / 2

        mMaxHourWidth = (width.toFloat() - mHeaderColumnWidth) / 2
        mMinHourWidth = mWidthPerHour

        //endregion
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.parseColor("#ffffff"))
        drawHeaderRowAndEvents(canvas)
        drawStaffColumnAndAxes(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(event)

        val result = mGestureDetector.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP && !mIsZooming && mCurrentFlingDirection == Direction.NONE) {
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
                    mCurrentOrigin.y = if (mScroller!!.currY.toFloat() == 0F) mScroller!!.currY.toFloat() else mScroller!!.currY.toFloat()
                mCurrentOrigin.x = mScroller!!.currX.toFloat()
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    override fun invalidate() {
        super.invalidate()
        mAreDimensionsInvalid = true
    }
    //--------------------------------------Extension-----------------------------------------------

    private fun Calendar.zeroSECONDAndMILLSECOND(): Long {
        return apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun Float.inScreenVertical() = this < height && this > -mHourHeight

    private fun Int.getBitmapByPosition(): Bitmap? {
        if (mapAvatar[this] == null) {
            val res = when (this % 8) {
                0 -> R.mipmap.ic_launcher
                1 -> R.drawable.ic_2
                2 -> R.drawable.ic_3
                3 -> R.drawable.ic_4
                4 -> R.drawable.ic_5
                5 -> R.drawable.ic_6
                6 -> R.drawable.ic_7
                else -> R.drawable.ic_8
            }

            Glide.with(context)
                    .asBitmap()
                    .load(res)
                    .apply(
                            RequestOptions()
                                    .override(mSizeAvatarStaff.toInt())
                                    .circleCrop()
                    )
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap?, transition: Transition<in Bitmap>?) {
                            mapAvatar[this@getBitmapByPosition] = resource
                            postInvalidate()
                        }
                    })
        }
        return mapAvatar[this]
    }

    //--------------------------------------Interface-----------------------------------------------

    interface EventTouchListener {

        /**
         * Triggered when touched on DayView
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