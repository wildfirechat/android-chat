/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cn.wildfire.chat.kit.R;

/**
 * 底部弹出面板的基类：手机上还是 BottomSheet，宽屏下自动变成居中对话框。形态选择的理由见
 * {@link WfcSheetDialogCompat}。
 * <p>
 * 子类照常写 {@code onCreateView}，<strong>不需要知道自己是哪种形态</strong>。只有一类代码要区分：
 * 直接操作 {@code BottomSheetBehavior} 的（展开状态、键盘弹出时收起底部按钮行）——那些行为在
 * 居中对话框里没有对应物，用 {@link #isCenteredDialog()} 跳过即可。
 * <p>
 * 父类 {@code BottomSheetDialogFragment} 的所有 BottomSheet 专属逻辑（带动画的 dismiss）都是
 * {@code instanceof BottomSheetDialog} 保护的，这里返回一个普通 Dialog 会自动退化成
 * {@code super.dismiss()}，不会出问题。
 */
public abstract class WfcBottomSheetDialogFragment extends BottomSheetDialogFragment {

    /**
     * 本次是以居中对话框呈现，还是底部面板。
     */
    protected boolean isCenteredDialog() {
        return WfcSheetDialogCompat.isCentered(getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (!isCenteredDialog()) {
            return super.onCreateDialog(savedInstanceState);
        }
        // 这里不能用 WfcSheetDialogCompat.create()：DialogFragment 会在 onStart 里 show()，
        // 窗口属性统一放到下面的 onStart 里套，避免和父类的时序打架。
        return new AppCompatDialog(requireContext(), R.style.WfcCenteredDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && isCenteredDialog()) {
            WfcSheetDialogCompat.applyCenteredWindow(dialog);
        }
    }
}
