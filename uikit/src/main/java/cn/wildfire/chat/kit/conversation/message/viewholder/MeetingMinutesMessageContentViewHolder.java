/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.MeetingMinutesMessageContent;

@MessageContentType(MeetingMinutesMessageContent.class)
@EnableContextMenu
public class MeetingMinutesMessageContentViewHolder extends NormalMessageContentViewHolder {
    TextView contentTextView;

    public MeetingMinutesMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentTextView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        contentTextView = itemView.findViewById(R.id.contentTextView);
    }

    @Override
    public void onBind(UiMessage message) {
        MeetingMinutesMessageContent content = (MeetingMinutesMessageContent) message.message.content;
        String display = content.getTitle();
        if (TextUtils.isEmpty(display)) {
            display = content.getText();
        }
        contentTextView.setText(display);
    }

    public void onClick(View view) {
        MeetingMinutesMessageContent content = (MeetingMinutesMessageContent) message.message.content;
        if (!TextUtils.isEmpty(Config.AI_MINUTES_ROBOT_ID)
            && Config.AI_MINUTES_ROBOT_ID.equals(message.message.conversation.target)
            && !TextUtils.isEmpty(content.getMeetingId())) {
            String encodedConferenceId = Uri.encode(content.getMeetingId());
            String url = Config.getMinutesUrl() + "?conferenceId=" + encodedConferenceId;
            WfcWebViewActivity.loadUrl(fragment.getContext(), "", url);
        }
    }
}
