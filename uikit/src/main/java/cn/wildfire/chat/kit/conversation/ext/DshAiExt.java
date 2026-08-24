/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.content.Context;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.dsh.DshAiSettingsDialog;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.utils.DshState;
import android.text.TextUtils;

import cn.wildfirechat.model.ClientState;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.remote.ChatManager;

/**
 * AI 会话设置扩展
 * <p>
 * 在输入栏 "+" 扩展面板添加「AI 会话设置」入口，仅 DSH/AI 会话（群聊 + line==2）显示。
 * 点击弹出设置面板：模型 / 推理等级 / 工作目录 / 沙箱模式 / 计划模式。
 * 静默通道：打开发 207 DSH_Command（op=query）组合查询，读 scope=31 type=3 面板数据
 * 渲染（模型/effort 下拉、沙箱单选、计划开关、cwd + dirs 目录选择）；所有操作发
 * 207 DSH_Command（op=set，cmd=命令文本，如 "/model deepseek-official/xxx"），
 * 不再发送 /model /effort /cwd /sandbox /plan /compact /reset 文本命令、不解析回复。
 * 207 为透明消息，不落消息流。
 * </p>
 *
 * @author WildFireChat
 * @since 2026
 */
public class DshAiExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation  当前会话
     */
    @ExtContextMenuItem
    public void openDshAiSettings(View containerView, Conversation conversation) {
        if (activity == null || activity.isFinishing() || messageViewModel == null) {
            return;
        }
        new DshAiSettingsDialog(activity, conversation, messageViewModel).show();
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public int iconResId() {
        return R.drawable.ic_ext_dsh_ai;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.dsh_ai_settings);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }

    /**
     * 过滤条件：仅 DSH/AI 会话（群聊 + line==2）显示，其余会话不显示。
     *
     * @param conversation 会话
     * @return true=不显示
     */
    @Override
    public boolean filter(Conversation conversation) {
        return !DshState.isDshConversation(conversation);
    }

    /**
     * AI 不在线（AI 群群主无 clientStates 中 state==0 的在线客户端）时置灰禁用。
     * 判定与 ConversationFragment 的 aiOwnerOnline() 一致。
     */
    @Override
    public boolean disabled(Conversation conversation) {
        if (!DshState.isDshConversation(conversation)) {
            return false;
        }
        ChatManager chatManager = ChatManager.Instance();
        if (!chatManager.isEnableUserOnlineState()) {
            return false;
        }
        GroupInfo groupInfo = chatManager.getGroupInfo(conversation.target, false);
        if (groupInfo == null || TextUtils.isEmpty(groupInfo.owner)) {
            return false;
        }
        UserOnlineState ownerState = chatManager.getUserOnlineStateMap().get(groupInfo.owner);
        if (ownerState == null) {
            return true;
        }
        ClientState[] states = ownerState.getClientStates();
        if (states == null || states.length == 0) {
            return true;
        }
        for (ClientState cs : states) {
            if (cs.getPlatform() >= 1 && cs.getPlatform() <= 9 && cs.getState() == 0) {
                return false;
            }
        }
        return true;
    }
}
