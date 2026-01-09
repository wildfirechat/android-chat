/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.emoji2.widget.EmojiTextView;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.widget.LinkClickListener;
import cn.wildfire.chat.kit.widget.LinkTextViewMovementMethod;
import cn.wildfire.chat.kit.widget.selecttext.SelectTextHelper;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.menu.PopupMenu;


@MessageContentType(value = {
    TextMessageContent.class,
    PTextMessageContent.class

})
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    EmojiTextView contentTextView;
    TextView refTextView;
    SelectTextHelper selectTextHelper;
    long lastSelectedUpdateTimestamp = 0;
    CharSequence selectedText;

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
        // Destroy old SelectTextHelper to clean up listeners
        if (selectTextHelper != null) {
            selectTextHelper.destroy();
        }

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
                contentTextView.setText(content);
            }
        } else {
            contentTextView.setText(content);
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
        selectTextHelper = new SelectTextHelper.Builder(contentTextView)
            .setCursorHandleColor(Color.parseColor("#3B63E3")) // 游标颜色
            .setCursorHandleSizeInDp(22f) // 游标大小 单位dp
            .setSelectedColor(Color.parseColor("#ADE1F6")) // 选中文本的颜色
            .setSelectAll(true) // 初次选中是否全选 default true
            .setScrollShow(false) // 滚动时是否继续显示 default true
            .setSelectedAllNoPop(true) // 已经全选无弹窗，设置了监听会回调 onSelectAllShowCustomPop 方法
            .setMagnifierShow(true) // 放大镜 default true
            .setSelectTextLength(2)// 首次选中文本的长度 default 2
            .setPopDelay(100)// 弹窗延迟时间 default 100毫秒
            .build();
        selectTextHelper.setSelectListener(new SelectTextHelper.OnSelectListenerImpl() {

            @Override
            public void onTextSelected(CharSequence content) {
                Log.d("TODO", "onTextSelected: " + content);
                selectedText = content;
            }

            @Override
            public void onClickUrl(String url) {
                if (url.startsWith("http")) {
                    WfcWebViewActivity.loadUrl(fragment.getContext(), "", url);
                } else {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    fragment.startActivity(intent);
                }
            }

            @Override
            public PopupMenu newPopupMenu() {
                return ((ConversationMessageAdapter) adapter).popupMenuForMessageViewHolder(TextMessageContentViewHolder.class, TextMessageContentViewHolder.this, itemView);
            }
        });
        lastSelectedUpdateTimestamp = 0;
    }

    private String quoteMessageDigest(QuoteInfo quoteInfo) {
        Message message = quoteInfo.getMessage();
        if (message == null) {
            message = ChatManager.Instance().getMessageByUid(quoteInfo.getMessageUid());
        }
        String desc;
        if (message != null) {
            if (message.content instanceof RecallMessageContent) {
                desc = fragment.getString(R.string.message_recalled);
            } else {
                desc = message.content.digest(message);
            }
        } else {
            desc = fragment.getString(R.string.message_not_available);
            ChatManager.Instance().loadRemoteQuotedMessage(this.message.message);
        }
        return desc;
    }

    public void onClick(View view) {
        String content = ((TextMessageContent) message.message.content).getContent();
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), fragment.getString(R.string.message_detail_title), content);
    }

    public void onRefClick(View view) {
        Message message = ChatManager.Instance().getMessageByUid(quoteInfo.getMessageUid());
        if (message != null) {
            // TODO previewMessageActivity
            MessageContent messageContent = message.content;
            if (messageContent instanceof TextMessageContent) {
                WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), fragment.getString(R.string.message_detail_title), ((TextMessageContent) messageContent).getContent());
            } else {
                if (messageContent instanceof VideoMessageContent) {
                    MMPreviewActivity.previewVideo(fragment.getActivity(), message);
                } else if (messageContent instanceof ImageMessageContent) {
                    MMPreviewActivity.previewImage(fragment.getActivity(), message);
                }
            }
        }
    }

    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_COPY, confirm = false, priority = 12)
    public void copy(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        CharSequence text;
        if (!TextUtils.isEmpty(selectedText)) {
            text = selectedText;
        } else {
            TextMessageContent content = (TextMessageContent) message.message.content;
            text = content.getContent();
        }
        ClipData clipData = ClipData.newPlainText("messageContent", text);
        clipboardManager.setPrimaryClip(clipData);
    }


    @Override
    public String contextMenuTitle(Context context, String tag) {
        if (MessageContextMenuItemTags.TAG_COPY.equals(tag)) {
            return context.getString(R.string.message_copy);
        }
        return super.contextMenuTitle(context, tag);
    }

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        return super.contextMenuItemFilter(uiMessage, tag);
        // TODO 根据是否是部分选中进行过滤
    }
}
