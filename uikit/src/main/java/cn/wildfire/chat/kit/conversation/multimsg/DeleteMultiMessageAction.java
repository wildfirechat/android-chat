/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class DeleteMultiMessageAction extends MultiMessageAction {

    @Override
    public void onClick(List<UiMessage> messages) {
        MessageViewModel messageViewModel = new ViewModelProvider(fragment).get(MessageViewModel.class);

        List<String> items = new ArrayList<>();
        items.add("删除本地消息");
        boolean isSuperGroup = false;
        UiMessage message = messages.get(0);
        if (message.message.conversation.type == Conversation.ConversationType.Group) {
            GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(message.message.conversation.target, false);
            if (groupInfo != null && groupInfo.superGroup == 1) {
                isSuperGroup = true;
            }
        }
        if ((message.message.conversation.type == Conversation.ConversationType.Group && !isSuperGroup)
            || message.message.conversation.type == Conversation.ConversationType.Single
            || message.message.conversation.type == Conversation.ConversationType.Channel) {
            items.add("删除远程消息");
        } else if (message.message.conversation.type == Conversation.ConversationType.SecretChat) {
            items.add("删除自己及对方消息");
        }

        new MaterialDialog.Builder(fragment.getContext())
            .items(items)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    if (position == 0) {
                        for (UiMessage message : messages) {
                            messageViewModel.deleteMessage(message.message);
                        }
                    } else {
                        for (UiMessage message : messages) {
                            messageViewModel.deleteRemoteMessage(message.message);
                        }
                    }
                }
            })
            .show();
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_delete;
    }

    @Override
    public String title(Context context) {
        return "删除";
    }

    @Override
    public boolean confirm() {
        return true;
    }

    @Override
    public String confirmPrompt() {
        return "确认删除?";
    }
}
