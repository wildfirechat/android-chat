/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * 支持缩放的FrameLayout
 * <p>
 * Child View 设置onClickLister会导致缩放失效
 */
public class ZoomableFrameLayout extends FrameLayout implements ScaleGestureDetector.OnScaleGestureListener {

    private enum Mode {
        NONE,
        // 下拉关闭
        DRAG,
        // 放大之后，拖拽查看
        DRAG_ZOOM,
        ZOOM
    }

    private static final String TAG = "ZoomLayout";
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;

    private boolean touchListener = false;
    private boolean enableZoom = false;
    private boolean enableDrag = false;
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
    private OnDragToFinishListener dragListener;

    // Drag-to-dismiss tracking (WeChat-style scale + translate)
    private float currentDragScale = 1f;
    private float currentDragDeltaX = 0f;
    private float currentDragDeltaY = 0f;
    private float currentDragPivotX = 0f;
    private float currentDragPivotY = 0f;

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
                if (child() == null) {
                    return false;
                }
                if (!enableZoom && !enableDrag) {
                    return false;
                }
                int y = (int) motionEvent.getY();
                int x = (int) motionEvent.getX();
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "DOWN");
                        lastX = x;
                        lastY = y;
                        currentDragScale = 1f;
                        currentDragDeltaX = 0f;
                        currentDragDeltaY = 0f;
                        currentDragPivotX = x;
                        currentDragPivotY = y;
                        if (scale > MIN_ZOOM) {
                            mode = Mode.DRAG_ZOOM;
                            startX = motionEvent.getX() - prevDx;
                            startY = motionEvent.getY() - prevDy;
                        } else {
                            mode = Mode.DRAG;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (enableDrag) {
                            if (mode == Mode.DRAG_ZOOM) {
                                dx = motionEvent.getX() - startX;
                                dy = motionEvent.getY() - startY;
                            } else if (mode == Mode.DRAG) {
                                float deltaX = motionEvent.getX() - lastX;
                                float deltaY = motionEvent.getY() - lastY;
                                float dragScale = Math.max(1f - Math.abs(deltaY) / getViewHeight(), 0.4f);
                                currentDragScale = dragScale;
                                currentDragDeltaX = deltaX;
                                currentDragDeltaY = deltaY;
                                // Pivot must be in child-local coordinates so scaling follows the finger
                                float pivotX = lastX - child().getLeft();
                                float pivotY = lastY - child().getTop();
                                currentDragPivotX = pivotX;
                                currentDragPivotY = pivotY;
                                child().setPivotX(pivotX);
                                child().setPivotY(pivotY);
                                child().setScaleX(dragScale);
                                child().setScaleY(dragScale);
                                child().setTranslationX(deltaX);
                                child().setTranslationY(deltaY);
                                if (dragListener != null) {
                                    dragListener.onDragOffset(Math.abs(deltaY), getViewHeight() / 6f);
                                }
                            }
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
                        if (enableDrag && mode == Mode.DRAG) {
                            boolean shouldDismiss = scale <= 1 && Math.abs(currentDragDeltaY) >= getViewHeight() / 6f;
                            if (!shouldDismiss) {
                                // Snap back: animate translation and scale back to neutral
                                new ResetDragAnimator(ZoomableFrameLayout.this, currentDragDeltaX, currentDragDeltaY, currentDragScale);
                            } else {
                                // Dismiss: provide current child screen rect for return animation
                                RectF childScreenRect = computeChildScreenRect();
                                if (dragListener != null) {
                                    dragListener.onDragToFinishWithCurrentViewRect(childScreenRect);
                                }
                            }
                        }
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

                if ((mode == Mode.DRAG_ZOOM && scale >= MIN_ZOOM) || mode == Mode.ZOOM) {
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

    public void setOnDragListener(OnDragToFinishListener dragListener) {
        this.dragListener = dragListener;
    }

    /**
     * 是否开启缩放，默认开启
     *
     * @param enable
     */
    public void setEnableZoom(boolean enable) {
        this.enableZoom = enable;
    }

    public void setEnableDrag(boolean enable) {
        this.enableDrag = enable;
    }

    public void reset() {
        setTranslationX(0);
        setTranslationY(0);
    }

    private float getActiveY(MotionEvent event) {
        try {
            return event.getY(event.findPointerIndex(0));
        } catch (Exception e) {
            return event.getY();
        }
    }

    private int getViewHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    /**
     * Returns the child view's current bounding rect in screen coordinates,
     * accounting for the drag-induced pivot, scale and translation.
     */
    private RectF computeChildScreenRect() {
        if (child() == null) return null;
        int[] loc = new int[2];
        getLocationOnScreen(loc);

        float s  = currentDragScale;
        float px = currentDragPivotX;   // = lastX (initial touch X in view coords)
        float py = currentDragPivotY;   // = lastY
        float tx = currentDragDeltaX;
        float ty = currentDragDeltaY;

        // px, py are pivot in child-local coords. Child is offset inside ZoomableFrameLayout
        // by child().getLeft()/getTop(). A child-local point (vx, vy) maps to screen:
        //   screen_x = loc[0] + child().getLeft() + px*(1-s) + s*vx + tx
        //   screen_y = loc[1] + child().getTop()  + py*(1-s) + s*vy + ty
        float left   = loc[0] + child().getLeft() + px * (1 - s) + tx;
        float top    = loc[1] + child().getTop()  + py * (1 - s) + ty;
        float right  = left + child().getWidth()  * s;
        float bottom = top  + child().getHeight() * s;
        return new RectF(left, top, right, bottom);
    }

    /**
     * Returns the child view's screen rect at its natural (no-drag) position.
     * Safe to call at any time, including when no drag has occurred.
     */
    public RectF getChildNaturalScreenRect() {
        if (child() == null) return null;
        int[] loc = new int[2];
        child().getLocationOnScreen(loc);
        return new RectF(loc[0], loc[1], loc[0] + child().getWidth(), loc[1] + child().getHeight());
    }

    /** Animates the dragged child back to its neutral position (scale=1, translateX/Y=0). */
    private static class ResetDragAnimator extends ValueAnimator {
        private final WeakReference<ZoomableFrameLayout> mRef;

        ResetDragAnimator(ZoomableFrameLayout layout, float fromX, float fromY, float fromScale) {
            mRef = new WeakReference<>(layout);
            setValues(
                android.animation.PropertyValuesHolder.ofFloat("translateX", fromX, 0f),
                android.animation.PropertyValuesHolder.ofFloat("translateY", fromY, 0f),
                android.animation.PropertyValuesHolder.ofFloat("scale", fromScale, 1f)
            );
            setDuration(200);
            setInterpolator(new android.view.animation.DecelerateInterpolator());
            addUpdateListener(animation -> {
                ZoomableFrameLayout l = mRef.get();
                if (l != null && l.child() != null) {
                    float tx = (Float) animation.getAnimatedValue("translateX");
                    float ty = (Float) animation.getAnimatedValue("translateY");
                    float sc = (Float) animation.getAnimatedValue("scale");
                    l.child().setTranslationX(tx);
                    l.child().setTranslationY(ty);
                    l.child().setScaleX(sc);
                    l.child().setScaleY(sc);
                    if (l.dragListener != null) {
                        l.dragListener.onDragOffset(Math.abs(ty), l.getViewHeight() / 6f);
                    }
                }
            });
            start();
        }
    }

}
