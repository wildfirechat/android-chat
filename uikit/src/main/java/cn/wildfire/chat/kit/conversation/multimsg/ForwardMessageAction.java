/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.multimsg;

import android.content.Context;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;
import java.util.stream.Collectors;

import cn.wildfire.chat.kit.R;
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
            .items("逐条转发", "合并转发")
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
        Toast.makeText(fragment.getActivity(), "逐条转发", Toast.LENGTH_SHORT).show();
    }

    private void forward(List<UiMessage> messages) {
        Toast.makeText(fragment.getActivity(), "合并转发", Toast.LENGTH_SHORT).show();
        CompositeMessageContent content = new CompositeMessageContent();
        String title;
        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo1 = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
            UserInfo userInfo2 = ChatManager.Instance().getUserInfo(conversation.target, false);
            title = userInfo1.displayName + "和" + userInfo2.displayName + "的聊天记录";
        } else {
            title = "群的聊天记录";
        }
        content.setTitle(title);
        List<Message> msgs = messages.stream().map(uiMessage -> uiMessage.message).collect(Collectors.toList());
        content.setMessages(msgs);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_forward;
    }

    @Override
    public String title(Context context) {
        return "转发";
    }

    @Override
    public boolean filter(Conversation conversation) {
        return false;
    }
}
