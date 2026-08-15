/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.page;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelStoreOwner;

/**
 * 页面 Fragment 找到自己宿主的入口。
 * <p>
 * 同一个 Fragment 手机端装在自己的 Activity 里、平板右栏里装在 {@code PanePageFragment} 里，
 * 所以「宿主」不一定是 {@code getActivity()}：先沿父 Fragment 链往上找，找不到再看 Activity。
 * 手机端父链上不存在 {@link WfcPageHost}，一定落到 Activity，与改造前逐字节等价。
 */
public final class WfcPageCompat {

    private WfcPageCompat() {
    }

    /**
     * 本 Fragment 所在页面的宿主，找不到返回 null（Fragment 已 detach 时）。
     */
    @Nullable
    public static WfcPageHost hostOf(@Nullable Fragment fragment) {
        if (fragment == null) {
            return null;
        }
        for (Fragment parent = fragment.getParentFragment(); parent != null; parent = parent.getParentFragment()) {
            if (parent instanceof WfcPageHost) {
                return (WfcPageHost) parent;
            }
        }
        Activity activity = fragment.getActivity();
        return activity instanceof WfcPageHost ? (WfcPageHost) activity : null;
    }

    /**
     * 从一个页面里打开另一个页面 —— 平板右栏下压到<strong>本页所在的那条导航栈</strong>上，
     * 其余情况等价于 {@code fragment.startActivity(intent)}。
     * <p>
     * 这是页面之间导航的<strong>推荐写法</strong>。直接 {@code startActivity} 也能进右栏
     * （主界面会拦截），但那条路拿不到发起者，只能靠「上一次按下点落在哪一栏」去猜是压栈还是
     * 换内容；用本方法则是确定的。
     * <p>
     * 手机端 {@code getActivity()} 不是 {@link WfcPageNavigator}，逐字节等价于原来的
     * {@code startActivity(intent)}。
     */
    public static void startPage(Fragment from, Intent intent) {
        if (!openInPane(from, intent, -1)) {
            from.startActivity(intent);
        }
    }

    /**
     * 打开一个需要回传结果的页面（选人、选会话……）。
     * <p>
     * <strong>选择器要开在右栏就必须用本方法</strong>：{@code Fragment.startActivityForResult}
     * 在到达 Activity 之前，requestCode 已被 FragmentManager 换成内部生成的码，主界面拦下来
     * 也无法把结果送回调用方。本方法把原始的 {@code requestCode} 直接交给右栏，
     * 该页出栈时结果会投递回 {@code from.onActivityResult(requestCode, ...)}，
     * 与手机端的回调时机、参数完全一致。
     * <p>
     * 页面内部回传结果用 {@link #setPageResult} + {@link #finishPage}，
     * 与 {@code setResult} + {@code finish} 一一对应。
     */
    public static void startPageForResult(Fragment from, Intent intent, int requestCode) {
        if (!openInPane(from, intent, requestCode)) {
            from.startActivityForResult(intent, requestCode);
        }
    }

    /**
     * 「打开下一个页面，并把自己从导航栈里去掉」——手机端 {@code startActivity(next)} 紧跟
     * {@code finish()} 的那种收尾。用完即弃的页面（选人建群、验证码、引导流程）该这么走：
     * 返回时不该再回到一个已经完成使命的页面。
     * <p>
     * 与 {@link #finishAfterOpeningPage} 的区别：那个在右栏里是<strong>不做</strong>顶替的，
     * 发起页留在栈里（从会话返回到用户资料是合理的）。两种收尾按产品语义逐页选。
     * <p>
     * 返回 false 时调用方需按自己原来的方式打开页面并 finish —— 之所以不在这里兜底，
     * 是因为「打开」这一步各页面并不相同（打开会话要走 {@code ConversationRouter}）。
     *
     * @return true 表示已在右栏顶替完成，调用方到此为止
     */
    public static boolean replaceSelfWithPage(@Nullable Fragment from, @Nullable Intent intent) {
        if (from == null || intent == null) {
            return false;
        }
        Activity activity = from.getActivity();
        return activity instanceof WfcPageNavigator
            && ((WfcPageNavigator) activity).replacePageInPane(from, intent);
    }

    private static boolean openInPane(Fragment from, Intent intent, int requestCode) {
        if (from == null || intent == null) {
            return false;
        }
        Activity activity = from.getActivity();
        return activity instanceof WfcPageNavigator
            && ((WfcPageNavigator) activity).openPageInPane(from, intent, requestCode);
    }

    /**
     * 关闭当前页面，等价于改造前的 {@code getActivity().finish()}。
     * <p>
     * <strong>右栏里绝不能真的 finish 宿主 Activity</strong>——宿主是双栏主界面，
     * finish 掉就是整个界面退出。
     */
    public static void finishPage(@Nullable Fragment fragment) {
        WfcPageHost host = hostOf(fragment);
        if (host != null) {
            host.finishPage();
            return;
        }
        // 兜底：页面装在一个还没实现 WfcPageHost 的 Activity 里
        Activity activity = fragment == null ? null : fragment.getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    /**
     * 回传结果给打开本页的一方，等价于 {@code getActivity().setResult(...)}。
     * 通常紧跟一次 {@link #finishPage}，顺序与 Activity 版本一致。
     */
    public static void setPageResult(@Nullable Fragment fragment, int resultCode, @Nullable Intent data) {
        WfcPageHost host = hostOf(fragment);
        if (host != null) {
            host.setPageResult(resultCode, data);
            return;
        }
        Activity activity = fragment == null ? null : fragment.getActivity();
        if (activity != null) {
            activity.setResult(resultCode, data);
        }
    }

    /**
     * 「刚打开了下一个页面，顺手把自己关掉」这种收尾。
     * <p>
     * 手机端等价于 {@code getActivity().finish()}：不关掉的话，从新页面返回会先回到本页，
     * 与产品预期不符。<strong>右栏里则什么也不做</strong>——右栏本来就有返回栈，
     * 下面那一层留着才是对的（从会话返回到用户资料是合理的）。
     */
    public static void finishAfterOpeningPage(@Nullable Fragment fragment) {
        WfcPageHost host = hostOf(fragment);
        if (host == null || host.isPaneHost()) {
            return;
        }
        host.finishPage();
    }

    /**
     * 重算并刷新本页菜单，等价于 {@code invalidateOptionsMenu()}。
     */
    public static void invalidatePageMenu(@Nullable Fragment fragment) {
        WfcPageHost host = hostOf(fragment);
        if (host != null) {
            host.invalidatePageMenu();
        }
    }

    /**
     * 改写本页标题。页面标题通常由 {@link WfcPage#pageTitle()} 静态给出，
     * 本方法用于运行时才知道标题的页面（如加载完群资料后才有群名）。
     */
    public static void setPageTitle(@Nullable Fragment fragment, CharSequence title) {
        WfcPageHost host = hostOf(fragment);
        if (host != null) {
            host.setPageTitle(title);
        }
    }

    /**
     * 本 Fragment 是否正装在平板右栏里。判断依据见 {@link WfcPageHost#isPaneHost()}。
     */
    public static boolean isInPane(@Nullable Fragment fragment) {
        WfcPageHost host = hostOf(fragment);
        return host != null && host.isPaneHost();
    }

    /**
     * 「一个页面」这一层的 ViewModel 作用域。
     * <p>
     * 页面内多个 Fragment 要共享一个 ViewModel（选人页的 {@code PickUserViewModel} 被列表、
     * 搜索、已选栏共用）时，改造前一律写 {@code new ViewModelProvider(getActivity())} ——
     * 手机端「一个 Activity = 一个页面」，这没问题。
     * <p>
     * <strong>平板右栏里这么写是错的</strong>：Activity 是双栏主界面，五条栈上所有页面会共用
     * 同一个 ViewModel 实例，两个 tab 各开一个选人页就会互相串选中状态，且页面关掉后
     * ViewModel 不会清（Activity 还活着），下次打开还残留上次的勾选。
     * <p>
     * 本方法返回真正代表「当前页面」的作用域：手机端是 Activity，右栏是那一层
     * {@code PanePageFragment} —— 它随页面出栈而销毁，ViewModel 也随之 {@code onCleared}。
     */
    public static ViewModelStoreOwner pageScope(Fragment fragment) {
        WfcPageHost host = hostOf(fragment);
        if (host instanceof ViewModelStoreOwner) {
            return (ViewModelStoreOwner) host;
        }
        return fragment.requireActivity();
    }
}
