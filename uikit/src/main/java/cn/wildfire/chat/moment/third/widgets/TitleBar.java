package cn.wildfire.chat.moment.third.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.wildfire.chat.kit.R;

import static cn.wildfire.chat.moment.third.widgets.TitleBar.TitleBarMode.MODE_BOTH;
import static cn.wildfire.chat.moment.third.widgets.TitleBar.TitleBarMode.MODE_LEFT;
import static cn.wildfire.chat.moment.third.widgets.TitleBar.TitleBarMode.MODE_NONE;
import static cn.wildfire.chat.moment.third.widgets.TitleBar.TitleBarMode.MODE_RIGHT;


/**
 * Created by 大灯泡 on 2016/7/20.
 * <p>
 * titlebar
 */

public class TitleBar extends FrameLayout implements View.OnClickListener, View.OnLongClickListener {

    protected String leftText;
    protected String rightText;
    @DrawableRes
    protected int leftIcon;
    @DrawableRes
    protected int rightIcon;
    protected boolean isTransparent;
    @ColorInt
    protected int mainTextColor;
    protected int mainTextSize;
    protected String mainText;

    @ColorInt
    protected int leftTextColor;
    @ColorInt
    protected int rightTextColor;
    protected int leftTextSize;
    protected int rightTextSize;
    private int rightBtnPadding;
    private int rootBackgroundColor;
    private boolean darkMode;

    private LinearLayout ll_left;
    private ImageView iv_left;
    private TextView tx_left;
    private LinearLayout ll_right;
    private ImageView iv_right;
    private TextView tx_right;
    private TextView title;
    private View root;

    Drawable background = null;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({MODE_LEFT, MODE_RIGHT, MODE_BOTH, MODE_NONE})
    public @interface TitleBarMode {
        int MODE_LEFT = 16;
        int MODE_RIGHT = 17;
        int MODE_BOTH = 18;
        int MODE_NONE = 19;
    }

    private int currentMode = MODE_LEFT;

    public TitleBar(Context context) {
        this(context, null);
    }

    public TitleBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TitleBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttrs(context, attrs);
        initView(context);
    }

    private void initFromAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TitleBar);
        background = a.getDrawable(R.styleable.TitleBar_android_background);
        isTransparent = a.getBoolean(R.styleable.TitleBar_transparentMode, false);
        leftIcon = a.getResourceId(R.styleable.TitleBar_icon_left, R.drawable.back_left);
        rightIcon = a.getResourceId(R.styleable.TitleBar_icon_right, 0);
        leftText = a.getString(R.styleable.TitleBar_text_left);
        rightText = a.getString(R.styleable.TitleBar_text_right);
        currentMode = a.getInt(R.styleable.TitleBar_mode, MODE_LEFT);
        mainTextSize = a.getDimensionPixelSize(R.styleable.TitleBar_title_text_size, 18);
        mainTextColor = a.getColor(R.styleable.TitleBar_title_text_color, Color.WHITE);
        darkMode = a.getBoolean(R.styleable.TitleBar_dark_mode, true);

        leftTextSize = a.getDimensionPixelSize(R.styleable.TitleBar_left_text_size, 16);
        leftTextColor = a.getColor(R.styleable.TitleBar_left_text_color, Color.WHITE);

        rightTextSize = a.getDimensionPixelSize(R.styleable.TitleBar_right_text_size, 16);
        rightTextColor = a.getColor(R.styleable.TitleBar_right_text_color, Color.WHITE);
        rightBtnPadding = a.getDimensionPixelSize(R.styleable.TitleBar_right_btn_padding, 0);

        mainText = a.getString(R.styleable.TitleBar_title_text);
        if (!isTransparent) {
            if (darkMode) {
                leftIcon = leftIcon == R.drawable.back_left ? R.drawable.back_left_black : leftIcon;
                leftTextColor = leftTextColor == Color.WHITE ? getResources().getColor(R.color.title_bar_dark) : leftTextColor;
                mainTextColor = mainTextColor == Color.WHITE ? getResources().getColor(R.color.title_bar_dark) : mainTextColor;
                rightTextColor = rightTextColor == Color.WHITE ? getResources().getColor(R.color.title_bar_dark) : rightTextColor;
                if (background == null) {
                    setBackgroundColor(getResources().getColor(R.color.action_bar_bg));
                }
            }
        }
        a.recycle();
    }

    private void initView(Context context) {

        View.inflate(context, R.layout.moment_widget_title_bar, this);
        root = findViewById(R.id.title_bar_root);
        ll_left = findViewById(R.id.ll_title_bar_left);
        iv_left = findViewById(R.id.ic_title_bar_left);
        tx_left = findViewById(R.id.tx_title_bar_left);

        ll_right = findViewById(R.id.ll_title_bar_right);
        iv_right = findViewById(R.id.ic_title_bar_right);
        tx_right = findViewById(R.id.tx_title_bar_right);

        title = findViewById(R.id.tx_title);

        ll_right.setOnClickListener(this);
        ll_right.setOnLongClickListener(this);
        ll_right.setVisibility(INVISIBLE);
        ll_left.setOnClickListener(this);
        title.setOnLongClickListener(this);

        setValues();

    }

    private void setValues() {
        setTransparentMode(isTransparent);
        setMode(currentMode);
        setTitleTextSize(mainTextSize);
        setTitleTextColor(mainTextColor);
        setLeftTextColor(leftTextColor);
        setLeftTextSize(leftTextSize);
        setRightTextColor(rightTextColor);
        setRightTextSize(rightTextSize);
        setTitleText(mainText);
        setLeftIcon(leftIcon);
        setRightIcon(rightIcon);
        setLeftText(leftText);
        setRightText(rightText);
    }

    public int getTitleBarMode() {
        return currentMode;
    }

    public void setMode(@TitleBarMode int mode) {
        setModeInternal(mode, true);
    }

    public void setTitleTextSize(int pxSize) {
        this.mainTextSize = pxSize;
        this.title.setTextSize(mainTextSize);
    }

    void setModeInternal(@TitleBarMode int mode, boolean changeImmediately) {
        this.currentMode = mode;
        if (changeImmediately) {
            onModeChange();
        }
    }

    private void onModeChange() {
        switch (currentMode) {
            case MODE_BOTH:
                setViewsVisible(VISIBLE, ll_left, ll_right);
                break;
            case MODE_NONE:
                setViewsVisible(GONE, ll_left, ll_right);
                break;
            case MODE_LEFT:
                setViewsVisible(VISIBLE, ll_left);
                setViewsVisible(INVISIBLE, ll_right);
                break;
            case MODE_RIGHT:
                setViewsVisible(VISIBLE, ll_right);
                setViewsVisible(INVISIBLE, ll_left);
                break;
            default:
                //都不匹配则按照left模式设置
                setViewsVisible(VISIBLE, ll_left);
                setViewsVisible(INVISIBLE, ll_right);
                break;
        }
    }

    private void setViewsVisible(int visible, View... views) {
        for (View view : views) {
            if (view.getVisibility() != visible) {
                view.setVisibility(visible);
            }
        }
    }

    public void setTransparentMode(boolean transparent) {
        if (background != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                root.setBackground(background);
            } else {
                root.setBackgroundDrawable(background);
            }
            return;
        }
        isTransparent = transparent;
        if (isTransparent) {
            setTitleBarBackgroundColor(Color.TRANSPARENT);
        } else {
            setTitleBarBackgroundColor(getResources().getColor(R.color.action_bar_bg));
        }
    }

    public int getTitleBarBackgroundColor() {
        return rootBackgroundColor;
    }

    public void setTitleBarBackgroundColor(int color) {
        if (rootBackgroundColor == color) return;
        rootBackgroundColor = color;
        root.setBackgroundColor(color);
    }

    public TextView getTitleView() {
        return title;
    }

    public void setTitleText(String titleStr) {
        if (TextUtils.isEmpty(titleStr)) {
            title.setText("");
        } else {
            title.setText(titleStr);
        }
    }

    public void setTitleText(int stringResid) {
        if (stringResid > 0) {
            title.setText(stringResid);
        }
    }


    public void setTitleTextColor(@ColorInt int mainTextColor) {
        this.mainTextColor = mainTextColor;
        this.title.setTextColor(mainTextColor);
    }

    public void setLeftIcon(int resid) {
        try {
            iv_left.setImageResource(resid);
            setShowLeftIcon(resid != 0);
            leftIcon = resid;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRightIcon(int resid) {
        try {
            iv_right.setImageResource(resid);
            setShowRightIcon(resid != 0);
            rightIcon = resid;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setLeftText(int resid) {
        if (resid > 0) {
            tx_left.setText(resid);
        }
    }

    public void setLeftText(String leftText) {
        if (TextUtils.isEmpty(leftText)) {
            tx_left.setText("");
            tx_left.setVisibility(GONE);
        } else {
            tx_left.setText(leftText);
            if (tx_left.getVisibility() != VISIBLE) {
                tx_left.setVisibility(VISIBLE);
            }
        }
    }

    public void setLeftTextColor(int color) {
        tx_left.setTextColor(color);
    }

    public void setLeftTextSize(int textSize) {
        this.leftTextSize = textSize;
        tx_left.setTextSize(textSize);
    }

    public void setRightTextColor(int color) {
        tx_right.setTextColor(color);
    }

    public void setRightTextSize(int textSize) {
        this.rightTextSize = textSize;
        tx_right.setTextSize(textSize);
    }

    public void setTitleText(CharSequence mainText) {
        if (mainText != null) {
            this.mainText = mainText.toString();
            title.setText(mainText);
        }
    }

    public void setRightText(int resid) {
        if (resid > 0) {
            if (tx_right.getVisibility() != VISIBLE) {
                tx_right.setVisibility(VISIBLE);
            }
            tx_right.setText(resid);
        }
    }

    public void setRightText(String leftText) {
        if (TextUtils.isEmpty(leftText)) {
            tx_right.setText("");
            tx_right.setVisibility(GONE);
        } else {
            if (tx_right.getVisibility() != VISIBLE) {
                tx_right.setVisibility(VISIBLE);
            }
            tx_right.setText(leftText);
        }
    }

    public void setShowLeftIcon(boolean isShow) {
        iv_left.setVisibility(isShow ? VISIBLE : GONE);
    }

    public void setShowRightIcon(boolean isShow) {
        iv_right.setVisibility(isShow ? VISIBLE : GONE);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.ll_title_bar_left) {
            if (onTitleBarClickListener != null) {
                onTitleBarClickListener.onLeftClick(ll_left, false);
            }

        } else if (i == R.id.ll_title_bar_right) {
            if (onTitleBarClickListener != null) {
                onTitleBarClickListener.onRightClick(ll_left, false);
            }

        }
    }


    @Override
    public boolean onLongClick(View v) {
        int i = v.getId();
        if (i == R.id.ll_title_bar_left) {
            if (onTitleBarClickListener != null) {
                return onTitleBarClickListener.onLeftClick(ll_left, true);
            }

        } else if (i == R.id.ll_title_bar_right) {
            if (onTitleBarClickListener != null) {
                return onTitleBarClickListener.onRightClick(ll_left, true);
            }

        } else if (i == R.id.tx_title) {
            if (onTitleBarClickListener != null) {
                return onTitleBarClickListener.onTitleLongClick(v);
            }
        }
        return false;
    }

    public LinearLayout getLeftLayout() {
        return ll_left;
    }

    public ImageView getLeftIconView() {
        return iv_left;
    }

    public TextView getLeftTextView() {
        return tx_left;
    }

    public LinearLayout getRightLayout() {
        return ll_right;
    }

    public ImageView getRightIconView() {
        return iv_right;
    }

    public TextView getRightTextView() {
        return tx_right;
    }

    public TextView getTitleText() {
        return title;
    }

    private OnTitleBarClickListener onTitleBarClickListener;

    public OnTitleBarClickListener getOnTitleBarClickListener() {
        return onTitleBarClickListener;
    }

    public void setOnTitleBarClickListener(OnTitleBarClickListener onTitleBarClickListener) {
        this.onTitleBarClickListener = onTitleBarClickListener;
    }

    public interface OnTitleBarClickListener {
        boolean onLeftClick(View v, boolean isLongClick);

        boolean onTitleLongClick(View v);

        boolean onRightClick(View v, boolean isLongClick);
    }
}
