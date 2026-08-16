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
                handleUrlClick(fragment, url);
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

    /**
     * 拿得到发起页时优先用这一版：http 链接会开在会话所在的那条右栏栈上。
     * tel/mailto/geo/sms 仍然交给系统应用，与改造前一致。
     */
    public static void handleUrlClick(androidx.fragment.app.Fragment fragment, String url) {
        if (fragment.getContext() == null) {
            return;
        }
        if (isSystemHandledScheme(url)) {
            openWithSystemApp(fragment.getContext(), url);
        } else {
            LinkMessageContentViewHolder.openLink(fragment, url);
        }
    }

    public static void handleUrlClick(android.content.Context context, String url) {
        if (isSystemHandledScheme(url)) {
            openWithSystemApp(context, url);
        } else {
            LinkMessageContentViewHolder.openLink(context, url);
        }
    }

    private static boolean isSystemHandledScheme(String url) {
        String lower = url.toLowerCase();
        return lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("geo:")
            || lower.startsWith("sms:") || lower.startsWith("smsto:");
    }

    private static void openWithSystemApp(android.content.Context context, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
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
                spannable.setSpan(new PolicyURLSpan(fragment, urlSpan.getURL()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    /**
     * 带上发起页的 URLSpan。
     * <p>
     * 改造前只有 {@code widget.getContext()}，那是双栏主界面，网页只能靠「上一次点在哪一栏」
     * 去猜该压到哪条栈上。span 与本 viewholder 同生共死，本来就攥着 fragment，多存一份不增加泄漏面。
     */
    private static class PolicyURLSpan extends URLSpan {
        private final androidx.fragment.app.Fragment fragment;

        public PolicyURLSpan(androidx.fragment.app.Fragment fragment, String url) {
            super(url);
            this.fragment = fragment;
        }

        @Override
        public void onClick(View widget) {
            if (fragment != null && fragment.getContext() != null) {
                handleUrlClick(fragment, getURL());
            } else {
                handleUrlClick(widget.getContext(), getURL());
            }
        }
    }
}
