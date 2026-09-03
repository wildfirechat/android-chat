/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.dsh.AgentApprovalMessageContent;
import cn.wildfirechat.message.dsh.AgentApprovalResultMessageContent;

/**
 * DSH 工具审批卡片（202）。
 * <p>
 * 「同意」（主色）/「拒绝」（红）并排大按钮；点击发送 DSH_ApprovalResult 并立即本地置灰；
 * 锁定态（approved/rejected/expired 或本地已决）显示状态文本。
 * </p>
 */
@MessageContentType(AgentApprovalMessageContent.class)
@EnableContextMenu
public class DshApprovalMessageContentViewHolder extends NormalMessageContentViewHolder {
    // 本地已决策的消息 id：点击后立即置灰，不依赖服务端 updateMessage 推送的实时性。
    // 重新进入会话时以 content.state 为准（服务端会 updateMessage）。
    private static final Set<Long> locallyDecidedMessageIds = new HashSet<>();

    TextView toolNameTextView;
    TextView reasonTextView;
    View actionsLinearLayout;
    TextView approveButton;
    TextView rejectButton;
    TextView stateTextView;

    private AgentApprovalMessageContent approvalContent;
    private String decidedAction;

    public DshApprovalMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindViews(View itemView) {
        toolNameTextView = itemView.findViewById(R.id.dshToolNameTextView);
        reasonTextView = itemView.findViewById(R.id.dshReasonTextView);
        actionsLinearLayout = itemView.findViewById(R.id.dshActionsLinearLayout);
        approveButton = itemView.findViewById(R.id.dshApproveButton);
        rejectButton = itemView.findViewById(R.id.dshRejectButton);
        stateTextView = itemView.findViewById(R.id.dshStateTextView);
    }

    private void bindEvents(View itemView) {
        approveButton.setOnClickListener(v -> decide(AgentApprovalResultMessageContent.ACTION_APPROVE));
        rejectButton.setOnClickListener(v -> decide(AgentApprovalResultMessageContent.ACTION_REJECT));
    }

    @Override
    protected void onBind(UiMessage message) {
        approvalContent = (AgentApprovalMessageContent) message.message.content;

        toolNameTextView.setTypeface(Typeface.MONOSPACE);
        toolNameTextView.setText(approvalContent.getToolName());

        String reason = approvalContent.getReason();
        if (reason != null && !reason.isEmpty()) {
            reasonTextView.setVisibility(View.VISIBLE);
            reasonTextView.setText("原因：" + reason);
        } else {
            reasonTextView.setVisibility(View.GONE);
        }

        if (isLocked()) {
            actionsLinearLayout.setVisibility(View.GONE);
            stateTextView.setVisibility(View.VISIBLE);
            stateTextView.setText(stateText());
        } else {
            actionsLinearLayout.setVisibility(View.VISIBLE);
            stateTextView.setVisibility(View.GONE);
        }
    }

    private boolean isLocked() {
        String state = approvalContent.getState();
        return locallyDecidedMessageIds.contains(message.message.messageId)
            || "approved".equals(state) || "rejected".equals(state) || "expired".equals(state);
    }

    private String stateText() {
        String state = approvalContent.getState();
        if (locallyDecidedMessageIds.contains(message.message.messageId)) {
            return AgentApprovalResultMessageContent.ACTION_APPROVE.equals(decidedAction) ? "已同意" : "已拒绝";
        }
        if ("approved".equals(state)) {
            return "已同意";
        }
        if ("rejected".equals(state)) {
            return "已拒绝";
        }
        return "已过期";
    }

    private void decide(String action) {
        if (isLocked()) {
            return;
        }
        AgentApprovalResultMessageContent content = new AgentApprovalResultMessageContent(approvalContent.getAid(), action);
        messageViewModel.sendMessage(message.message.conversation, content);
        locallyDecidedMessageIds.add(message.message.messageId);
        decidedAction = action;
        onBind(message);
    }
}
