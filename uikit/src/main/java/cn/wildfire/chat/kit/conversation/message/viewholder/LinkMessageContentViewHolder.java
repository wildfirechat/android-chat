/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.LinkMessageContent;

@MessageContentType(LinkMessageContent.class)
@EnableContextMenu
public class LinkMessageContentViewHolder extends NormalMessageContentViewHolder{
    @BindView(R2.id.thumbnailImageView)
    ImageView thumbnailImageView;
    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;

    private LinkMessageContent linkMessageContent;

    public LinkMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        linkMessageContent = (LinkMessageContent) message.message.content;
        titleTextView.setText(linkMessageContent.getTitle());
        descTextView.setText(!TextUtils.isEmpty(linkMessageContent.getContentDigest())? linkMessageContent.getContentDigest() : linkMessageContent.getUrl());
        GlideApp.with(fragment)
            .load(linkMessageContent.getThumbnailUrl())
            .placeholder(R.mipmap.logo)
            .into(thumbnailImageView);
    }

    @OnClick(R2.id.linkMessageContentItemView)
    public void onClick(View view) {
        WfcWebViewActivity.loadUrl(fragment.getContext(), linkMessageContent.getTitle(), linkMessageContent.getUrl());
    }
}
