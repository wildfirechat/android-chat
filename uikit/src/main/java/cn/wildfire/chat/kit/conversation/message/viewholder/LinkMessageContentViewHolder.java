/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.LinkMessageContent;

@MessageContentType(LinkMessageContent.class)
@EnableContextMenu
public class LinkMessageContentViewHolder extends NormalMessageContentViewHolder {
    ImageView thumbnailImageView;
    TextView titleTextView;
    TextView descTextView;

    private LinkMessageContent linkMessageContent;

    public LinkMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
       itemView.findViewById(R.id.linkMessageContentItemView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        thumbnailImageView =itemView.findViewById(R.id.thumbnailImageView);
        titleTextView =itemView.findViewById(R.id.titleTextView);
        descTextView =itemView.findViewById(R.id.descTextView);
    }

    @Override
    public void onBind(UiMessage message) {
        linkMessageContent = (LinkMessageContent) message.message.content;
        titleTextView.setText(linkMessageContent.getTitle());
        descTextView.setText(!TextUtils.isEmpty(linkMessageContent.getContentDigest()) ? linkMessageContent.getContentDigest() : linkMessageContent.getUrl());
        Glide.with(fragment)
            .load(linkMessageContent.getThumbnailUrl())
            .placeholder(R.mipmap.logo)
            .into(thumbnailImageView);
    }

    public void onClick(View view) {
        WfcWebViewActivity.loadUrl(fragment.getContext(), linkMessageContent.getTitle(), linkMessageContent.getUrl());
    }
}
