/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import cn.wildfire.chat.kit.R;

/**
 * 把会话标题（含静音/听筒图标）画到某个 toolbar 上。
 * <p>
 * 代码原样搬自 {@code ConversationFragment}，只是把「往哪个 toolbar 上画」变成了构造参数，
 * 这样独立会话页和平板双栏右栏可以共用同一套渲染逻辑，不必各写一份。
 */
public class ConversationTitleHelper {

    /**
     * 标题文本的落点。
     * <p>
     * 独立 Activity 必须走 {@code activity.setTitle()}：它除了改 toolbar 标题，还会同步 Activity
     * 自身的 title（{@code getTitle()} 要读到它，最近任务列表也要用）。双栏宿主的右栏 toolbar
     * 不是 Activity 的 ActionBar，直接 {@code toolbar.setTitle()} 即可。
     */
    public interface TitleSetter {
        void setTitle(CharSequence title);
    }

    private final Context context;
    private final Toolbar toolbar;
    private final TitleSetter titleSetter;

    /**
     * Toolbar 的标题 TextView 没有公开 API，只能遍历子 View 找出来，找到后缓存。
     */
    private TextView toolbarTitleView;

    public ConversationTitleHelper(Context context, Toolbar toolbar, TitleSetter titleSetter) {
        this.context = context;
        this.toolbar = toolbar;
        this.titleSetter = titleSetter;
    }

    public void setTitle(CharSequence title, CharSequence subTitle, boolean silent, boolean earpiece) {
        if (!TextUtils.isEmpty(title) && (silent || earpiece)) {
            applyTitleWithIcons(title, silent, earpiece);
        } else {
            titleSetter.setTitle(title);
            if (toolbarTitleView != null) {
                toolbarTitleView.setEllipsize(TextUtils.TruncateAt.END);
            }
        }
        toolbar.setSubtitle(subTitle);
    }

    /**
     * toolbar 被回收或将要承载另一个会话时调用，丢弃标题 TextView 缓存。
     */
    public void reset() {
        toolbarTitleView = null;
    }

    private void applyTitleWithIcons(CharSequence title, boolean silent, boolean earpiece) {
        // 先设置纯文本标题，确保 Toolbar 已创建标题 TextView
        titleSetter.setTitle(title);
        toolbar.post(() -> {
            TextView titleView = findToolbarTitleView(title);
            if (titleView == null) {
                return;
            }
            int size = (int) titleView.getTextSize();
            int iconColor = ColorUtils.setAlphaComponent(titleView.getCurrentTextColor(), 0x80);

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(title == null ? "" : title);
            if (silent) {
                appendTitleIcon(ssb, context, R.drawable.ic_conversation_silent, size, iconColor);
            }
            if (earpiece) {
                appendTitleIcon(ssb, context, R.drawable.ic_conversation_earpiece, size, iconColor);
            }

            // 标题过长时从中间省略，末尾图标不会被截断
            titleView.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            titleView.setText(ssb);
        });
    }

    private void appendTitleIcon(SpannableStringBuilder ssb, Context context, int drawableRes, int size, int color) {
        Drawable icon = ContextCompat.getDrawable(context, drawableRes);
        if (icon == null) {
            return;
        }
        icon = icon.mutate();
        icon.setBounds(0, 0, size, size);
        DrawableCompat.setTint(icon, color);
        ssb.append("  "); // 与前面内容的间隔 + 图标占位符
        int iconStart = ssb.length() - 1;
        ssb.setSpan(new CenteredImageSpan(icon), iconStart, iconStart + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    }

    private TextView findToolbarTitleView(CharSequence title) {
        if (toolbarTitleView != null) {
            return toolbarTitleView;
        }
        for (int i = 0; i < toolbar.getChildCount(); i++) {
            View child = toolbar.getChildAt(i);
            if (child instanceof TextView && TextUtils.equals(((TextView) child).getText(), title)) {
                toolbarTitleView = (TextView) child;
                return toolbarTitleView;
            }
        }
        return null;
    }

    /**
     * 垂直居中显示的图标 Span，避免使用默认基线对齐导致图标偏下。
     */
    private static class CenteredImageSpan extends ReplacementSpan {
        private final Drawable drawable;

        CenteredImageSpan(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
            return drawable.getBounds().width();
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
            canvas.save();
            int drawableHeight = drawable.getBounds().height();
            Paint.FontMetricsInt fm = paint.getFontMetricsInt();
            int lineCenter = y + (fm.descent + fm.ascent) / 2;
            int transY = lineCenter - drawableHeight / 2;
            canvas.translate(x, transY);
            drawable.draw(canvas);
            canvas.restore();
        }
    }
}
