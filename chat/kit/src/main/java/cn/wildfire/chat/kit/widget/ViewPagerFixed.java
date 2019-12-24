package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class ViewPagerFixed extends ViewPager {
    private boolean noScroll = false;
    private boolean noScrollAnim = false;

    public ViewPagerFixed(@NonNull Context context) {
        super(context);
    }

    public ViewPagerFixed(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            if(noScroll) return false;
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * 设置是否能左右滑动
     * @param noScroll true 不能滑动
     */
    public void setNoScroll(boolean noScroll) {
        this.noScroll = noScroll;
    }
    /**
     * 设置没有滑动动画
     * @param noAnim false 无动画
     */
    public void setScrollAnim(boolean noAnim){
        this.noScrollAnim = noAnim;
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        if(noScroll) return false;
        return super.onTouchEvent(arg0);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item,noScrollAnim);
    }
}
