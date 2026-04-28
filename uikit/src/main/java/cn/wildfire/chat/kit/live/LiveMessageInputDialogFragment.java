/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播聊天输入框 DialogFragment
 */
public class LiveMessageInputDialogFragment extends DialogFragment {

    private static final String ARG_CALL_ID = "callId";

    public static LiveMessageInputDialogFragment newInstance(String callId) {
        LiveMessageInputDialogFragment f = new LiveMessageInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, callId);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String callId = getArguments() != null ? getArguments().getString(ARG_CALL_ID) : null;

        EditText editText = new EditText(requireContext());
        editText.setHint(R.string.live_chat_input_hint);
        editText.setSingleLine(true);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        editText.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle(R.string.live_chat_send_title)
            .setView(editText)
            .setPositiveButton(R.string.send, (d, w) -> {
                String text = editText.getText().toString().trim();
                if (!TextUtils.isEmpty(text) && callId != null) {
                    Conversation conv = new Conversation(Conversation.ConversationType.ChatRoom, callId, 0);
                    ChatManager.Instance().sendMessage(conv, new TextMessageContent(text), null, 0, null);
                }
            })
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        // Show keyboard automatically
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        return dialog;
    }
}
