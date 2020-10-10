/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;


@MessageContentType(value = {
    CompositeMessageContent.class,

})
@EnableContextMenu
public class CompositeMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.contentTextView)
    TextView contentTextView;

    public CompositeMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        CompositeMessageContent content = (CompositeMessageContent) message.message.content;
        titleTextView.setText(content.getTitle());
        List<Message> messages = content.getMessages();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < messages.size() && i < 4; i++) {
            Message msg = messages.get(i);
            String sender = msg.sender;
            UserInfo userInfo = ChatManager.Instance().getUserInfo(sender, false);
            sb.append(userInfo.displayName + ": " + msg.content.digest(msg));
            sb.append("\n");
        }
        contentTextView.setText(sb.toString());
    }

    @OnClick(R2.id.contentTextView)
    public void onClick(View view) {
        // TODO
    }

}
