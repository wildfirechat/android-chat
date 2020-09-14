/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.message.CardMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@MessageContentType(value = {
    CardMessageContent.class,

})
@EnableContextMenu
public class UserCardMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.userCardPortraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.userCardNameTextView)
    TextView nameTextView;
    @BindView(R2.id.userIdTextView)
    TextView userIdTextView;

    CardMessageContent userCardMessageContent;

    public UserCardMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        userCardMessageContent = (CardMessageContent) message.message.content;
        nameTextView.setText(userCardMessageContent.getDisplayName());
        userIdTextView.setText(userCardMessageContent.getName());
        GlideApp
            .with(fragment)
            .load(userCardMessageContent.getPortrait())
            .transforms(new CenterCrop(), new RoundedCorners(10))
            .placeholder(R.mipmap.avatar_def)
            .into(portraitImageView);
    }

    @OnClick(R2.id.contentLayout)
    void onUserCardClick() {
        Intent intent = new Intent(fragment.getContext(), UserInfoActivity.class);
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userCardMessageContent.getTarget(), false);
        intent.putExtra("userInfo", userInfo);
        fragment.startActivity(intent);
    }
}
