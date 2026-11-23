/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * 主界面底部导航。
 * <p>
 * Material 的 {@link BottomNavigationView} 在 XML 里一次性 inflate 超过 5 个菜单项时会直接抛
 * {@code IllegalArgumentException}（{@code NavigationBarMenu#addInternal} 按
 * {@link #getMaxItemCount()} 校验）。主界面现在有「消息 / 办公 / 通讯录 / 工作台 / 发现 / 我的」
 * 六个 tab，工作台开启时必然超过 5 个。
 * <p>
 * {@code getMaxItemCount()} 在 {@code NavigationBarView} 构造 {@code NavigationBarMenu} 时被调用，
 * 这里放大上限即可让 6 个（以及以后可能更多）tab 正常 inflate；布局、测量、图标 tint、
 * 角标、选中回调仍全部由 Material 原实现负责，不需要自己重写底部导航。
 */
public class MainBottomNavigationView extends BottomNavigationView {

    /**
     * 允许的最大 tab 数。当前最多 6 个（消息、办公、通讯录、工作台、发现、我的），
     * 稍微放大一点，后续再加 tab 时不用再改这里。
     */
    private static final int MAX_ITEM_COUNT = 8;

    public MainBottomNavigationView(@NonNull Context context) {
        super(context);
    }

    public MainBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MainBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public int getMaxItemCount() {
        return MAX_ITEM_COUNT;
    }
}
