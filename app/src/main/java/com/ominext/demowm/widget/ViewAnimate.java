package com.ominext.demowm.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by Admin on 9/10/2018.
 */

public class ViewAnimate extends ImageView {
    private static final float MIN_SCALE = 1F;
    private static final float MAX_SCALE = 4F;

    private ScaleGestureDetector SGD;
    private GestureDetector GD;
    private boolean isSingleTouch;
    private float scale = 1f;
    private float width, height = 0;
    int left, top, right, bottom;

    public ViewAnimate(Context context) {
        super(context);
        init(context);
    }

    public ViewAnimate(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ViewAnimate(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOnTouchListener(new MyTouchListeners());
        SGD = new ScaleGestureDetector(context, new ScaleListener());
        GD = new GestureDetector(context, new SingleTapConfirm());
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (width == 0 && height == 0) {
            width = ViewAnimate.this.getWidth();
            height = ViewAnimate.this.getHeight();
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }
    }

    private class MyTouchListeners implements View.OnTouchListener {

        float dX, dY;

        MyTouchListeners() {
            super();
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            SGD.onTouchEvent(event);

            if (GD.onTouchEvent(event))
                return true;

            if (event.getPointerCount() > 1) {
                isSingleTouch = false;
            } else {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    isSingleTouch = true;
                }
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = ViewAnimate.this.getX() - event.getRawX();
                    dY = ViewAnimate.this.getY() - event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isSingleTouch) {
                        ViewAnimate.this.animate()
                                .x(event.getRawX() + dX)
                                .y(event.getRawY() + dY)
                                .setDuration(0)
                                .start();
                        checkDimension(ViewAnimate.this);
                    }
                    break;
                default:
                    return true;
            }
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.e("onGlobalLayout: ", scale + " " + width + " " + height);
            scale *= detector.getScaleFactor();
            scale = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
            setScale();
            return true;
        }
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (scale < MAX_SCALE) {
                scale = Math.min(MAX_SCALE, scale * 2);
            } else if (scale == MAX_SCALE) {
                scale = MIN_SCALE;
            }
            setScale();
            return true;
        }
    }

    private void setScale() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) (width * scale), (int) (height * scale));
        params.gravity = Gravity.CENTER;
        ViewAnimate.this.setLayoutParams(params);
//        ViewAnimate.this.animate()
//                .scaleY(scale)
//                .scaleX(scale)
//                .setStartDelay(0)
//                .start();
//        ScaleAnimation scaleAnimation = new ScaleAnimation(1f,2f,1f,2f);
//        scaleAnimation.setDuration(0);
//        scaleAnimation.setFillAfter(true);
//        ViewAnimate.this.startAnimation(scaleAnimation);
        checkDimension(ViewAnimate.this);
    }

    private void checkDimension(View vi) {
        if (vi.getX() > left) {
            vi.animate()
                    .x(left)
                    .y(vi.getY())
                    .setDuration(0)
                    .start();
        }

        if ((vi.getWidth() + vi.getX()) < right) {
            vi.animate()
                    .x(right - vi.getWidth())
                    .y(vi.getY())
                    .setDuration(0)
                    .start();
        }

        if (vi.getY() > top) {
            vi.animate()
                    .x(vi.getX())
                    .y(top)
                    .setDuration(0)
                    .start();
        }

        if ((vi.getHeight() + vi.getY()) < bottom) {
            vi.animate()
                    .x(vi.getX())
                    .y(bottom - vi.getHeight())
                    .setDuration(0)
                    .start();
        }
    }

}
