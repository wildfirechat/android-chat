package cn.wildfire.chat.moment.third.utils;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;

/**
 * @author KCrason
 * @date 2018/5/6
 */
public class Utils {

    public static int dp2px(float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, WfcUIKit.getWfcUIKit().getApplication().getResources().getDisplayMetrics());
    }

    public static int getScreenWidth() {
        return WfcUIKit.getWfcUIKit().getApplication().getResources().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return WfcUIKit.getWfcUIKit().getApplication().getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * feed 条目去掉左右内边距后，真正能放内容的宽度。
     * <p>
     * 宽屏下条目会限宽居中（见 res-moment/values-w*dp/dimens.xml），此时整屏宽度
     * 不再等于内容宽度，凡是拿宽度算排版的地方都要改用本方法。
     * 手机上内边距就是布局里原有的 16dp，取值等于"屏宽 - 32dp"。
     */
    public static int getFeedContentWidth() {
        Resources resources = WfcUIKit.getWfcUIKit().getApplication().getResources();
        int padding = resources.getDimensionPixelSize(R.dimen.moment_feed_item_padding_horizontal);
        return resources.getDisplayMetrics().widthPixels - 2 * padding;
    }

    public static int calcStatusBarHeight(Context context) {
        int statusHeight = -1;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }

    public static boolean calculateShowCheckAllText(String content) {
        Paint textPaint = new Paint();
        textPaint.setTextSize(Utils.dp2px(16f));
        float textWidth = textPaint.measureText(content);
        // 原式为 getScreenWidth() - 74dp，宽屏下条目限宽后整屏宽度已不代表正文宽度。
        // 改为从内容列宽再减去头像栏占位：内容列宽本身已扣掉条目左右各 16dp，
        // 故这里只需再减 42dp，手机上合计仍是"屏宽 - 74dp"，判定结果与改造前一致。
        float maxContentViewWidth = Utils.getFeedContentWidth() - Utils.dp2px(42f);
        float maxLines = textWidth / maxContentViewWidth;
        return maxLines > 4;
    }


    public static void startAlphaAnimation(View view, boolean isShowTranslation) {
        if (isShowTranslation && view != null) {
            ValueAnimator valueAnimator = ValueAnimator.ofFloat(0.5f, 1f);
            valueAnimator.addUpdateListener(animation -> view.setAlpha((Float) animation.getAnimatedValue()));
            valueAnimator.setDuration(500).start();
        }
    }
}
