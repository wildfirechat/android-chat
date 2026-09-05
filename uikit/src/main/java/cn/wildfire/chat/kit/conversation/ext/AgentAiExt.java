/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.agent.AgentAiSettingsDialog;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.utils.AgentState;

import cn.wildfirechat.model.ClientState;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.remote.ChatManager;

/**
 * AI 会话设置扩展
 * <p>
 * 在输入栏 "+" 扩展面板添加「AI 会话设置」入口，仅 Agent/AI 会话（群聊 + line==2）显示。
 * 多机器人（多 agent）会话：入口先弹出机器人列表（列表项显示机器人名），选中后把目标
 * 机器人 uid 绑定到面板；单机器人/尚无状态推送的会话直接打开默认面板。
 * 面板：模型 / 推理等级 / 工作目录 / 沙箱模式 / 计划模式。
 * 静默通道：打开发 207 Agent_Command（op=query，带目标 robotId）组合查询，
 * 读 scope=31 type=3 面板数据渲染（模型/effort 下拉、沙箱单选、计划开关、cwd + dirs
 * 目录选择）；所有操作发 207 Agent_Command（op=set，cmd=命令文本，如
 * "/model deepseek-official/xxx"，带目标 robotId），不再发送 /model /effort /cwd /sandbox
 * /plan /compact /reset 文本命令、不解析回复。207 为透明消息，不落消息流。
 * </p>
 *
 * @author WildFireChat
 * @since 2026
 */
public class AgentAiExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation  当前会话
     */
    @ExtContextMenuItem
    public void openAgentAiSettings(View containerView, Conversation conversation) {
        if (activity == null || activity.isFinishing() || messageViewModel == null) {
            return;
        }
        List<String> robotUids = AgentState.listAgentRobotUids(conversation);
        if (robotUids.size() > 1) {
            // 多机器人：先列表选择目标机器人，再把 robotUid 绑定到面板
            showRobotChooser(conversation, robotUids);
            return;
        }
        String robotUid = robotUids.isEmpty() ? null : robotUids.get(0);
        new AgentAiSettingsDialog(activity, conversation, messageViewModel, robotUid).show();
    }

    /**
     * 多机器人选择对话框：列表项显示机器人显示名（用户信息缺失回退完整 uid，
     * 见 {@link AgentState#agentRobotName}）；选中后打开绑定该 uid 的面板。
     */
    private void showRobotChooser(Conversation conversation, List<String> robotUids) {
        String[] names = new String[robotUids.size()];
        for (int i = 0; i < robotUids.size(); i++) {
            names[i] = AgentState.agentRobotName(robotUids.get(i));
        }
        new MaterialDialog.Builder(activity)
            .title(R.string.agent_ai_choose_robot)
            .items(names)
            .itemsCallback((dialog, itemView, position, text) -> {
                String robotUid = (position >= 0 && position < robotUids.size()) ? robotUids.get(position) : null;
                new AgentAiSettingsDialog(activity, conversation, messageViewModel, robotUid).show();
            })
            .negativeText(R.string.cancel)
            .show();
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public int iconResId() {
        return R.drawable.ic_ext_agent_ai;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.agent_ai_settings);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }

    /**
     * 过滤条件：仅 Agent/AI 会话（群聊 + line==2）显示，其余会话不显示。
     *
     * @param conversation 会话
     * @return true=不显示
     */
    @Override
    public boolean filter(Conversation conversation) {
        return !AgentState.isAgentConversation(conversation);
    }

    /**
     * AI 不在线（AI 群群主无 clientStates 中 state==0 的在线客户端）时置灰禁用。
     * 判定与 ConversationFragment 的 aiOwnerOnline() 一致。
     */
    @Override
    public boolean disabled(Conversation conversation) {
        if (!AgentState.isAgentConversation(conversation)) {
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
