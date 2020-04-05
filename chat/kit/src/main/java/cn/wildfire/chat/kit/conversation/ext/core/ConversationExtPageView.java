package cn.wildfire.chat.kit.conversation.ext.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.List;

import cn.wildfirechat.chat.R;

public class ConversationExtPageView extends LinearLayout implements View.OnClickListener {
    private OnExtViewClickListener listener;
    private int pageIndex;
    public static final int EXT_PER_PAGE = 8;

    public ConversationExtPageView(Context context) {
        super(context);
        init(context);
    }

    public ConversationExtPageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConversationExtPageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConversationExtPageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.conversation_ext_layout, this, false);
        addView(view);
    }

    public void updateExtViews(List<ConversationExt> exts) {
        int[][] states = new int[][]{
            new int[]{android.R.attr.state_pressed},  // pressed
            new int[]{}
        };

        int[] colors = new int[]{
            Color.parseColor("#D9D9D9"),
            Color.WHITE
        };

        ColorStateList stateList = new ColorStateList(states, colors);

        for (int index = 0; index < exts.size(); index++) {
            ImageView iconImageView = findViewWithTag("icon_" + index);
            iconImageView.setImageResource(exts.get(index).iconResId());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                iconImageView.setImageTintList(stateList);
                iconImageView.setImageTintMode(PorterDuff.Mode.MULTIPLY);
            }

            iconImageView.setOnClickListener(this);
            TextView titleTextView = findViewWithTag("title_" + index);
            titleTextView.setText(exts.get(index).title(getContext()));
        }
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        int index = Integer.parseInt(tag.substring(tag.lastIndexOf("_") + 1));
        if (listener != null) {
            listener.onClick(pageIndex * EXT_PER_PAGE + index);
        }
    }

    public void setOnExtViewClickListener(OnExtViewClickListener listener) {
        this.listener = listener;
    }

    public interface OnExtViewClickListener {
        void onClick(int index);
    }
}
