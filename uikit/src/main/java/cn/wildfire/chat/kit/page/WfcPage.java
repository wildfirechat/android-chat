/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.page;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.MenuRes;
import androidx.annotation.Nullable;

/**
 * 「一个页面」的内容契约，由 <strong>Fragment</strong> 实现。
 * <p>
 * 本仓库里一个页面有两种宿主形态：手机端是一个 {@code WfcBaseActivity}（壳布局
 * {@code fragment_container_activity} + 一个 Fragment），平板右栏里是一个
 * {@code PanePageFragment}（自带 toolbar + 同一个 Fragment）。改造前标题、菜单、返回键
 * 只写在 Activity 里，右栏没有 Activity，于是只能把同一段代码在右栏再表达一遍
 * —— 33 个带菜单的 Activity 都要搬一次，且两份实现会各自漂移
 * （{@code EmployeeInfoActivity} 与改造前的 {@code UserInfoActivity} 就是逐行重复的两份）。
 * <p>
 * 现在反过来：<strong>这些能力下沉到 Fragment 只写一份</strong>，两种宿主都通过本接口向它要。
 * 宿主侧的能力见 {@link WfcPageHost}。
 * <p>
 * <strong>全部是 default 方法</strong>：列表类页面什么都不用实现。老页面不实现本接口也照常工作
 * —— 宿主发现内容 Fragment 不是 {@code WfcPage} 时，行为与改造前逐行一致。
 */
public interface WfcPage {

    /**
     * 页面的菜单资源，0 表示没有菜单。等价于改造前 {@code WfcBaseActivity.menu()}。
     */
    @MenuRes
    default int pageMenu() {
        return 0;
    }

    /**
     * 菜单项的可见性/可用性计算，以及 SearchView 之类 actionView 的装配。
     * 等价于改造前 {@code WfcBaseActivity.afterMenus(Menu)}。
     * <p>
     * 状态变化后要重算，调用 {@link WfcPageCompat#invalidatePageMenu} —— 手机端等价于
     * {@code invalidateOptionsMenu()}，右栏等价于重新 inflate 本页 toolbar 的菜单。
     */
    default void onPreparePageMenu(Menu menu) {
    }

    /**
     * 菜单点击，返回 true 表示已消费。等价于 {@code Activity.onOptionsItemSelected}。
     */
    default boolean onPageMenuItemSelected(MenuItem item) {
        return false;
    }

    /**
     * 页面标题，null 表示交给宿主决定：手机端用 manifest 里该 Activity 的 {@code android:label}，
     * 右栏同样去读那个 label，因此两端标题必然一致。
     */
    @Nullable
    default CharSequence pageTitle() {
        return null;
    }

    /**
     * 本页自带标题栏，宿主不要再给一条。
     * <p>
     * 搜索类页面顶部是「搜索框 + 取消」而不是「标题 + 返回箭头」，手机端它们继承的是
     * {@code WfcBaseNoToolbarActivity}；右栏里对应的表达就是让 {@code PanePageFragment}
     * 把自己那条 toolbar 收起来，页面整块占满右栏。
     * <p>
     * 返回 true 的页面必须自己提供关闭入口（搜索页是「取消」按钮）——没有返回箭头了。
     * 系统返回键不受影响，仍然由宿主处理。
     */
    default boolean providesOwnToolbar() {
        return false;
    }

    /**
     * 返回键。返回 true 表示页面自己消费掉了（收起表情面板、退出多选），宿主不再关闭页面。
     */
    default boolean onPageBackPressed() {
        return false;
    }

    /**
     * 把启动 intent 交给页面。
     * <p>
     * 手机端页面从 {@code getIntent()} 或 {@code newInstance(...)} 的 arguments 里拿参数，
     * 因此<strong>只有右栏会调用本方法</strong>：右栏没有 Intent，且像会话页那样需要等到视图
     * 建好之后才能应用参数（{@code setupConversation} 依赖 adapter 与输入面板）。
     * 宿主保证在视图创建完成之后调用。
     */
    default void onPageIntent(Intent intent) {
    }

    /**
     * 本页已经在当前导航栈里，又被同一个身份再次打开，此时栈已退回到本页这一层。
     * 相当于 {@code launchMode=singleTop} 的 {@code onNewIntent}。
     * <p>
     * 默认什么也不做——「退回到原来那一层」通常就是全部诉求。<strong>不要无条件按新 intent
     * 重建页面</strong>，那会把草稿和滚动位置一起丢掉。
     */
    default void onNewPageIntent(Intent intent) {
    }
}
