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
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

/**
 * 支持缩放改变子 view 大小的FrameLayout
 * 仅支持包含一个子 view
 * <p>
 */
public class ResizableFrameLayout extends FrameLayout implements ScaleGestureDetector.OnScaleGestureListener {

    private enum Mode {
        NONE,
        // 下拉关闭
        DRAG,
        // 放大之后，拖拽查看
        DRAG_ZOOM,
        ZOOM
    }

    private static final String TAG = "ResizableLayout";
    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 4.0f;

    private boolean enableZoom = false;
    private boolean enableDrag = true;
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
    private int originalWidth;
    private int originalHeight;
    private OnClickListener onClickListener;

    public ResizableFrameLayout(Context context) {
        super(context);
        init(context);
    }

    public ResizableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ResizableFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(context, this);
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (child() == null) {
                    return false;
                }
                if (!enableZoom) {
                    return false;
                }
                int y = (int) motionEvent.getY();
                int x = (int) motionEvent.getX();
                switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        Log.i(TAG, "DOWN");
                        lastX = x;
                        lastY = y;
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
                                // do nothing
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
                        mode = Mode.NONE;
                        prevDx = dx;
                        prevDy = dy;
                        if (Math.abs(y - lastY) < 5 && Math.abs(x - lastX) < 5) {
                            //todo 如果横纵坐标的偏移量都小于五个像素，那么就把它当做点击事件触发
                            if (onClickListener != null) {
                                Log.d(TAG, "click");
                                onClickListener.onClick(ResizableFrameLayout.this);
                            }
                        }
                        break;
                }
                scaleDetector.onTouchEvent(motionEvent);

                if ((mode == Mode.DRAG_ZOOM && scale >= MIN_ZOOM) || mode == Mode.ZOOM) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    float maxDx = child().getWidth() - getWidth();  // adjusted for zero pivot
                    float maxDy = child().getHeight() - getHeight();  // adjusted for zero pivot

                    dx = Math.min(Math.max(dx, -maxDx), 0);  // adjusted for zero pivot
                    dy = Math.min(Math.max(dy, -maxDy), 0);  // adjusted for zero pivot
                    Log.i(TAG, "Width: " + child().getWidth() + ", scale " + scale + ", dx " + dx
                        + ", dy " + dy + ", max " + maxDx);
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
    }

    private void applyScaleAndTranslation() {
        if (child() == null) {
            return;
        }
        View view = child();
        if (this.originalWidth == 0) {
            // 等待子 view layout 之后，在获取其默认大小
            this.originalWidth = view.getWidth();
            this.originalHeight = view.getHeight();
//            view.addOnLayoutChangeListener(new OnLayoutChangeListener() {
//                @Override
//                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//                    int newWidth = right - left;
//                    int newHeight = bottom - top;
//                    int oldWidth = oldRight - oldLeft;
//                    int oldHeight = oldBottom - oldTop;
//
//                    // 判断是否宽高发生变化
//                    if (newWidth != oldWidth || newHeight != oldHeight) {
//                        v.setScaleX(1.0f);
//                        v.setScaleY(1.0f);
//                    }
//                }
//            });
        }

        // 获取当前布局参数
        ViewGroup.LayoutParams lp = view.getLayoutParams();

        // 假设原始大小已保存为原始宽高 originalWidth/originalHeight
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 设置 view 大小，会触发surfaceView 重新 layout，及 video frame重新渲染
        if (lp.width != newWidth || lp.height != newHeight) {
//
//            // 先对原始 view 进行缩放
//            view.setScaleX(scale);
//            view.setScaleY(scale);
//
            lp.width = newWidth;
            lp.height = newHeight;
            view.setLayoutParams(lp);
        } else {
            view.setTranslationX(dx);
            view.setTranslationY(dy);
        }

        Log.d(TAG, "applyResizeAndTranslation " + scale + " " + newWidth + " " + newHeight + " " + originalWidth + " " + originalHeight);
    }

    private View child() {
        return getChildAt(0);
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        Log.d(TAG, "setOnClickListener");
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

//    public void setEnableDrag(boolean enable) {
//        this.enableDrag = enable;
//    }

    public void reset() {
        View view = child();
        if (view != null && this.originalWidth > 0) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            lp.width = originalWidth;
            lp.height = originalHeight;
            view.setLayoutParams(lp);
        }
    }
}
