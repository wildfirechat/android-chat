/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 「我能在双栏右栏里换一个会话显示」。只有平板双栏主界面会实现它，手机端没有任何实现类。
 * <p>
 * 与 {@link ConversationHost} 的区别：{@code ConversationHost} 是<strong>会话页对宿主的要求</strong>
 * （设标题、关闭自己），独立会话页和双栏右栏都要实现；本接口是<strong>路由对宿主的要求</strong>
 * （换一个会话），只有双栏主界面才有意义。故意分成两个接口，避免让独立会话页去实现一个
 * 它无法履行的「换会话」语义。
 *
 * @see ConversationRouter
 */
public interface ConversationPaneHost {

    /**
     * 在右栏打开 intent 指定的会话。
     *
     * @param conversationIntent 与 {@link ConversationActivity} 完全相同的 intent，
     *                           extras 的键名、含义、默认值都一致，因此新增 extra 时两条路径自动同步。
     */
    void showConversationInPane(Intent conversationIntent);

    /**
     * 同上，但带上发起跳转的 Fragment。
     * <p>
     * 右栏据此判断是「压到发起者所在的那条栈」（在用户资料页点「发消息」——返回应能回到资料页）
     * 还是「换当前 tab 的内容」（点会话列表的另一项）。拿不到 Fragment 时传 null，
     * 语义退化为后者，与单参数版本一致。
     */
    void showConversationInPane(@Nullable Fragment caller, Intent conversationIntent);
}
