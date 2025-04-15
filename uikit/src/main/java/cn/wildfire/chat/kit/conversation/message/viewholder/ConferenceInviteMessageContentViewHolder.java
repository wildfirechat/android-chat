/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.Manifest;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.voip.conference.ConferenceInfoActivity;
import cn.wildfirechat.uikit.permission.PermissionKit;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(value = {
    ConferenceInviteMessageContent.class,
})
public class ConferenceInviteMessageContentViewHolder extends NormalMessageContentViewHolder {
    ImageView hostPortraitImageView;
    TextView titleTextView;
    TextView descTextView;

    private ConferenceInviteMessageContent inviteMessageContent;

    public ConferenceInviteMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentLayout).setOnClickListener(_v -> joinConference());
    }

    private void bindViews(View itemView) {
        hostPortraitImageView = itemView.findViewById(R.id.hostPortraitImageView);
        titleTextView = itemView.findViewById(R.id.titleTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
    }

    @Override
    protected void onBind(UiMessage message) {
        inviteMessageContent = (ConferenceInviteMessageContent) message.message.content;
        titleTextView.setText(inviteMessageContent.getTitle());
        descTextView.setText(inviteMessageContent.getDesc());
        UserInfo userInfo = ChatManager.Instance().getUserInfo(inviteMessageContent.getHost(), false);
        Glide
            .with(fragment)
            .load(userInfo.portrait)
            .transforms(new CenterCrop(), new RoundedCorners(10))
            .placeholder(R.mipmap.avatar_def)
            .into(hostPortraitImageView);
    }

    void joinConference() {
        if (!AVEngineKit.isSupportConference()) {
            Toast.makeText(fragment.getActivity(), R.string.conference_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(fragment.getActivity(), permissions);
        PermissionKit.checkThenRequestPermission(fragment.getActivity(), fragment.getChildFragmentManager(), tuples, o -> {
            if (o) {
                Intent intent = new Intent(fragment.getActivity(), ConferenceInfoActivity.class);
                intent.putExtra("conferenceId", inviteMessageContent.getCallId());
                intent.putExtra("password", inviteMessageContent.getPassword());
                fragment.startActivity(intent);
            }
        });
    }
}
