/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Dialog;

import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceMessageInputDialogFragment extends KeyboardDialogFragment {
    @Override
    public void sendMessage(String message) {
        // TODO 会话不对
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, "EPhwEwgg", 0);
        ChatManager.Instance().sendMessage(conversation, new TextMessageContent(message), null, 0, null);

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
