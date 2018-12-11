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
import android.util.TypedValue
import android.view.*
import android.widget.OverScroller
import com.ominext.demowm.R
import com.ominext.demowm.util.TimeUtils
import com.ominext.demowm.util.isTheSameDay
import java.util.*
import kotlin.collections.ArrayList

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
    }

    private var BUFFER_DAY = 84 // total = BUFFER_DAY// * 2 + mNumberOfVisibleDays
    private var mContext: Context
    private val mTimeTextPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var mTimeTextWidth: Float = 0F
    private var mTimeTextHeight: Float = 0F
    private val mHeaderTextPaint: Paint by lazy { Paint(Paint.ANTI_ALIAS_FLAG) }
    private var mHeaderTextHeight: Float = 0F
    private var mHeaderHeight: Float = 0F
    private lateinit var mGestureDetector: GestureDetectorCompat
    private var mScroller: OverScroller? = null
    private val mCurrentOrigin = PointF(0f, 0f)
    private var mCurrentScrollDirection = Direction.NONE
    private val mHeaderBackgroundPaint: Paint by lazy { Paint() }
    private var mWidthPerDay: Float = 0.toFloat()
    private val mDayBackgroundPaint: Paint by lazy { Paint() }
    private val mHourSeparatorPaint: Paint by lazy { Paint() }
    private var mHeaderMarginBottom: Float = 0F
    private val mTodayBackgroundPaint: Paint by lazy { Paint() }
    //    private val mFutureBackgroundPaint: Paint by lazy { Paint() }
//    private val mFutureWeekendBackgroundPaint: Paint by lazy { Paint() }
//    private val mPastWeekendBackgroundPaint: Paint by lazy { Paint() }
    private val mNowCirclePaint: Paint by lazy { Paint() }
    private var mNowRadius: Float = 15F
    private val mNowLinePaint: Paint by lazy { Paint() }
    //    private val mTodayHeaderTextPaint: TextPaint by lazy { TextPaint(Paint.ANTI_ALIAS_FLAG) }
    private val mEventBackgroundPaint: Paint by lazy { Paint() }

    /**
     * Width of the first column (time 01:00 - 23:00)
     */
    private var mHeaderColumnWidth: Float = 0F

    //    private var mPreviousPeriodEvents: List<EventSummary>? = null
//    private var mCurrentPeriodEvents: List<EventSummary>? = null
    //    private var mNextPeriodEvents: List<EventSummary>? = null
    private val mEventTextPaint: TextPaint by lazy { TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG) }
    private val mHeaderColumnBackgroundPaint: Paint by lazy { Paint() }
    private var mFetchedPeriod = -1 // the middle period the calendar has fetched.
    private var mRefreshEvents = false
    private var mCurrentFlingDirection = Direction.NONE

    //    private val mBitmapCollapse = ViewUtils.getBitmapFromXml(context, R.drawable.ic_arrow_up)
//    private val mBitmapExpand = ViewUtils.getBitmapFromXml(context, R.drawable.ic_arrow_down)
    private val mPaintBitmapExpandCollapse = Paint()
    private val mDestRectBitmapExpandCollapse: Rect = Rect()
    private val mSrcRectBitmapExpandCollapse: Rect = Rect()

    private val mAllDayText = "AllDay"
    private val mAllDayTextPaint: TextPaint by lazy { TextPaint(Paint.ANTI_ALIAS_FLAG) }
    private var mAllDayTextWidth: Float = 0F
    private var mAllDayTextHeight: Float = 0F

    /**
     * The first visible day in the week view.
     */
    var mFirstVisibleDay: Calendar? = null
        private set

    /**
     * The last visible day in the week view.
     */
    var mLastVisibleDay: Calendar? = null
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
//    private var mEffectiveMinHourHeight = mMinHourHeight //compensates for the fact that you can't keep zooming out.
//    private var mMaxHourHeight = 250

    var mFirstDayOfWeek = Calendar.MONDAY
        /**
         * Set the first day of the week. First day of the week is used only when the week view is first
         * drawn. It does not of any effect after user starts scrolling horizontally.
         * <p>
         * <b>Note:</b> This method will only work if the week view is set to display more than 6 days at
         * once.
         * </p>
         *
         * @param value The supported values are {@link Calendar#SUNDAY},
         *                       {@link Calendar#MONDAY}, {@link Calendar#TUESDAY},
         *                       {@link Calendar#WEDNESDAY}, {@link Calendar#THURSDAY},
         *                       {@link Calendar#FRIDAY}.
         */
        set(value) {
            field = value
            invalidate()
        }
    /**
     * Returns the last day of week.
     *
     * @return the last day of week
     */
    val mLastDayOfWeek: Int
        get() = if (mFirstDayOfWeek == Calendar.MONDAY) Calendar.SUNDAY else Calendar.SATURDAY

    var mTextSize = 16
        set(value) {
            field = value
//            mTodayHeaderTextPaint.textSize = mTextSize.toFloat()
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
    private var mNumberOfVisibleDays = 5
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

    var mEventPadding = 8
        set(value) {
            field = value
            invalidate()
        }

    var mHeaderColumnBackgroundColor = Color.WHITE
        set(value) {
            field = value
            mHeaderColumnBackgroundPaint.color = field
            invalidate()
        }

    private var mIsFirstDraw = true
    private var mAreDimensionsInvalid = true

    var mOverlappingEventGap = 0
        /**
         * Set the gap between overlapping events.
         *
         * @param value The gap between overlapping events.
         */
        set(value) {
            field = value
            invalidate()
        }

    var mEventMarginVertical = 0
        /**
         * Set the top and bottom margin of the event. The event will release this margin from the top
         * and bottom edge. This margin is useful for differentiation consecutive events.
         *
         * @param value The top and bottom margin.
         */
        set(value) {
            field = value
            invalidate()
        }

    /**
     * The scrolling speed factor in horizontal direction.
     */
    var mXScrollingSpeed = 0.6f

    private var mScrollToDay: Calendar? = null
    private var mScrollToHour = -1.0

    /**
     * Corner radius for event rect.
     */
    var mEventCornerRadius = 0

    /**
     * Whether weekends should have a background color different from the normal day background
     * color. The weekend background colors are defined by the attributes
     * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
     */
    var mShowDistinctWeekendColor = false
        /**
         * Set whether weekends should have a background color different from the normal day background
         * color. The weekend background colors are defined by the attributes
         * `futureWeekendBackgroundColor` and `pastWeekendBackgroundColor`.
         *
         * @param value True if weekends should have different background colors.
         */
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Whether past and future days should have two different background colors. The past and
     * future day colors are defined by the attributes `futureBackgroundColor` and
     * `pastBackgroundColor`.
     */
    var mShowDistinctPastFutureColor = false
        /**
         * Set whether weekends should have a background color different from the normal day background
         * color. The past and future day colors are defined by the attributes `futureBackgroundColor`
         * and `pastBackgroundColor`.
         *
         * @param value True if past and future should have two different
         *                                    background colors.
         */
        set(value) {
            field = value
            invalidate()
        }

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

    // All-day events
    private var mPositionFilled: Array<MutableSet<Int>> = Array(BUFFER_DAY * 2 + mNumberOfVisibleDays, { ArraySet<Int>() })
    private var mAllDayEventNumArray: IntArray = IntArray(BUFFER_DAY * 2 + mNumberOfVisibleDays)
    private var mOriginalAllDayEvent: BooleanArray = BooleanArray(BUFFER_DAY * 2 + mNumberOfVisibleDays)

    private var mLimitedAllDayEvents = true
    private var mToggleList: ArrayList<RectF> = arrayListOf()
    private var mNeedToScrollAllDayEvents = false
    private val mVisibleHeaderBackgroundPaint: Paint by lazy { Paint() }
    private val mBackgroundColor = Color.parseColor("#fffefc")
    private var mMinYWhenScrollingAllDayEvents: Int = 0

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
    var mInitListener: InitListener? = null
    var mEventTouchListener: EventTouchListener? = null
    //    var mEmptyViewClickListener: EmptyViewClickListener? = null
//    var mEmptyViewLongPressListener: EmptyViewLongPressListener? = null

    /**
     * Event loaders define the  interval after which the events
     * are loaded in week view. For a MonthLoader events are loaded for every month. You can define
     * your custom event loader by extending WeekViewLoader.
     */
//    var mWeekViewLoader: WeekViewLoader? = null

    private val mDateTimeInterpreter: DateTimeInterpreter by lazy {
        object : DateTimeInterpreter {

            override fun interpretDate(date: Calendar): String {
                return try {
                    val dateFormat = TimeUtils.getDateFormat(context.getString(R.string.TIME_FORMAT_WEEK))
                    dateFormat.applyPattern(context.getString(R.string.TIME_FORMAT_WEEK))
                    dateFormat.format(date.time)
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
    //    var mScrollListener: ScrollListener? = null
    var mTitleChangeListener: TitleChangeListener? = null
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
                    if (e1.y > mHeaderHeight || mNeedToScrollAllDayEvents) {
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
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), (velocityX * mXScrollingSpeed).toInt(), 0, Integer.MIN_VALUE, Integer.MAX_VALUE, (-((mHourHeight * 24).toFloat() + mHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
                    ViewCompat.postInvalidateOnAnimation(this@WeekView)
                }
                Direction.VERTICAL -> if (e1.y > mHeaderHeight || mNeedToScrollAllDayEvents) {
                    mScroller?.fling(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), 0, velocityY.toInt(), Integer.MIN_VALUE, Integer.MAX_VALUE, if (mNeedToScrollAllDayEvents) mMinYWhenScrollingAllDayEvents else (-((mHourHeight * 24).toFloat() + mHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2 - height)).toInt(), 0)
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
//            mEffectiveMinHourHeight = mMinHourHeight
//            mMaxHourHeight = a.getDimensionPixelSize(R.styleable.WeekView_maxHourHeight, mMaxHourHeight)
            mTextSize = a.getDimensionPixelSize(R.styleable.WeekView_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mTextSizeTime = a.getDimensionPixelSize(R.styleable.WeekView_textSizeTime, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mTextSizeTime.toFloat(), context.resources.displayMetrics).toInt())
            mHeaderColumnPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerColumnPadding, mHeaderColumnPadding)
            mHeaderColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColor, mHeaderColumnTextColor)
            mHeaderColumnTextColorTime = a.getColor(R.styleable.WeekView_headerColumnTextColorTime, mHeaderColumnTextColorTime)
            mHeaderSundayColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColorSunday, mHeaderSundayColumnTextColor)
            mHeaderSaturdayColumnTextColor = a.getColor(R.styleable.WeekView_headerColumnTextColorSaturday, mHeaderSaturdayColumnTextColor)
            mNumberOfVisibleDays = a.getInteger(R.styleable.WeekView_noOfVisibleDays, mNumberOfVisibleDays)
            mShowFirstDayOfWeekFirst = a.getBoolean(R.styleable.WeekView_showFirstDayOfWeekFirst, mShowFirstDayOfWeekFirst)
            mHeaderRowPadding = a.getDimensionPixelSize(R.styleable.WeekView_headerRowPadding, mHeaderRowPadding)
            mHeaderRowBackgroundColor = a.getColor(R.styleable.WeekView_headerRowBackgroundColor, mHeaderRowBackgroundColor)
            mDayBackgroundColor = a.getColor(R.styleable.WeekView_dayBackgroundColor, mDayBackgroundColor)
//            mFutureBackgroundColor = a.getColor(R.styleable.WeekView_futureBackgroundColor, mFutureBackgroundColor)
//            mPastBackgroundColor = a.getColor(R.styleable.WeekView_pastBackgroundColor, mPastBackgroundColor)
//            mFutureWeekendBackgroundColor = a.getColor(R.styleable.WeekView_futureWeekendBackgroundColor, mFutureBackgroundColor) // If not set, use the same color as in the week
//            mPastWeekendBackgroundColor = a.getColor(R.styleable.WeekView_pastWeekendBackgroundColor, mPastBackgroundColor)
            mHourSeparatorColor = a.getColor(R.styleable.WeekView_hourSeparatorColor, mHourSeparatorColor)
            mTodayBackgroundColor = a.getColor(R.styleable.WeekView_todayBackgroundColor, mTodayBackgroundColor)
            mHourSeparatorHeight = a.getDimensionPixelSize(R.styleable.WeekView_hourSeparatorHeight, mHourSeparatorHeight)
//            mTodayHeaderTextColor = a.getColor(R.styleable.WeekView_todayHeaderTextColor, mTodayHeaderTextColor)
            mEventTextSize = a.getDimensionPixelSize(R.styleable.WeekView_eventTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, mEventTextSize.toFloat(), context.resources.displayMetrics).toInt())
            mEventTextColor = a.getColor(R.styleable.WeekView_eventTextColor, mEventTextColor)
            mEventPadding = a.getDimensionPixelSize(R.styleable.WeekView_eventPadding, mEventPadding)
            mHeaderColumnBackgroundColor = a.getColor(R.styleable.WeekView_headerColumnBackground, mHeaderColumnBackgroundColor)
            mOverlappingEventGap = a.getDimensionPixelSize(R.styleable.WeekView_overlappingEventGap, mOverlappingEventGap)
            mEventMarginVertical = a.getDimensionPixelSize(R.styleable.WeekView_eventMarginVertical, mEventMarginVertical)
            mXScrollingSpeed = a.getFloat(R.styleable.WeekView_xScrollingSpeed, mXScrollingSpeed)
            mEventCornerRadius = a.getDimensionPixelSize(R.styleable.WeekView_eventCornerRadius, mEventCornerRadius)
            mShowDistinctPastFutureColor = a.getBoolean(R.styleable.WeekView_showDistinctPastFutureColor, mShowDistinctPastFutureColor)
            mShowDistinctWeekendColor = a.getBoolean(R.styleable.WeekView_showDistinctWeekendColor, mShowDistinctWeekendColor)
            mHorizontalFlingEnabled = a.getBoolean(R.styleable.WeekView_horizontalFlingEnabled, mHorizontalFlingEnabled)
            mVerticalFlingEnabled = a.getBoolean(R.styleable.WeekView_verticalFlingEnabled, mVerticalFlingEnabled)
//            mAllDayEventHeight = a.getDimensionPixelSize(R.styleable.WeekView_allDayEventHeight, mAllDayEventHeight)
            mScrollDuration = a.getInt(R.styleable.WeekView_scrollDuration, mScrollDuration)
        } finally {
            a.recycle()
        }

        init()
    }

    private fun init() {
        // Scrolling initialization.
        mGestureDetector = GestureDetectorCompat(mContext, mGestureListener)
        mScroller = OverScroller(mContext, FastOutLinearInInterpolator())

        mMinimumFlingVelocity = ViewConfiguration.get(mContext).scaledMinimumFlingVelocity
        mScaledTouchSlop = ViewConfiguration.get(mContext).scaledTouchSlop

        // Measure settings for time column.
        mTimeTextPaint.textAlign = Paint.Align.RIGHT
        mTimeTextPaint.textSize = mTextSizeTime.toFloat()
        mTimeTextPaint.color = mHeaderColumnTextColorTime
        val rect = Rect()
        mTimeTextPaint.getTextBounds("00 PM", 0, "00 PM".length, rect)
        mTimeTextHeight = rect.height().toFloat()
        mHeaderMarginBottom = -mTimeTextHeight / 2
        initTextTimeWidth()

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

//        mFutureBackgroundPaint.color = mFutureBackgroundColor

//        mPastBackgroundPaint.color = mPastBackgroundColor

//        mFutureWeekendBackgroundPaint.color = mFutureWeekendBackgroundColor

//        mPastWeekendBackgroundPaint.color = mPastWeekendBackgroundColor

        // Prepare hour separator color paint.
        mHourSeparatorPaint.color = mHourSeparatorColor
        mHourSeparatorPaint.style = Paint.Style.STROKE
        mHourSeparatorPaint.pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 0f)
        mHourSeparatorPaint.strokeWidth = DEFAULT_STROKE_WIDTH

        // Prepare today background color paint.
        mTodayBackgroundPaint.color = mTodayBackgroundColor

        // Prepare today header text color paint.
//        mTodayHeaderTextPaint.textAlign = Paint.Align.CENTER
//        mTodayHeaderTextPaint.textSize = mTextSize.toFloat()
//        mTodayHeaderTextPaint.color = mHeaderColumnTextColor

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

    /**
     * Initialize time column width. Calculate value with all possible hours (supposed widest text).
     */
    private fun initTextTimeWidth() {
        mTimeTextWidth = (0..23)
                .map {
                    // Measure time string and get max width.
                    mDateTimeInterpreter.interpretTime(it)
                }
                .map { mTimeTextPaint.measureText(it) }
                .max()
                ?: 0f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(mBackgroundColor)
        canvas.drawLine(0F, DEFAULT_STROKE_WIDTH / 2, mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, DEFAULT_STROKE_WIDTH / 2, mHeaderBackgroundPaint)
        canvas.drawLine(0F, height - DEFAULT_STROKE_WIDTH / 2, width.toFloat(), height - DEFAULT_STROKE_WIDTH / 2, mHeaderBackgroundPaint)

        drawHeaderRowAndEvents(canvas)

        drawTimeColumnAndAxes(canvas)
    }

    /**
     * Calculates the height of the header.
     */
    private fun calculateHeaderHeight() {
        //Make sure the header is the right size (depends on AllDay events)
        var containsAllDayEvent = false
        mNeedToScrollAllDayEvents = false
        mHasAllDayEvents = containsAllDayEvent
        mHeaderHeight = mHeaderTextHeight + mHeaderRowPadding * 2 + if (containsAllDayEvent) mAllDayEventHeight else 0
    }

    /**
     * Draws the time column and all the axes/separators.
     *
     * @param canvas
     */
    private fun drawTimeColumnAndAxes(canvas: Canvas) {
        // Draw the background color for the header column.
        canvas.drawRect(0f, mHeaderHeight, mHeaderColumnWidth, height.toFloat(), mHeaderColumnBackgroundPaint)

        // Clip to paint in time column only.
        canvas.clipRect(0f, mHeaderHeight, mHeaderColumnWidth, height.toFloat(), Region.Op.REPLACE)

        if (mNeedToScrollAllDayEvents) {
            return
        }

        for (i in 0..23) {
            val top = mHeaderHeight + mCurrentOrigin.y + (mHourHeight * i).toFloat() + mHeaderMarginBottom + DISTANCE_FROM_TOP

            // Draw the text if its y position is not outside of the visible area. The pivot point of the text is the point at the bottom-right corner.
            val time = mDateTimeInterpreter.interpretTime(i)
            if (top < height) {
                canvas.drawText(time, mTimeTextWidth + mHeaderColumnPadding, top + mTimeTextHeight, mTimeTextPaint)
            }
        }
    }

    /**
     * Draws the header row.
     *
     * @param canvas
     */
    private fun drawHeaderRowAndEvents(canvas: Canvas) {
        // Calculate the available width for each day.
        mHeaderColumnWidth = mTimeTextWidth + mHeaderColumnPadding * 2
        mWidthPerDay = width.toFloat() - mHeaderColumnWidth
        mWidthPerDay /= mNumberOfVisibleDays

        calculateHeaderHeight() //Make sure the header is the right size (depends on AllDay events)

        val today = TimeUtils.today()

        if (mAreDimensionsInvalid) {
            mAreDimensionsInvalid = false
            if (mScrollToDay != null)
                goToDate(mScrollToDay!!)

            mAreDimensionsInvalid = false
            if (mScrollToHour >= 0)
                goToHour(mScrollToHour)

            mScrollToDay = null
            mScrollToHour = -1.0
            mAreDimensionsInvalid = false
        }

        // Iterate through each day.
//        val oldFirstVisibleDay = mFirstVisibleDay
        mFirstVisibleDay = today.clone() as Calendar
//        mFirstVisibleDay!!.add(Calendar.DATE, -Math.round(mCurrentOrigin.x / mWidthPerDay))
//        if (mFirstVisibleDay != oldFirstVisibleDay && mScrollListener != null) {
//            if (oldFirstVisibleDay != null) {
//                mScrollListener?.onFirstVisibleDayChanged(mFirstVisibleDay!!, oldFirstVisibleDay)
//            }
//        }

        if (mIsFirstDraw) {
            mIsFirstDraw = false

            mInitListener?.onViewCreated()
            // If the week view is being drawn for the first time, then consider the first day of the week.
            if (mNumberOfVisibleDays >= 7 && today.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek && mShowFirstDayOfWeekFirst) {
                val difference = today.get(Calendar.DAY_OF_WEEK) - mFirstDayOfWeek
                mCurrentOrigin.x += mWidthPerDay * difference
            }

        }

        // If the new mCurrentOrigin.y is invalid, make it valid.
        if (mNeedToScrollAllDayEvents) {
            mCurrentOrigin.y = Math.max(mCurrentOrigin.y, mMinYWhenScrollingAllDayEvents.toFloat())
        } else if (mCurrentOrigin.y < height.toFloat() - (mHourHeight * 24).toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2 - DISTANCE_FROM_TOP) {
            mCurrentOrigin.y = height.toFloat() - (mHourHeight * 24).toFloat() - mHeaderHeight - (mHeaderRowPadding * 2).toFloat() - mHeaderMarginBottom - mTimeTextHeight / 2
        }

        // Don't put an "else if" because it will trigger a glitch when completely zoomed out and
        // scrolling vertically.
        if (mCurrentOrigin.y > 0) {
            mCurrentOrigin.y = 0f
        }

        // Consider scroll offset.
        var leftDaysWithGaps = (-Math.ceil(((if (mCurrentOrigin.x == 0f) mCurrentOrigin.x else mCurrentOrigin.x - 5) / mWidthPerDay).toDouble())).toInt()
        val startFromPixel = mCurrentOrigin.x + mWidthPerDay * leftDaysWithGaps +
                mHeaderColumnWidth - DEFAULT_STROKE_WIDTH
        var startPixel = startFromPixel

        //Draw events START
        // Prepare to iterate for each day.
        var day = today.clone() as Calendar
        day.add(Calendar.HOUR, 6)

        // Clip to paint events only.
        canvas.clipRect(mHeaderColumnWidth, mHeaderHeight + mHeaderMarginBottom + mTimeTextHeight / 2, width.toFloat(), height.toFloat(), Region.Op.REPLACE)

        mFirstVisibleDay!!.add(Calendar.DATE, -Math.round(mCurrentOrigin.x / mWidthPerDay))

        val dashPath = Path()
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {

            // Check if the day is today.
            day = today.clone() as Calendar
            mLastVisibleDay = day.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            mLastVisibleDay?.add(Calendar.DATE, dayNumber - 2)
            val sameDay = day.isTheSameDay(today)

            if (mTitleChangeListener != null && dayNumber == leftDaysWithGaps + 2) {
                mTitleChangeListener?.onTitleChange(day)
            }

            // Draw background color for each day.
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0) {
                if (day.before(today)) {
                    mDayBackgroundPaint.alpha = PAST_ALPHA
                    mHourSeparatorPaint.alpha = PAST_ALPHA
                } else {
                    mDayBackgroundPaint.alpha = NORMAL_ALPHA
                    mHourSeparatorPaint.alpha = NORMAL_ALPHA
                }

                //Draw the line separating days in normal event section: if the day is the first day of week -> draw thick separator else draw dash separator
                if (day.get(Calendar.DAY_OF_WEEK) == mFirstDayOfWeek && dayNumber != leftDaysWithGaps + 1) {
                    mDayBackgroundPaint.color = STROKE_HIGHLIGHT_COLOR
                    mDayBackgroundPaint.strokeWidth = STROKE_HIGHLIGHT_WIDTH
                    canvas.drawLine(startPixel - DEFAULT_STROKE_WIDTH, mHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel - DEFAULT_STROKE_WIDTH, height.toFloat(), mDayBackgroundPaint)
                    mDayBackgroundPaint.color = mHeaderRowBackgroundColor
                    mDayBackgroundPaint.strokeWidth = DEFAULT_STROKE_WIDTH
                } else {
                    dashPath.moveTo(start, mHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom)
                    dashPath.lineTo(start, height.toFloat())
                    canvas.drawPath(dashPath, mDayBackgroundPaint)
                    dashPath.reset()
                }

                if (sameDay) {
                    canvas.drawRect(start + DEFAULT_STROKE_WIDTH, mHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom, startPixel + mWidthPerDay - DEFAULT_STROKE_WIDTH, height - DEFAULT_STROKE_WIDTH / 2, mTodayBackgroundPaint)
                }
            }

            if (!mNeedToScrollAllDayEvents) {
                // Draw the lines for hours.
                val path = Path()
                for (hourNumber in 0..23) {
                    val top = mHeaderHeight + mCurrentOrigin.y + (mHourHeight * hourNumber).toFloat() + mTimeTextHeight / 2 + mHeaderMarginBottom
                    if (top > mHeaderHeight + mTimeTextHeight / 2 + mHeaderMarginBottom - mHourSeparatorHeight && top < height && startPixel + mWidthPerDay - start > 0) {
                        path.moveTo(start, top)
                        path.lineTo(start + mWidthPerDay, top)
                        canvas.drawPath(path, mHourSeparatorPaint)
                        path.reset()
                    }
                }

            }

            // In the next iteration, start from the next day.
            startPixel += mWidthPerDay
        }
        //Draw events END


        //Draw header, time, column START
        mHeaderBackgroundPaint.alpha = NORMAL_ALPHA

        // Hide everything in the first cell (top left corner).
        canvas.clipRect(0f, 0f, mTimeTextWidth + (mHeaderColumnPadding * 2).toFloat() + DEFAULT_STROKE_WIDTH, height.toFloat(), Region.Op.REPLACE)

        //draw line separator between column time and the rest
        canvas.drawLine(mHeaderColumnWidth, mHeaderTextHeight + mHeaderRowPadding * 2, mHeaderColumnWidth, mHeaderHeight, mHeaderBackgroundPaint)

        // Draw the partial of line separating the header (not include all day event) and all-day events
        run {
            val y: Float
            if (mHasAllDayEvents) {
                y = mHeaderTextHeight + mHeaderRowPadding * 2
                canvas.drawLine(0f, y, mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, y, mAllDayEventSeparatorPaint)
            } else {
                y = mHeaderTextHeight + mHeaderRowPadding * 2 - DEFAULT_STROKE_WIDTH / 2
                canvas.drawLine(0f, y, mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, y, mAllDayEventSeparatorPaint)
            }
        }

        //Draw the rect of time in the left column
        run {
            val y: Float
            if (mHasAllDayEvents) {
                y = mHeaderTextHeight + mHeaderRowPadding * 2 + mAllDayEventHeight - DEFAULT_STROKE_WIDTH
                canvas.drawLine(0F, y, mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, y, mHeaderBackgroundPaint)
            }
        }
        run {
            val x = mHeaderColumnWidth
            canvas.drawLine(x, mHeaderHeight, x, height.toFloat(), mHeaderBackgroundPaint)
        }
        run {
            val y = height - DEFAULT_STROKE_WIDTH / 2
            canvas.drawLine(0F, y, mHeaderColumnWidth, y, mHeaderBackgroundPaint)
        }

        //draw text all day
        if (mHasAllDayEvents) {
            canvas.drawText(mAllDayText, (mHeaderColumnWidth - mAllDayTextWidth) / 2, mHeaderTextHeight + mHeaderRowPadding * 2 + mEventPadding + mAllDayTextHeight, mAllDayTextPaint)
        }

        // Clip to paint header row only.
        canvas.clipRect(mHeaderColumnWidth + DEFAULT_STROKE_WIDTH, 0f, width.toFloat(), mHeaderHeight, Region.Op.REPLACE)

        // Draw the header background.
        canvas.drawRect(0f, 0f, width.toFloat(), mHeaderTextHeight + mHeaderRowPadding * 2, mVisibleHeaderBackgroundPaint)

        // Draw the line in the top of week view
        canvas.drawLine(mHeaderColumnWidth, DEFAULT_STROKE_WIDTH / 2, width.toFloat(), DEFAULT_STROKE_WIDTH / 2, mAllDayEventSeparatorPaint)

        //Draw the line separating the header (include all day event section) and normal event section
        run {
            val y: Float
            if (mHasAllDayEvents) {
                y = mHeaderTextHeight + mHeaderRowPadding * 2 + mAllDayEventHeight - DEFAULT_STROKE_WIDTH
                canvas.drawLine(mHeaderColumnWidth - DEFAULT_STROKE_WIDTH, y, width.toFloat(), y, mHeaderBackgroundPaint)
            } else {
                y = mHeaderTextHeight + mHeaderRowPadding * 2 - DEFAULT_STROKE_WIDTH / 2
                canvas.drawLine(mHeaderColumnWidth - DEFAULT_STROKE_WIDTH, y, width.toFloat(), y, mHeaderBackgroundPaint)
            }
        }

        // Draw the line separating the header (not include all day event section) and all-day events
        if (mHasAllDayEvents) {
            canvas.drawLine(mHeaderColumnWidth, mHeaderTextHeight + mHeaderRowPadding * 2, width.toFloat(), mHeaderTextHeight + mHeaderRowPadding * 2, mAllDayEventSeparatorPaint)
        }

        // Draw background for the all-day section.
        startPixel = startFromPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day = today.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
//            if (day.before(today)) {
//                mHeaderBackgroundPaint.alpha = PAST_ALPHA
//            } else {
//                mHeaderBackgroundPaint.alpha = NORMAL_ALPHA
//            }
            //draw line separating days in event all day section: if the day is not first day of week so don't draw
            if (day.get(Calendar.DAY_OF_WEEK) != mFirstDayOfWeek) {
                canvas.drawLine(startPixel, mHeaderTextHeight + mHeaderRowPadding * 2, startPixel, height.toFloat(), mHeaderBackgroundPaint)
            }

            //draw background all day section if the day is today
            if (day.isTheSameDay(today)) {
                canvas.drawRect(startPixel + DEFAULT_STROKE_WIDTH, DEFAULT_STROKE_WIDTH, startPixel + mWidthPerDay - DEFAULT_STROKE_WIDTH, (height - DEFAULT_STROKE_WIDTH), mTodayBackgroundPaint)
            }
            startPixel += mWidthPerDay
        }
        //Draw header, time, column END

        //region Draws all-day events START
        startPixel = startFromPixel - BUFFER_DAY * mWidthPerDay
        leftDaysWithGaps -= BUFFER_DAY
        run {
            var dayNumber = leftDaysWithGaps + 1
            var i = 0
            while (dayNumber <= leftDaysWithGaps + BUFFER_DAY * 2 + mNumberOfVisibleDays && i < mPositionFilled.size) {
                mPositionFilled[i] = HashSet()
                mAllDayEventNumArray[i] = 0
                mOriginalAllDayEvent[i] = false
                day = today.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                dayNumber++
                i++
            }
        }

        run {
            var dayNumber = leftDaysWithGaps + 1
            var i = 0
            while (dayNumber <= leftDaysWithGaps + BUFFER_DAY * 2 + mNumberOfVisibleDays && i < mPositionFilled.size) {
                day = today.clone() as Calendar
                day.add(Calendar.DATE, dayNumber - 1)
                startPixel += mWidthPerDay
                dayNumber++
                i++
            }
        }
        //endregion Draws all-day events END

        //region Draw the separator between weeks START
        startPixel = startFromPixel
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            // Draw the line separating the day in the header (include all day event)
//            canvas.drawLine(startPixel + mWidthPerDay, 0f, startPixel + mWidthPerDay, mHeaderTextHeight + mHeaderColumnPadding * 2, mHeaderBackgroundPaint)

            day = today.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            if (day.get(Calendar.DAY_OF_WEEK) == mFirstDayOfWeek && dayNumber != leftDaysWithGaps + 1) {
                mDayBackgroundPaint.color = STROKE_HIGHLIGHT_COLOR
                mDayBackgroundPaint.strokeWidth = STROKE_HIGHLIGHT_WIDTH

                // Draw the line separating the week in the all day section
                canvas.drawLine(startPixel - DEFAULT_STROKE_WIDTH, 0f, startPixel - DEFAULT_STROKE_WIDTH, height.toFloat(), mDayBackgroundPaint)
                mDayBackgroundPaint.color = mHeaderRowBackgroundColor
                mDayBackgroundPaint.strokeWidth = DEFAULT_STROKE_WIDTH
            }
            startPixel += mWidthPerDay
        }
        //endregion Draw the separator between weeks END

        //region Draw the header row texts START
        startPixel = startFromPixel
        leftDaysWithGaps += BUFFER_DAY
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            // Check if the day is today.
            day = today.clone() as Calendar
            day.add(Calendar.DATE, dayNumber - 1)
            val isSunday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val isSaturday = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
            // Draw the day labels.
            val dayLabel = mDateTimeInterpreter.interpretDate(day)
            val x = startPixel
            if (!isSaturday && !isSunday && (mHolidays[day] == null || mHolidays[day] == false)) {
                mHeaderTextPaint.color = mHeaderColumnTextColor
                if (day.before(today)) {
                    mHeaderTextPaint.alpha = DAY_PAST_ALPHA
                } else {
                    mHeaderTextPaint.alpha = NORMAL_ALPHA
                }
                canvas.drawText(dayLabel, x, mHeaderTextHeight + mHeaderRowPadding, mHeaderTextPaint)
            } else {
                mHeaderTextPaint.color = if (isSaturday && (mHolidays[day] == null || mHolidays[day] == false)) mHeaderSaturdayColumnTextColor else mHeaderSundayColumnTextColor
                if (day.before(today)) {
                    mHeaderTextPaint.alpha = DAY_PAST_ALPHA
                } else {
                    mHeaderTextPaint.alpha = NORMAL_ALPHA
                }

                // May be is draw in case of holiday, hmm... I'm still not understand what the below part code does
                // so I temporarily comment those and use my code
//                var strDay = TimeUtils.getDayOfMonth(day)
//                var strDay = day.get(Calendar.DAY_OF_MONTH).toString()
//                canvas.drawText(strDay, x, mHeaderTextHeight + mHeaderRowPadding, mHeaderTextPaint)
//                val rect = Rect()
//                strDay += "()"
//                mHeaderTextPaint.getTextBounds(strDay, 0, strDay.length, rect)
//                canvas.drawText(TimeUtils.getDayOfWeek(day), x + rect.width(), mHeaderTextHeight + mHeaderRowPadding, mHeaderTextPaint)

                // my temporary code
                canvas.drawText(dayLabel, x, mHeaderTextHeight + mHeaderRowPadding, mHeaderTextPaint)
            }
            startPixel += mWidthPerDay
        }
        //endregion Draw the header row texts START
    }

    /**
     * Get the time and date where the user clicked on.
     *
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     * @return The time and date at the clicked position.
     */
    private fun getTimeFromPoint(x: Float, y: Float): Calendar? {
        val leftDaysWithGaps = (-Math.ceil((mCurrentOrigin.x / mWidthPerDay).toDouble())).toInt()
        var startPixel = mCurrentOrigin.x + mWidthPerDay * leftDaysWithGaps +
                mHeaderColumnWidth
        for (dayNumber in leftDaysWithGaps + 1..leftDaysWithGaps + mNumberOfVisibleDays + 1) {
            val start = if (startPixel < mHeaderColumnWidth) mHeaderColumnWidth else startPixel
            if (mWidthPerDay + startPixel - start > 0 && x > start && x < startPixel + mWidthPerDay) {
                val day = TimeUtils.today()
                day.add(Calendar.DATE, dayNumber - 1)
                val pixelsFromZero = y - mCurrentOrigin.y - mHeaderHeight
                -(mHeaderRowPadding * 2).toFloat() - mTimeTextHeight / 2 - mHeaderMarginBottom
                val hour = (pixelsFromZero / mHourHeight).toInt()
                val minute = (60 * (pixelsFromZero - hour * mHourHeight) / mHourHeight).toInt()
                day.add(Calendar.HOUR, hour)
                day.set(Calendar.MINUTE, minute)
                return day
            }
            startPixel += mWidthPerDay
        }
        return null
    }


    /**
     * Returns true if an all-day event can be drawn in the end row of a particular day.
     *
     * @param allDayEventNum the number of all-day events of a particular day.
     * @return
     */
    private fun canDrawInEndRow(allDayEventNum: Int): Boolean {
        return mMaxVisibleAllDayEventNum >= allDayEventNum
    }

    /**
     * For drawing the all-day events, checks if whether it can draw an all-day event in the end row or the end row should be used to draw the text indicating the other events which can't be displayed.
     *
     * @param columnIndex the start column of event
     * @param endColumn   the end column of event
     * @param rowIndex    the current row index
     * @return
     */
    private fun checkCanDrawInEndRow(columnIndex: Int, endColumn: Int, rowIndex: Int): Boolean {
        var canDrawInEndRow = true
        var j = columnIndex
        while (j < mPositionFilled.size && j <= endColumn) {
            mPositionFilled[j].add(rowIndex)
            canDrawInEndRow = canDrawInEndRow && canDrawInEndRow(mAllDayEventNumArray[j])
            j++
        }
        return if (!mLimitedAllDayEvents) true else canDrawInEndRow
    }

    /**
     * Returns the number of all-day events which has already drawn of a particular day.
     *
     * @param columnIndex the column index of the day
     * @return
     */
    private fun getFilledRowNum(columnIndex: Int): Int {
        val rowSet = mPositionFilled[columnIndex]
        return (0 until mMaxVisibleAllDayEventNum - 1)
                .count { rowSet.contains(it) }
    }

    /**
     * Toggles the value of `'mLimitedAllDayEvents'`, then refreshes the week view.
     */
    private fun toggleAllDayEvents() {
        mLimitedAllDayEvents = !mLimitedAllDayEvents
        notifyDataSetChanged()
    }

    fun ellipsize(original: CharSequence, avail: Float): CharSequence {
        return ellipsize(original, avail, mEventTextPaint)
    }

    fun ellipsize(original: CharSequence, avail: Float, textPaint: TextPaint): CharSequence {
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

    private fun Calendar.zeroSECONDAndMILLSECOND(): Long {
        return apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    /**
     * Reload calendar setting
     * @param fontSize
     * @param firstDayOfWeek It can be SUNDAY: 0 or MONDAY: 1
     */
    fun updateCalendarSettings(fontSize: Float, firstDayOfWeek: Int) {
        var isSettingChanged = false

        val newFontSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, fontSize, context.resources.displayMetrics).toInt()
        if (mEventTextSize != newFontSize) {
            mEventTextSize = newFontSize
            mEventTextPaint.textSize = mEventTextSize.toFloat()
            mAllDayEventItemHeight = DimensionUtils.dpToPx(fontSize * 1.75F)
            isSettingChanged = true
        }

        val firstDayOfWeekInCalendar = if (firstDayOfWeek == TimeUtils.SUNDAY) {
            Calendar.SUNDAY
        } else {
            Calendar.MONDAY
        }
        if (mFirstDayOfWeek != firstDayOfWeekInCalendar) {
            mFirstDayOfWeek = firstDayOfWeekInCalendar
            isSettingChanged = true
        }

        if (isSettingChanged) {
            invalidate()
        }
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
        var leftDays = (mCurrentOrigin.x / mWidthPerDay).toDouble()

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

        val nearestOrigin = (mCurrentOrigin.x - leftDays * mWidthPerDay).toInt()

        if (nearestOrigin != 0) {
            // Stop current animation.
            mScroller?.forceFinished(true)
            // Snap to date.
            mScroller?.startScroll(mCurrentOrigin.x.toInt(), mCurrentOrigin.y.toInt(), -nearestOrigin, 0, (Math.abs(nearestOrigin) / mWidthPerDay * mScrollDuration).toInt())
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
    //      Public methods.
    //
    /////////////////////////////////////////////////////////////////

    /**
     * Show a specific day on the week view.
     *
     * @param date The date to show.
     */
    fun goToDate(date: Calendar) {
        mScroller?.forceFinished(true)
        mCurrentFlingDirection = Direction.NONE
        mCurrentScrollDirection = mCurrentFlingDirection

        date.set(Calendar.HOUR_OF_DAY, 0)
        date.set(Calendar.MINUTE, 0)
        date.set(Calendar.SECOND, 0)
        date.set(Calendar.MILLISECOND, 0)

        if (mAreDimensionsInvalid) {
            mScrollToDay = date
            return
        }

        mRefreshEvents = true

        val today = TimeUtils.today()

        val day = DateUtils.DAY_IN_MILLIS
        val dateInMillis = date.timeInMillis + date.timeZone.getOffset(date.timeInMillis)
        val todayInMillis = today.timeInMillis + today.timeZone.getOffset(today.timeInMillis)
        val dateDifference = dateInMillis / day - todayInMillis / day
        mCurrentOrigin.x = -dateDifference * mWidthPerDay
        invalidate()
    }

    /**
     * Refreshes the view and loads the events again.
     */
    fun notifyDataSetChanged() {
        mRefreshEvents = true
        invalidate()
    }

    /**
     * Vertically scroll to a specific hour in the week view.
     *
     * @param hour The hour to scroll to in 24-hour format. Supported values are 0-24.
     */
    fun goToHour(hour: Double) {
        if (mAreDimensionsInvalid) {
            mScrollToHour = hour
            return
        }

        var verticalOffset = 0
        if (hour > 24)
            verticalOffset = mHourHeight * 24
        else if (hour > 0)
            verticalOffset = (mHourHeight * hour).toInt()

        if (verticalOffset > (mHourHeight * 24 - height).toFloat() + mHeaderHeight + mHeaderMarginBottom)
            verticalOffset = ((mHourHeight * 24 - height).toFloat() + mHeaderHeight + mHeaderMarginBottom).toInt()

        mCurrentOrigin.y = (-verticalOffset).toFloat()
        invalidate()
    }

    /**
     * Get the first hour that is visible on the screen.
     *
     * @return The first hour that is visible.
     */
    fun getFirstVisibleHour(): Double {
        return (-mCurrentOrigin.y / mHourHeight).toDouble()
    }

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