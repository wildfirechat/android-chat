/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.poll.activity.PollHomeActivity;
import cn.wildfire.chat.kit.poll.service.PollServiceProvider;
import cn.wildfirechat.model.Conversation;

/**
 * 投票扩展
 * <p>
 * 在输入栏插件面板添加投票按钮，仅群聊可用。
 * 点击后进入投票首页，包含发起投票和我的投票两个入口。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class PollExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation  当前会话
     */
    @ExtContextMenuItem
    public void openPollHome(View containerView, Conversation conversation) {
        Intent intent = PollHomeActivity.buildIntent(activity, conversation.target);
        startActivity(intent);
    }

    @Override
    public int priority() {
        return 85;
    }

    @Override
    public int iconResId() {
        // 使用投票图标，如果没有则使用收藏图标作为替代
        return R.drawable.ic_poll;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.poll);
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
        return !PollServiceProvider.getInstance().isAvailable();
    }
}
