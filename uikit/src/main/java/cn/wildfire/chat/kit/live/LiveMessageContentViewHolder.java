/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfire.chat.kit.live.message.LiveMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播消息视图持有者
 * <p>
 * 用于在会话列表中展示直播邀请消息。点击后跳转到观众观看直播页面。
 * </p>
 */
@MessageContentType(value = {
        LiveMessageContent.class,
})
public class LiveMessageContentViewHolder extends NormalMessageContentViewHolder {

    ImageView hostPortraitImageView;
    TextView titleTextView;
    TextView statusTextView;

    private LiveMessageContent liveContent;

    public LiveMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
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
        liveContent = (LiveMessageContent) message.message.content;
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

    @OptIn(markerClass = UnstableApi.class)
    private void watchLive() {
        if (TextUtils.isEmpty(Config.LIVE_ADDRESS)) {
            Toast.makeText(fragment.getContext(), "未配置直播服务", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(fragment.getActivity(), LiveInfoActivity.class);
        intent.putExtra("liveId", liveContent.getLiveId());
        fragment.startActivity(intent);
    }
}
