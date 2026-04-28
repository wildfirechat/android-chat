/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

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
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播消息视图持有者
 * <p>
 * 用于在会话列表中展示直播邀请消息。点击后跳转到观众观看直播页面。
 * </p>
 */
@MessageContentType(value = {
    LiveStreamingStartMessageContent.class,
})
public class LiveStreamingMessageContentViewHolder extends NormalMessageContentViewHolder {

    ImageView hostPortraitImageView;
    TextView titleTextView;
    TextView statusTextView;

    private LiveStreamingStartMessageContent liveContent;

    public LiveStreamingMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindViews(View itemView) {
        hostPortraitImageView = itemView.findViewById(R.id.hostPortraitImageView);
        titleTextView = itemView.findViewById(R.id.titleTextView);
        statusTextView = itemView.findViewById(R.id.statusTextView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentLayout).setOnClickListener(_v -> watchLive());
    }

    @Override
    protected void onBind(UiMessage message) {
        liveContent = (LiveStreamingStartMessageContent) message.message.content;
        String title = liveContent.getTitle();
        titleTextView.setText(title != null && !title.isEmpty() ? title : fragment.getString(R.string.live_streaming));

        UserInfo hostInfo = ChatManager.Instance().getUserInfo(liveContent.getHost(), false);
        if (hostInfo != null) {
            Glide.with(fragment)
                .load(hostInfo.portrait)
                .transforms(new CenterCrop(), new RoundedCorners(10))
                .placeholder(R.mipmap.avatar_def)
                .into(hostPortraitImageView);
        }
    }

    private void watchLive() {
        Intent intent = new Intent(fragment.getActivity(), LiveAudienceActivity.class);
        intent.putExtra("liveContent", liveContent);
        fragment.startActivity(intent);
    }
}
