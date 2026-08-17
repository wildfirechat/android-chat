/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.page.WfcPageNavigator;
import cn.wildfire.chat.kit.utils.WfcDeviceUtils;

/**
 * 全仓库所有「打开会话」入口的统一出口。
 * <p>
 * 改造前每个入口都是 {@code startActivity(new Intent(ctx, ConversationActivity.class))}，
 * 在平板双栏下会在双栏之上再压一个全屏会话页。本类把这个决策收口到一处：
 * <ol>
 * <li>当前 Context 就是双栏主界面 → 直接换右栏；</li>
 * <li>处于双栏、但当前是别的独立页（群信息、搜索、收藏……） → 回到双栏主界面并在右栏打开；</li>
 * <li>其余情况 → {@code context.startActivity(conversationIntent)}，与改造前逐字节一致。</li>
 * </ol>
 * <p>
 * <strong>手机端保证</strong>：{@link #twoPaneHostActivity} 默认为 {@code null}，
 * 且 {@code R.bool.wfc_two_pane} 无限定符取值为 {@code false}，两个条件任一不满足都直接走第 3 条。
 * 手机端两个条件都不满足。
 * <p>
 * <strong>对 AAR 集成方的保证</strong>：第 2 条依赖 {@link #setTwoPaneHostActivity(Class)} 显式注册。
 * 集成方不注册（默认）时，即使运行在平板上也只会走第 3 条，行为与改造前完全一致 ——
 * 不能只判断屏幕宽度，否则集成方的平板用户会因为「路由到一个并不支持双栏的主界面」而打不开会话。
 */
public class ConversationRouter {

    /**
     * 标记该 intent 是「回到主界面并在右栏打开会话」，而不是普通的启动主界面。
     */
    public static final String EXTRA_OPEN_CONVERSATION_IN_PANE = "wfcOpenConversationInPane";

    /**
     * 双栏主界面的 Activity 类。由 App 在启动时注册（demo 见 {@code MyApp.onCreate}）。
     * 注册本身与屏幕大小无关，是否真的走双栏由 {@link WfcDeviceUtils#isTwoPaneLayout(Context)} 决定。
     */
    private static Class<? extends Activity> twoPaneHostActivity;

    private ConversationRouter() {
    }

    public static void setTwoPaneHostActivity(Class<? extends Activity> activityClass) {
        twoPaneHostActivity = activityClass;
    }

    /**
     * 打开会话。{@code conversationIntent} 用 {@link ConversationActivity} 的
     * {@code buildConversationIntent} 系列方法构造，或直接 new，extras 保持不变。
     */
    public static void open(Context context, Intent conversationIntent) {
        open(null, context, conversationIntent);
    }

    /**
     * 由 Fragment 发起的「打开会话」。优先用这个重载：双栏下右栏要靠发起者的位置来决定
     * 是压栈（在资料页点「发消息」，返回应能回到资料页）还是换内容（点列表另一项）。
     */
    public static void open(Fragment fragment, Intent conversationIntent) {
        if (fragment == null) {
            return;
        }
        open(fragment, fragment.getContext(), conversationIntent);
    }

    private static void open(@Nullable Fragment caller, Context context, Intent conversationIntent) {
        if (context == null || conversationIntent == null) {
            return;
        }
        // 必须先判双栏再找导航器：双栏主界面在手机上依然是同一个 Activity 类、依然实现
        // WfcPageNavigator，只是没有右栏。少了这个判断，手机端点会话会调到一个空实现上，
        // 表现为「点了没反应」。
        if (WfcDeviceUtils.isTwoPaneLayout(context)) {
            WfcPageNavigator paneNavigator = findPaneNavigator(context);
            if (paneNavigator != null) {
                if (paneNavigator.openPageInPane(caller, conversationIntent, -1)) {
                    return;
                }
                // 兜底：主界面实现了导航接口但当前没有右栏（如窄窗口），退回独立会话页。
                // 与 MainActivity.showConversationInPane 的兜底保持一致。
                Intent fallbackIntent = new Intent(context, ConversationActivity.class);
                fallbackIntent.putExtras(conversationIntent);
                context.startActivity(fallbackIntent);
                return;
            }
            Intent twoPaneIntent = buildTwoPaneIntent(context, conversationIntent);
            if (twoPaneIntent != null) {
                context.startActivity(twoPaneIntent);
                return;
            }
        }
        // 手机端只会走到这里，与改造前的 context.startActivity(conversationIntent) 完全一致
        context.startActivity(conversationIntent);
    }

    /**
     * 供通知点击等需要「先回主界面、再打开会话」的场景使用（这些场景 App 可能尚未启动，
     * 拿不到 Activity，只能靠 {@code PendingIntent.getActivities} 起一串 Activity）。
     *
     * @return 双栏下是长度为 1 的数组（只起主界面，会话参数放在 extras 里）；
     * 其余情况原样返回 {@code {mainIntent, conversationIntent}}。
     */
    public static Intent[] buildTaskIntents(Context context, Intent mainIntent, Intent conversationIntent) {
        Intent twoPaneIntent = buildTwoPaneIntent(context, conversationIntent);
        if (twoPaneIntent != null) {
            return new Intent[]{twoPaneIntent};
        }
        return new Intent[]{mainIntent, conversationIntent};
    }

    /**
     * @return 双栏且已注册双栏主界面时，返回一个「打开主界面并在右栏显示该会话」的 intent；否则返回 null。
     */
    private static Intent buildTwoPaneIntent(Context context, Intent conversationIntent) {
        if (twoPaneHostActivity == null || context == null || conversationIntent == null) {
            return null;
        }
        if (!WfcDeviceUtils.isTwoPaneLayout(context)) {
            return null;
        }
        Intent intent = new Intent(context, twoPaneHostActivity);
        Bundle extras = conversationIntent.getExtras();
        if (extras != null) {
            // 透传全部会话参数，键名与 ConversationActivity 一致，新增 extra 时无需改这里
            intent.putExtras(extras);
        }
        intent.putExtra(EXTRA_OPEN_CONVERSATION_IN_PANE, true);
        if (!(unwrapActivity(context) instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        // 主界面是 singleTask：已存在的实例会收到 onNewIntent，其上的 Activity 会被自动清掉，
        // 所以从群信息页、搜索页点进会话后不会留下一层旧页面。
        return intent;
    }

    /**
     * 沿 ContextWrapper 链向上找双栏导航器。Fragment 的 {@code getContext()} 可能是
     * ContextThemeWrapper（对话框、带主题的 Fragment），必须解包才能拿到 Activity。
     */
    private static WfcPageNavigator findPaneNavigator(Context context) {
        Context ctx = context;
        while (ctx != null) {
            if (ctx instanceof WfcPageNavigator) {
                return (WfcPageNavigator) ctx;
            }
            if (!(ctx instanceof ContextWrapper)) {
                return null;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return null;
    }

    private static Context unwrapActivity(Context context) {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                return ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return ctx;
    }
}
