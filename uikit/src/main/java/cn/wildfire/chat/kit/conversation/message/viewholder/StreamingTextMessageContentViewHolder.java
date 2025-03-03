/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.widget.LinkClickListener;
import cn.wildfire.chat.kit.widget.LinkTextViewMovementMethod;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.StreamingTextGeneratedMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratingMessageContent;


@MessageContentType(value = {
    StreamingTextGeneratingMessageContent.class,
    StreamingTextGeneratedMessageContent.class
})
@EnableContextMenu
public class StreamingTextMessageContentViewHolder extends NormalMessageContentViewHolder {
    TextView contentTextView;
    ProgressBar progressBar;

    public StreamingTextMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentTextView).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        contentTextView = itemView.findViewById(R.id.contentTextView);
        progressBar = itemView.findViewById(R.id.progressBar);
    }

    @Override
    public void onBind(UiMessage message) {
        MessageContent messageContent = message.message.content;
        String content = this.streamingTextContent();
        if (messageContent instanceof StreamingTextGeneratingMessageContent) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
        if (content.startsWith("<") && content.endsWith(">")) {
            Spanned spanned = null;
            try {
                spanned = Html.fromHtml(content);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (spanned != null && spanned.length() > 0) {
                contentTextView.setText(spanned);
            } else {
                MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);
            }
        } else {
            MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, content, ImageSpan.ALIGN_BOTTOM);
        }
        contentTextView.setMovementMethod(new LinkTextViewMovementMethod(new LinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                WfcWebViewActivity.loadUrl(fragment.getContext(), "", link);
                return true;
            }
        }));
    }

    public void onClick(View view) {
        String content = this.streamingTextContent();
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), fragment.getString(R.string.message_detail_title), content);
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        ClipData clipData = ClipData.newPlainText("messageContent", this.streamingTextContent());
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CLIP.equals(tag)) {
            return context.getString(R.string.message_copy);
        }
        return super.contextMenuTitle(context, tag);
    }

    private String streamingTextContent() {
        MessageContent messageContent = message.message.content;
        String content;
        if (messageContent instanceof StreamingTextGeneratingMessageContent) {
            content = ((StreamingTextGeneratingMessageContent) messageContent).getText();
        } else {
            content = ((StreamingTextGeneratedMessageContent) messageContent).getText();
        }
        return content;
    }
}
