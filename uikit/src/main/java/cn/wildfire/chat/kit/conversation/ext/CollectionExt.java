/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.collection.CollectionServiceProvider;
import cn.wildfire.chat.kit.collection.CreateCollectionActivity;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfirechat.model.Conversation;

/**
 * 接龙扩展
 * <p>
 * 在输入栏插件面板添加接龙按钮，仅群聊可用。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CollectionExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation  当前会话
     */
    @ExtContextMenuItem
    public void createCollection(View containerView, Conversation conversation) {
        Intent intent = new Intent(activity, CreateCollectionActivity.class);
        intent.putExtra(CreateCollectionActivity.EXTRA_CONVERSATION, conversation);
        startActivity(intent);
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public int iconResId() {
        return R.drawable.ic_ext_collection;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.collection);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }

    /**
     * 过滤条件：仅群聊显示，且服务已配置
     *
     * @param conversation 会话
     * @return true=不显示
     */
    @Override
    public boolean filter(Conversation conversation) {
        // 不是群聊，不显示
        if (conversation.type != Conversation.ConversationType.Group) {
            return true;
        }
        // 服务未配置，不显示
        return !CollectionServiceProvider.getInstance().isAvailable();
    }
}
