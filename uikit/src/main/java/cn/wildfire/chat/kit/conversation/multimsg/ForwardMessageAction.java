/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardViewModel;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ForwardMessageAction extends MultiMessageAction {
  @Override
    public void onClick(List<UiMessage> messages) {
        new MaterialDialog.Builder(fragment.getActivity())
            .items(new String[]{
                fragment.getString(R.string.forward_one_by_one),
                fragment.getString(R.string.forward_merge)
            })
            .itemsCallback((dialog, itemView, position, text) -> {
                switch (position) {
                    case 0:
                        forwardOneByOne(messages);
                        break;
                    case 1:
                        forward(messages);
                        break;
                    default:
                        break;
                }
            })
            .build()
            .show();
    }

    private void forwardOneByOne(List<UiMessage> messages) {
        ArrayList<Message> msgs = new ArrayList<>();
        for (UiMessage uiMessage : messages) {
            Message message = uiMessage.message;
            msgs.add(message);
        }
        Intent intent = new Intent(fragment.getContext(), ForwardActivity.class);
        intent.putExtra("messages", msgs);
        fragment.startActivity(intent);
    }

    private void forward(List<UiMessage> messages) {
        Toast.makeText(fragment.getActivity(), R.string.forward_merge, Toast.LENGTH_SHORT).show();
        CompositeMessageContent content = new CompositeMessageContent();
        String title = fragment.getString(R.string.forward_chat_record_title);
        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo1 = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
            UserInfo userInfo2 = ChatManager.Instance().getUserInfo(conversation.target, false);
            title = fragment.getString(R.string.forward_chat_with, userInfo1.displayName, userInfo2.displayName);
        } else if (conversation.type == Conversation.ConversationType.Group) {
            title = fragment.getString(R.string.forward_group_chat);
        }
        content.setTitle(title);
        ForwardViewModel forwardViewModel = new ViewModelProvider(fragment).get(ForwardViewModel.class);
        List<Message> msgs = new ArrayList<>();
        for (UiMessage uiMessage : messages) {
            Message message1 = uiMessage.message;
            message1.content = forwardViewModel.filterMessageContent(message1);
            msgs.add(message1);
        }
        content.setMessages(msgs);
        Message message = new Message();
        message.content = content;

        Intent intent = new Intent(fragment.getContext(), ForwardActivity.class);
        intent.putExtra("message", message);
        fragment.startActivity(intent);
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_forward;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.message_action_forward);
    }

    @Override
    public boolean filter(Conversation conversation) {
        return false;
    }
}
