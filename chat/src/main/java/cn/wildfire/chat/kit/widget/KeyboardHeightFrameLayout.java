package cn.wildfire.chat.kit.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class KeyboardHeightFrameLayout extends FrameLayout implements InputAwareLayout.InputView {
    public KeyboardHeightFrameLayout(@NonNull Context context) {
        super(context);
    }

    public KeyboardHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public KeyboardHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KeyboardHeightFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    @Override
    public void show(int height, boolean immediate) {
        // TODO
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.height = height;
        getChildAt(0).setVisibility(VISIBLE);
        setVisibility(VISIBLE);
    }

    @Override
    public void hide(boolean immediate) {
        setVisibility(GONE);
    }

    @Override
    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }
}
