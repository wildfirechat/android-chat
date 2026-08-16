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

import android.app.AlertDialog;

import cn.wildfire.chat.kit.Config;
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
        openLink(fragment, linkMessageContent.getUrl());
    }

    /**
     * 拿得到发起页时优先用这一版：平板上网页会压到会话所在的那条右栏栈上。
     * 只有 Context 的调用方走 {@link #openLink(android.content.Context, String)}。
     */
    public static void openLink(androidx.fragment.app.Fragment fragment, String url) {
        if (TextUtils.isEmpty(url) || fragment.getContext() == null) {
            return;
        }
        confirmAndOpen(fragment.getContext(), url, () -> WfcWebViewActivity.loadUrl(fragment, "", url));
    }

    public static void openLink(android.content.Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        confirmAndOpen(context, url, () -> WfcWebViewActivity.loadUrl(context, "", url));
    }

    /**
     * {@code Config.OPEN_LINK_POLICY} 的三种策略：2 禁止、1 先确认、其余直接打开。
     * 「怎么打开」由调用方给进来，两个重载只在这一步上不同。
     */
    private static void confirmAndOpen(android.content.Context context, String url, Runnable open) {
        if (Config.OPEN_LINK_POLICY == 2) {
            android.widget.Toast.makeText(context, R.string.open_link_forbidden, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (Config.OPEN_LINK_POLICY == 1) {
            new AlertDialog.Builder(context)
                .setTitle(R.string.tip)
                .setMessage(R.string.open_link_warning)
                .setPositiveButton(R.string.confirm_safe, (dialog, which) -> open.run())
                .setNegativeButton(R.string.close, null)
                .show();
            return;
        }
        open.run();
    }
}
