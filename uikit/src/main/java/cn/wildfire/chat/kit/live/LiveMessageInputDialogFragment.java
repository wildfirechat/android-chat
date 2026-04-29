/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;

import cn.wildfire.chat.kit.voip.conference.KeyboardDialogFragment;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播聊天输入框 — 键盘感知 DialogFragment（与会议输入体验一致）
 */
public class LiveMessageInputDialogFragment extends KeyboardDialogFragment {

    private static final String ARG_CALL_ID = "callId";

    public static LiveMessageInputDialogFragment newInstance(String callId) {
        LiveMessageInputDialogFragment f = new LiveMessageInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, callId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make the full-screen dialog window transparent so the live video is visible behind
        // the keyboard input panel (only the input bar at the bottom is opaque).
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void sendMessage(String message) {
        String callId = getArguments() != null ? getArguments().getString(ARG_CALL_ID) : null;
        if (!TextUtils.isEmpty(message) && callId != null) {
            Conversation conv = new Conversation(Conversation.ConversationType.ChatRoom, callId, 0);
            ChatManager.Instance().sendMessage(conv, new TextMessageContent(message), null, 0, null);
        }
        hideKeyboard(null);
    }

    @Override
    public void onKeyboardHidden() {
        super.onKeyboardHidden();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
