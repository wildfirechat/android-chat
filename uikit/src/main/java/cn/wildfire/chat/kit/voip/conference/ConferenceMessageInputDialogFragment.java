/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Dialog;

import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceMessageInputDialogFragment extends KeyboardDialogFragment {
    @Override
    public void sendMessage(String message) {
        ConferenceInfo conferenceInfo = ConferenceManager.getManager().getCurrentConferenceInfo();
        if (conferenceInfo == null) {
            return;
        }
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, conferenceInfo.getConferenceId(), 0);
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
