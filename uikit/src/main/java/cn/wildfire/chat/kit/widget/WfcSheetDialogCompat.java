/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.WfcDeviceUtils;

/**
 * 底部弹出面板（BottomSheet）在宽屏下改为<strong>居中对话框</strong>。
 * <p>
 * 底部面板是手机的形态：单手拇指够得到、内容贴着屏幕底边。同一个面板放到平板上会横着铺满
 * 整条屏幕宽度，内容被拉成又扁又长的一带，视线焦点还落在屏幕最边缘。宽屏下的等价形态是
 * 居中对话框 —— 微信平板端的转发确认框也是这么做的。
 * <p>
 * <strong>判定用 {@link WfcDeviceUtils#isTwoPaneLayout}（即 sw600dp）而不是「是不是平板设备」</strong>：
 * 平板分屏到窄窗口时，窗口本身已经和手机一样窄，那时底部面板反而才是对的形态。这也与
 * {@code drawable-sw600dp/shape_bottom_sheet_bg.xml}（居中形态下四角都要圆）用的是同一个条件，
 * 两者永远一致。
 * <p>
 * <strong>手机端逐字节不变</strong>：{@code isTwoPaneLayout()} 为 false 时返回的就是原来那个
 * {@link BottomSheetDialog}。
 */
public final class WfcSheetDialogCompat {

    private WfcSheetDialogCompat() {
    }

    /**
     * 当前窗口下，底部面板是否应该改成居中对话框。
     */
    public static boolean isCentered(@Nullable Context context) {
        return context != null && WfcDeviceUtils.isTwoPaneLayout(context);
    }

    /**
     * 造一个「手机上是底部面板、宽屏上是居中对话框」的 Dialog。
     * <p>
     * 两种形态都是普通 {@link Dialog}，{@code setContentView} / {@code show} / {@code dismiss}
     * 用法完全一致，调用方不需要区分。
     */
    @NonNull
    public static Dialog create(@NonNull Context context) {
        if (!isCentered(context)) {
            return new BottomSheetDialog(context);
        }
        Dialog dialog = new AppCompatDialog(context, R.style.WfcCenteredDialog);
        applyCenteredWindow(dialog);
        return dialog;
    }

    /**
     * 把一个已经创建好的 Dialog 摆成居中卡片：限宽、居中、键盘弹出时压缩而不是盖住。
     * <p>
     * 宽度取 {@code wfc_dialog_max_width} 与窗口宽度 90% 的较小值 —— 前者保证平板上不被拉宽，
     * 后者保证分屏到很窄的窗口时不会顶出屏幕。
     */
    public static void applyCenteredWindow(@NonNull Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        Resources res = dialog.getContext().getResources();
        int maxWidth = res.getDimensionPixelSize(R.dimen.wfc_dialog_max_width);
        int available = (int) (res.getDisplayMetrics().widthPixels * 0.9f);

        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = Math.min(maxWidth, available);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        window.setAttributes(lp);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
