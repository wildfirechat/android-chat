/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.voip.conference.ConferenceActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(value = {
    ConferenceInviteMessageContent.class,
})
public class ConferenceInviteMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.hostPortraitImageView)
    ImageView hostPortraitImageView;
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;

    private ConferenceInviteMessageContent inviteMessageContent;

    public ConferenceInviteMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        inviteMessageContent = (ConferenceInviteMessageContent) message.message.content;
        titleTextView.setText(inviteMessageContent.getTitle());
        descTextView.setText(inviteMessageContent.getDesc());
        UserInfo userInfo = ChatManager.Instance().getUserInfo(inviteMessageContent.getHost(), false);
        GlideApp
            .with(fragment)
            .load(userInfo.portrait)
            .transforms(new CenterCrop(), new RoundedCorners(10))
            .placeholder(R.mipmap.avatar_def)
            .into(hostPortraitImageView);
    }

    @OnClick(R2.id.contentLayout)
    void joinConference() {
        if (AVEngineKit.isSupportConference()) {
            Toast.makeText(fragment.getActivity(), "本版本不支持会议功能", Toast.LENGTH_SHORT).show();
            return;
        }
        AVEngineKit.Instance().joinConference(inviteMessageContent.getCallId(), inviteMessageContent.isAudioOnly(), inviteMessageContent.getPin(), inviteMessageContent.getHost(), inviteMessageContent.getTitle(), inviteMessageContent.getDesc(), inviteMessageContent.isAudience(), null);
        Intent intent = new Intent(fragment.getActivity(), ConferenceActivity.class);
        fragment.startActivity(intent);
    }
}
