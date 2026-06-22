/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;

import cn.wildfire.chat.kit.R;

public class ToastUtils {

    private ToastUtils() {
    }

    /**
     * 居中展示一个带图标和文字的提示 Toast（类似微信“听筒播放”等提示）。
     *
     * @param iconResId 图标资源，传 0 表示不显示图标
     * @param text      提示文字
     */
    @SuppressWarnings("deprecation")
    public static void showTipToast(Context context, @DrawableRes int iconResId, CharSequence text) {
        Toast toast = new Toast(context.getApplicationContext());
        View view = LayoutInflater.from(context).inflate(R.layout.tip_toast, null);
        ImageView iconImageView = view.findViewById(R.id.tipIconImageView);
        TextView textView = view.findViewById(R.id.tipTextView);
        if (iconResId != 0) {
            iconImageView.setImageResource(iconResId);
            iconImageView.setVisibility(View.VISIBLE);
        } else {
            iconImageView.setVisibility(View.GONE);
        }
        textView.setText(text);
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }
}
