/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * 非文字元素随字号放大的「封顶比例」工具。
 * <p>
 * 文字本身由全局 {@link FontScaleUtils} 的 fontScale 自由缩放；但图标 / 头像 / 固定行高若
 * 1:1 跟随会被超大字号撑爆，故统一封顶：图标/头像/固定尺寸到 {@link #CAP}，可被内容撑高的
 * 行（minHeight）放宽到 {@link #ROW}。
 * <p>
 * 缩放算法与鸿蒙端一致：scaledSize = base * min(fontScale, cap)。
 */
public class LayoutScale {
    /** 图标 / 头像 / 徽标 / 固定高度 / startMargin 的封顶比例。 */
    public static final float CAP = 1.2f;
    /** 列表行 minHeight（能被内容撑高，可放宽）的封顶比例。 */
    public static final float ROW = 1.3f;

    /**
     * 当前字号下，某个封顶比例对应的实际缩放系数 = min(fontScale, cap)。
     */
    public static float factor(Context context, float cap) {
        return Math.min(FontScaleUtils.getFontScale(context), cap);
    }

    /**
     * 按 cap 缩放 View 的固定宽和高（仅缩放确定的正数尺寸，忽略 match_parent/wrap_content）。
     * 适用于图标、头像等固定尺寸元素。
     */
    public static void scaleViewSize(View view, float cap) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            return;
        }
        float f = factor(view.getContext(), cap);
        if (f == 1f) {
            return;
        }
        if (lp.width > 0) {
            lp.width = Math.round(lp.width * f);
        }
        if (lp.height > 0) {
            lp.height = Math.round(lp.height * f);
        }
        view.setLayoutParams(lp);
    }

    /**
     * 通用列表项缩放：遍历 itemView，将固定高度的容器（行）按 {@link #ROW} 放大，
     * 将方形固定尺寸的 ImageView（头像/图标）按 {@link #CAP} 放大。
     * <p>
     * 适用于 id 不统一、无法逐一指定控件的列表项（如联系人各类 header、组织结构、收藏的群等）。
     */
    public static void scaleListItem(View itemView) {
        if (itemView == null || factor(itemView.getContext(), ROW) == 1f) {
            return;
        }
        scaleListItemInner(itemView);
    }

    private static void scaleListItemInner(View view) {
        if (view instanceof ImageView) {
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && lp.width > 0 && lp.height > 0) {
                scaleViewSize(view, CAP);
            }
            return;
        }
        if (view instanceof ViewGroup) {
            // 仅当其高度为固定正值时才会真正缩放（行容器），wrap/match 会被忽略
            scaleViewHeight(view, ROW);
            // 如果 ViewGroup 具有固定的正数宽度，需同步按 CAP 比例放大，防止子 ImageView 溢出裁剪
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && lp.width > 0) {
                lp.width = Math.round(lp.width * factor(view.getContext(), CAP));
                view.setLayoutParams(lp);
            }
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scaleListItemInner(group.getChildAt(i));
            }
        }
    }

    /**
     * 遍历视图树，将包含开关（Switch / SwitchCompat / SwitchMaterial）的固定高度行按 {@link #ROW} 放大。
     * <p>
     * 设置类页面里的开关项多为固定高度的静态布局（无统一 ViewHolder），字体放大后会被裁剪，
     * 故在页面布局完成后统一处理。仅识别 Switch 类控件，避免误伤列表项里的 CheckBox 等。
     */
    public static void scaleSwitchRows(View root) {
        if (root == null || factor(root.getContext(), ROW) == 1f) {
            return;
        }
        scaleSwitchRowsInner(root);
    }

    private static void scaleSwitchRowsInner(View view) {
        if (view instanceof android.widget.Switch
            || view instanceof androidx.appcompat.widget.SwitchCompat) {
            // 放大开关所在的、具有固定高度的最近祖先（即该设置项的行容器）
            ViewGroup parent = (view.getParent() instanceof ViewGroup) ? (ViewGroup) view.getParent() : null;
            while (parent != null) {
                ViewGroup.LayoutParams lp = parent.getLayoutParams();
                if (lp != null && lp.height > 0) {
                    scaleViewHeight(parent, ROW);
                    break;
                }
                parent = (parent.getParent() instanceof ViewGroup) ? (ViewGroup) parent.getParent() : null;
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                scaleSwitchRowsInner(group.getChildAt(i));
            }
        }
    }

    /**
     * 按 cap 缩放 View 的固定高度（行高），宽度保持不变。
     */
    public static void scaleViewHeight(View view, float cap) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp == null) {
            return;
        }
        float f = factor(view.getContext(), cap);
        if (f == 1f) {
            return;
        }
        if (lp.height > 0) {
            lp.height = Math.round(lp.height * f);
        }
        view.setLayoutParams(lp);
    }
}
