/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * 支持缩放的FrameLayout
 * <p>
 * Child View 设置onClickLister会导致缩放失效
 */
public class ZoomableFrameLayout extends FrameLayout implements ScaleGestureDetector.OnScaleGestureListener {

    private enum Mode {
        NONE,
        DRAG,
        ZOOM
    }

    private static final String TAG = "ZoomLayout";
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;

    private boolean enableZoom = true;
    private Mode mode = Mode.NONE;
    private float scale = 1.0f;
    private float lastScaleFactor = 0f;

    // Where the finger first  touches the screen
    private float startX = 0f;
    private float startY = 0f;

    // How much to translate the canvas
    private float dx = 0f;
    private float dy = 0f;
    private float prevDx = 0f;
    private float prevDy = 0f;
    private int lastX, lastY;
    private OnClickListener onClickListener;

    public ZoomableFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public ZoomableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int y = (int) motionEvent.getY();
                int x = (int) motionEvent.getX();
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "DOWN");
                        lastX = x;
                        lastY = y;
                        if (scale > MIN_ZOOM) {
                            mode = Mode.DRAG;
                            startX = motionEvent.getX() - prevDx;
                            startY = motionEvent.getY() - prevDy;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == Mode.DRAG) {
                            dx = motionEvent.getX() - startX;
                            dy = motionEvent.getY() - startY;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mode = Mode.ZOOM;
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = Mode.NONE; // changed from DRAG, was messing up zoom
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "UP");
                        mode = Mode.NONE;
                        prevDx = dx;
                        prevDy = dy;
                        if (Math.abs(y - lastY) < 5 && Math.abs(x - lastX) < 5) {
                            //todo 如果横纵坐标的偏移量都小于五个像素，那么就把它当做点击事件触发
                            if (onClickListener != null) {
                                onClickListener.onClick(ZoomableFrameLayout.this);
                            }
                        }
                        break;
                }
                scaleDetector.onTouchEvent(motionEvent);

                if ((mode == Mode.DRAG && scale >= MIN_ZOOM) || mode == Mode.ZOOM) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    float maxDx = child().getWidth() * (scale - 1);  // adjusted for zero pivot
                    float maxDy = child().getHeight() * (scale - 1);  // adjusted for zero pivot
                    dx = Math.min(Math.max(dx, -maxDx), 0);  // adjusted for zero pivot
                    dy = Math.min(Math.max(dy, -maxDy), 0);  // adjusted for zero pivot
                    Log.i(TAG, "Width: " + child().getWidth() + ", scale " + scale + ", dx " + dx
                        + ", max " + maxDx);
                    applyScaleAndTranslation();
                }

                return true;
            }
        });
    }

    // ScaleGestureDetector
    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleDetector) {
        Log.i(TAG, "onScaleBegin");
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleDetector) {
        float scaleFactor = scaleDetector.getScaleFactor();
        Log.i(TAG, "onScale(), scaleFactor = " + scaleFactor);
        if (lastScaleFactor == 0 || (Math.signum(scaleFactor) == Math.signum(lastScaleFactor))) {
            float prevScale = scale;
            scale *= scaleFactor;
            scale = Math.max(MIN_ZOOM, Math.min(scale, MAX_ZOOM));
            lastScaleFactor = scaleFactor;
            float adjustedScaleFactor = scale / prevScale;
            // added logic to adjust dx and dy for pinch/zoom pivot point
            Log.d(TAG, "onScale, adjustedScaleFactor = " + adjustedScaleFactor);
            Log.d(TAG, "onScale, BEFORE dx/dy = " + dx + "/" + dy);
            float focusX = scaleDetector.getFocusX();
            float focusY = scaleDetector.getFocusY();
            Log.d(TAG, "onScale, focusX/focusy = " + focusX + "/" + focusY);
            dx += (dx - focusX) * (adjustedScaleFactor - 1);
            dy += (dy - focusY) * (adjustedScaleFactor - 1);
            Log.d(TAG, "onScale, dx/dy = " + dx + "/" + dy);
        } else {
            lastScaleFactor = 0;
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleDetector) {
        Log.i(TAG, "onScaleEnd");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            scale = 1;
            dx = 0;
            dy = 0;
            applyScaleAndTranslation();
        }
    }

    private void applyScaleAndTranslation() {
        if (child() == null || !enableZoom) {
            return;
        }
        child().setScaleX(scale);
        child().setScaleY(scale);
        child().setPivotX(0f);  // default is to pivot at view center
        child().setPivotY(0f);  // default is to pivot at view center
        child().setTranslationX(dx);
        child().setTranslationY(dy);
    }

    private View child() {
        return getChildAt(0);
    }

    @Override
    public void setOnClickListener(@Nullable View.OnClickListener l) {
        this.onClickListener = l;
    }

    /**
     * 是否开启缩放，默认开启
     *
     * @param enable
     */
    public void setEnableZoom(boolean enable) {
        this.enableZoom = enable;
    }
}
