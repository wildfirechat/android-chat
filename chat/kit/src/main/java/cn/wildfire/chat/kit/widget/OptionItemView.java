package cn.wildfire.chat.kit.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import cn.wildfirechat.chat.R;

public class OptionItemView extends LinearLayout {
    private ImageView startImageView;
    private ImageView endImageView;
    private ImageView arrowIndicator;
    private TextView titleTextView;
    private TextView descTextView;
    private TextView badgeTextView;
    private View dividerView;

    private String title;
    private String desc;
    private int badgeCount;
    private int dividerVisibility = VISIBLE;

    public OptionItemView(Context context) {
        super(context);
        init(context, null);
    }

    public OptionItemView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public OptionItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public OptionItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.option_item, this);
        startImageView = view.findViewById(R.id.leftImageView);
        endImageView = view.findViewById(R.id.rightImageView);
        arrowIndicator = view.findViewById(R.id.arrowImageView);

        titleTextView = view.findViewById(R.id.titleTextView);
        descTextView = view.findViewById(R.id.descTextView);
        badgeTextView = view.findViewById(R.id.badgeTextView);

        dividerView = view.findViewById(R.id.dividerLine);

        if (attrs == null) {
            return;
        }

        boolean alignDividerToTitle = false;
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.OptionItemView);
        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int attr = typedArray.getIndex(i);
            switch (attr) {
                case R.styleable.OptionItemView_start_src:
                    int resId = typedArray.getResourceId(attr, 0);
                    if (resId != 0) {
                        startImageView.setVisibility(VISIBLE);
                        startImageView.setImageResource(resId);
                    } else {
                        startImageView.setVisibility(GONE);
                    }
                    break;
                case R.styleable.OptionItemView_title:
                    titleTextView.setText(typedArray.getString(attr));
                    break;
                case R.styleable.OptionItemView_badge_count:
                    int count = typedArray.getInt(attr, 0);
                    if (count > 0) {
                        badgeTextView.setVisibility(VISIBLE);
                        count = count > 99 ? 99 : count;
                        badgeTextView.setText(count + "");
                    }
                    break;
                case R.styleable.OptionItemView_desc:
                    String desc = typedArray.getString(attr);
                    if (!TextUtils.isEmpty(desc)) {
                        descTextView.setVisibility(VISIBLE);
                        descTextView.setText(desc);
                    }
                    break;
                case R.styleable.OptionItemView_end_src:
                    resId = typedArray.getResourceId(attr, 0);
                    if (resId != 0) {
                        endImageView.setImageResource(resId);
                        endImageView.setVisibility(View.VISIBLE);
                    }
                    break;
                case R.styleable.OptionItemView_show_arrow_indicator:
                    boolean show = typedArray.getBoolean(attr, false);
                    arrowIndicator.setVisibility(show ? VISIBLE : GONE);
                    break;

                case R.styleable.OptionItemView_divider_align_to_title:
                    alignDividerToTitle = typedArray.getBoolean(attr, false);
                    break;

                default:
                    break;
            }
        }

        if (alignDividerToTitle) {
            LayoutParams layoutParams = (LayoutParams) dividerView.getLayoutParams();
            int margin = startImageView.getVisibility() == VISIBLE ? 72 : 16;
            layoutParams.leftMargin = dp2px(margin);
            dividerView.setLayoutParams(layoutParams);
            dividerView.invalidate();
        }

        if (!TextUtils.isEmpty(title)) {
            titleTextView.setText(title);
        }
        if (!TextUtils.isEmpty(desc)) {
            descTextView.setText(desc);
        }
        if (badgeCount > 0) {
            badgeTextView.setText("" + badgeCount);
        }
    }

    public ImageView getStartImageView() {
        return startImageView;
    }

    public ImageView getEndImageView() {
        return endImageView;
    }

    public void setTitle(String title) {
        this.title = title;
        if (titleTextView != null) {
            titleTextView.setText(title);
        }
    }

    public void setDesc(String desc) {
        this.desc = desc;
        if (descTextView != null) {
            if (TextUtils.isEmpty(desc)) {
                descTextView.setVisibility(GONE);
            } else {
                descTextView.setVisibility(VISIBLE);
                descTextView.setText(desc);
            }
        }
    }

    public void setDividerVisibility(int visibility) {
        this.dividerVisibility = visibility;
        if (dividerView != null) {
            dividerView.setVisibility(visibility);
        }
    }

    public void setBadgeCount(int count) {
        this.badgeCount = count;
        if (badgeTextView != null) {
            badgeTextView.setVisibility(count > 0 ? VISIBLE : GONE);
            badgeTextView.setText(count + "");
        }
    }

    private int dp2px(int dip) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dip * density + 0.5f);
    }
}