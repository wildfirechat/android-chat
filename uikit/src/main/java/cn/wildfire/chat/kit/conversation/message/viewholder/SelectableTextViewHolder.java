/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.WfcWebViewActivity;
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
}
