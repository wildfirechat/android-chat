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
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import cn.wildfire.chat.kit.*;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.widget.LinkClickListener;
import cn.wildfire.chat.kit.widget.LinkTextViewMovementMethod;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.remote.ChatManager;


@MessageContentType(value = {
    TextMessageContent.class,
    PTextMessageContent.class

})
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    TextView contentTextView;
    TextView refTextView;

    private QuoteInfo quoteInfo;

    public TextMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.contentTextView).setOnClickListener(this::onClick);
        itemView.findViewById(R.id.refTextView).setOnClickListener(this::onRefClick);
    }

    private void bindViews(View itemView) {
        contentTextView = itemView.findViewById(R.id.contentTextView);
        refTextView = itemView.findViewById(R.id.refTextView);
    }

    @Override
    public void onBind(UiMessage message) {
        TextMessageContent textMessageContent = (TextMessageContent) message.message.content;
        String content = textMessageContent.getContent();
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
                MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
            }
        } else {
            MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
        }
        contentTextView.setMovementMethod(new LinkTextViewMovementMethod(new LinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                WfcWebViewActivity.loadUrl(fragment.getContext(), "", link);
                return true;
            }
        }));

        quoteInfo = textMessageContent.getQuoteInfo();
        if (quoteInfo != null && quoteInfo.getMessageUid() > 0) {
            refTextView.setVisibility(View.VISIBLE);
            refTextView.setText(this.quoteMessageDigest(quoteInfo));
        } else {
            refTextView.setVisibility(View.GONE);
        }
    }

    private String quoteMessageDigest(QuoteInfo quoteInfo) {
        Message message = quoteInfo.getMessage();
        if (message == null) {
            message = ChatManager.Instance().getMessageByUid(quoteInfo.getMessageUid());
        }
        String desc;
        if (message != null) {
            if (message.content instanceof RecallMessageContent) {
                desc = "消息已被撤回";
            } else {
                desc = message.content.digest(message);
            }
        } else {
            desc = "消息不可用，可能被删除或者过期";
            ChatManager.Instance().loadRemoteQuotedMessage(this.message.message);
        }

        return desc;
    }

    public void onClick(View view) {
        String content = ((TextMessageContent) message.message.content).getContent();
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "消息内容", content);
    }

    public void onRefClick(View view) {
        Message message = ChatManager.Instance().getMessageByUid(quoteInfo.getMessageUid());
        if (message != null) {
            // TODO previewMessageActivity
            MessageContent messageContent = message.content;
            if (messageContent instanceof TextMessageContent) {
                WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "消息内容", ((TextMessageContent) messageContent).getContent());
            } else {
                if (messageContent instanceof VideoMessageContent) {
                    MMPreviewActivity.previewVideo(fragment.getActivity(), message);
                } else if (messageContent instanceof ImageMessageContent) {
                    MMPreviewActivity.previewImage(fragment.getActivity(), message);
                }
            }
        }
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        TextMessageContent content = (TextMessageContent) message.message.content;
        ClipData clipData = ClipData.newPlainText("messageContent", content.getContent());
        clipboardManager.setPrimaryClip(clipData);
    }


    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_CLIP.equals(tag)) {
            return "复制";
        }
        return super.contextMenuTitle(context, tag);
    }
}
