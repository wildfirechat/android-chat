/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.page;

import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * 「一个页面的宿主」的契约：把所有<strong>以页面为单位</strong>的动作收敛到这里。
 * <p>
 * 两个实现：
 * <ul>
 *   <li>{@code WfcBaseActivity} —— 手机端，一个页面就是一个 Activity，这些动作全部是
 *       Activity 自己的方法（{@code setTitle} / {@code invalidateOptionsMenu} /
 *       {@code setResult} / {@code finish}）；</li>
 *   <li>{@code PanePageFragment} —— 平板右栏，一个页面是导航栈上的一层，这些动作对应
 *       「写本层 toolbar」「重刷本层菜单」「把结果交给压栈的那一方」「把本层弹掉」。</li>
 * </ul>
 * <p>
 * 页面 Fragment 不应该直接 {@code getActivity().finish()} / {@code getActivity().setResult(...)}
 * —— 在右栏里那会结束整个双栏主界面。一律通过 {@link WfcPageCompat} 拿到本接口再调用。
 */
public interface WfcPageHost {

    /**
     * 设置页面标题。
     */
    void setPageTitle(CharSequence title);

    /**
     * 设置副标题，null 或空串表示不显示。
     */
    void setPageSubtitle(@Nullable CharSequence subtitle);

    /**
     * 当前标题文本。
     */
    @Nullable
    CharSequence getPageTitle();

    /**
     * 重新计算并刷新菜单，等价于 {@code invalidateOptionsMenu()}。
     * 会重新走一遍 {@link WfcPage#onPreparePageMenu}。
     */
    void invalidatePageMenu();

    /**
     * 关闭本页面。手机端 = {@code finish()}；右栏 = 把本页连同压在本页上面的一起弹掉。
     */
    void finishPage();

    /**
     * 回传结果给打开本页的那一方。手机端 = {@code setResult(...)}；
     * 右栏 = 记下来，本页出栈时投递给发起跳转的那个 Fragment 的 {@code onActivityResult}。
     * <p>
     * 与 {@code Activity.setResult} 一样，只是记录，真正的投递发生在 {@link #finishPage()} 之后。
     */
    void setPageResult(int resultCode, @Nullable Intent data);

    /**
     * 本宿主是不是平板右栏的一层。
     * <p>
     * 页面代码应尽量不依赖它 —— 需要区分的地方通常说明还有一个动作没有抽进本接口。
     * 目前唯一的正当用法是「打开下一个页面后顺手关掉自己」这种收尾：手机端必须关
     * （否则返回会退回本页），右栏不能关（右栏本来就有返回栈，下面那层留着才对）。
     */
    boolean isPaneHost();
}
