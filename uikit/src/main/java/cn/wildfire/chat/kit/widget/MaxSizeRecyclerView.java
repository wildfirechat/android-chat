package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class MaxSizeRecyclerView extends RecyclerView {
    private int mMaxWidth;
    private int mMaxHeight;

    public MaxSizeRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public MaxSizeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaxSizeRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxSizeRecyclerView);
        mMaxWidth = a.getLayoutDimension(R.styleable.MaxSizeRecyclerView_maxWidth, 0);
        mMaxHeight = a.getLayoutDimension(R.styleable.MaxSizeRecyclerView_maxHeight, 0);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (mMaxWidth > 0) {
            widthSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.AT_MOST);
        }
        if (mMaxHeight > 0) {
            heightSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthSpec, heightSpec);
    }
}
