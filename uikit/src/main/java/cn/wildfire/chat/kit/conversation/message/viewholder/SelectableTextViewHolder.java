/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationMessageAdapter;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.widget.selecttext.SelectTextHelper;
import cn.wildfirechat.uikit.menu.PopupMenu;

public abstract class SelectableTextViewHolder extends NormalMessageContentViewHolder {
    protected SelectTextHelper selectTextHelper;
    protected CharSequence selectedText;

    public SelectableTextViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBind(UiMessage message) {
        // Destroy old SelectTextHelper to clean up listeners
        if (selectTextHelper != null) {
            selectTextHelper.destroy();
        }
        selectTextHelper = new SelectTextHelper.Builder(selectableTextView())
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
                handleUrlClick(fragment.getContext(), url);
            }

            @Override
            public PopupMenu newPopupMenu() {
                return ((ConversationMessageAdapter) adapter).popupMenuForMessageViewHolder(SelectableTextViewHolder.this.getClass(), SelectableTextViewHolder.this, itemView);
            }
        });
    }

    abstract protected TextView selectableTextView();

    @Override
    public boolean contextMenuItemFilter(UiMessage uiMessage, String tag) {
        return super.contextMenuItemFilter(uiMessage, tag);
        // TODO 根据是否是部分选中进行过滤
    }

    public static void handleUrlClick(android.content.Context context, String url) {
        String lower = url.toLowerCase();
        if (lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("geo:") || lower.startsWith("sms:") || lower.startsWith("smsto:")) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        } else {
            LinkMessageContentViewHolder.openLink(context, url);
        }
    }

    protected void replaceUrlSpans(TextView textView) {
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            URLSpan[] urlSpans = spannable.getSpans(0, text.length(), URLSpan.class);
            for (URLSpan urlSpan : urlSpans) {
                int start = spannable.getSpanStart(urlSpan);
                int end = spannable.getSpanEnd(urlSpan);
                spannable.removeSpan(urlSpan);
                spannable.setSpan(new PolicyURLSpan(urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private static class PolicyURLSpan extends URLSpan {
        public PolicyURLSpan(String url) {
            super(url);
        }

        @Override
        public void onClick(View widget) {
            handleUrlClick(widget.getContext(), getURL());
        }
    }
}
