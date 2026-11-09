package cn.wildfire.chat.moment.thirdbar;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;

import cn.wildfire.chat.moment.third.widgets.TitleBar;

/**
 * Created by 大灯泡 on 2019/1/15.
 * <p>
 * titlebar alpha 变化帮助类
 */
public class TitleBarAlphaChangeHelper {
    private static final String TAG = "TitleBarAlphaChangeHelp";

    private TitleBar mTitleBar;
    private View scrolledTarget;
    private View target;
    private int scrolledAlphaCriticalOffset;
    private int fullAlphaBackgroundColor = Color.parseColor("#EDEDED");
    private int[] fullAlphaBackgroundColorSplit = new int[4];
    private final int statusBarHeight;
    private int defaultTitleBarColor;
    private OnTitleBarAlphaColorChangeListener mAlphaColorChangeListener;
    float alphaEffectRangePlusRatio = 2.0f;

    private TitleBarAlphaChangeHelper(TitleBar titleBar, View scrolledTarget, View target, OnTitleBarAlphaColorChangeListener mListener) {
        this.mTitleBar = titleBar;
        this.scrolledTarget = scrolledTarget;
        this.target = target;
        this.statusBarHeight = getStatusBarHeight(titleBar.getContext());
        this.mAlphaColorChangeListener = mListener;
        this.defaultTitleBarColor = mTitleBar.getTitleBarBackgroundColor();
        setFullAlphaBackgroundColor(fullAlphaBackgroundColor);
        check();
    }

    public static TitleBarAlphaChangeHelper handle(TitleBar titleBar, View scrolledTarget, View target) {
        return new TitleBarAlphaChangeHelper(titleBar, scrolledTarget, target, null);
    }

    public static TitleBarAlphaChangeHelper handle(TitleBar titleBar, View scrolledTarget, View target, OnTitleBarAlphaColorChangeListener listener) {
        return new TitleBarAlphaChangeHelper(titleBar, scrolledTarget, target, listener);
    }

    private void check() {
        if (mTitleBar == null || scrolledTarget == null) return;
        scrolledTarget.post(new Runnable() {
            @Override
            public void run() {
                if (scrolledAlphaCriticalOffset == 0) {
                    int[] location = new int[2];
                    target.getLocationOnScreen(location);
                    int[] titleBarLocation = new int[2];
                    mTitleBar.getLocationOnScreen(titleBarLocation);
                    scrolledAlphaCriticalOffset = Math.abs(location[1] - (titleBarLocation[1] + mTitleBar.getHeight()));
                }
                handleInternal();
            }
        });
    }

    private void handleInternal() {
        if (scrolledTarget instanceof RecyclerView) {
            ((RecyclerView) scrolledTarget).addOnScrollListener(new RecyclerView.OnScrollListener() {
                int totalScrollY;

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    totalScrollY += dy;
                    Log.d(TAG, "RecyclerViewxxxxxxxxxxxxxxx: " + dy);
                    TitleBarAlphaChangeHelper.this.onScrolled(totalScrollY, dy > 0 ? -1 : 1);
                }
            });
        } else {
            scrolledTarget.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                int lastScrollY = 0;

                @Override
                public void onScrollChanged() {
                    int scrollY = scrolledTarget.getScrollY();
                    onScrolled(scrollY, scrollY - lastScrollY > 0 ? -1 : 1);
                    lastScrollY = scrollY;
                }
            });
        }
    }

    private void onScrolled(int scrollY, int direction) {
        if (scrollY >= 0) {
            float alpha;
            int titleBarHeight = mTitleBar.getHeight();
            //第二临界值：statusbar底部
            int scrolledAlphaCriticalOffset2 = scrolledAlphaCriticalOffset + titleBarHeight;
            int effectRange = (int) (scrolledAlphaCriticalOffset2 + statusBarHeight * alphaEffectRangePlusRatio);
            //到达临界值
            if (scrollY >= scrolledAlphaCriticalOffset) {
//                if (scrollY - scrolledAlphaCriticalOffset > effectRange) return;
                int offsetY;
                //到达状态栏的临界值
                if (scrollY >= scrolledAlphaCriticalOffset2) {
                    offsetY = scrollY - scrolledAlphaCriticalOffset2;
                    alpha = Math.min((float) offsetY / (statusBarHeight * alphaEffectRangePlusRatio), 1f);
                    mTitleBar.setLeftIcon(R.drawable.back_left_black);
                    mTitleBar.setRightIcon(R.drawable.ic_camera_black);
                    mTitleBar.getLeftIconView().setAlpha(alpha);
                    mTitleBar.getRightIconView().setAlpha(alpha);
                    mTitleBar.getLeftTextView().setAlpha(alpha);
                    int color = Color.argb(alphaRatioToInt(alpha),
                            fullAlphaBackgroundColorSplit[1],
                            fullAlphaBackgroundColorSplit[2],
                            fullAlphaBackgroundColorSplit[3]);
                    mTitleBar.setTitleBarBackgroundColor(color);
                    if (mAlphaColorChangeListener != null) {
                        mAlphaColorChangeListener.onChange(alpha, color);
                    }
                } else {
                    mTitleBar.setLeftIcon(R.drawable.back_left);
                    mTitleBar.setRightIcon(R.drawable.ic_camera);
                    //如果没到statusbar的底部，图标渐隐，到达后更改
                    offsetY = scrollY - scrolledAlphaCriticalOffset;
                    alpha = Math.max(1f - (float) offsetY / titleBarHeight, 0f);
                    mTitleBar.getLeftIconView().setAlpha(alpha);
                    mTitleBar.getRightIconView().setAlpha(alpha);
                }
            } else {
                alpha = 1f;
                mTitleBar.setLeftIcon(R.drawable.back_left);
                mTitleBar.setRightIcon(R.drawable.ic_camera);
                mTitleBar.getLeftIconView().setAlpha(alpha);
                mTitleBar.getRightIconView().setAlpha(alpha);
                mTitleBar.getLeftTextView().setAlpha(0f);
                mTitleBar.setTitleBarBackgroundColor(Color.TRANSPARENT);
                if (mAlphaColorChangeListener != null) {
                    mAlphaColorChangeListener.onChange(alpha, defaultTitleBarColor);
                }
            }
        }
    }

    public int getFullAlphaBackgroundColor() {
        return fullAlphaBackgroundColor;
    }

    public TitleBarAlphaChangeHelper setFullAlphaBackgroundColor(int fullAlphaBackgroundColor) {
        this.fullAlphaBackgroundColor = fullAlphaBackgroundColor;
        fullAlphaBackgroundColorSplit[0] = Color.alpha(fullAlphaBackgroundColor);
        fullAlphaBackgroundColorSplit[1] = Color.red(fullAlphaBackgroundColor);
        fullAlphaBackgroundColorSplit[2] = Color.green(fullAlphaBackgroundColor);
        fullAlphaBackgroundColorSplit[3] = Color.blue(fullAlphaBackgroundColor);
        return this;
    }

    private int alphaRatioToInt(float alpha) {
        return (int) (alpha * 255.0f + 0.5f);
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources()
                .getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources()
                    .getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public interface OnTitleBarAlphaColorChangeListener {
        void onChange(float alpha, int color);
    }

}
