/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「会话页独占一个 Activity」这一经典形态的宿主实现，即手机端的现状。
 * <p>
 * 四个方法都是从改造前的 {@code ConversationFragment} 里原样搬过来的，行为逐行一致：
 * 标题写到 Activity 的 toolbar，关闭会话就是 {@code finish()}，高亮消息 id 从 Activity
 * 自己的 Intent 里读。
 * <p>
 * uikit 作为 AAR 被集成时，宿主可能是集成方自己的 {@link WfcBaseActivity} 而并未实现
 * {@link ConversationHost}，{@code ConversationFragment} 会自动回退到本实现，因此本次改造
 * 对集成方不是破坏性变更。
 */
public class WfcBaseActivityConversationHost implements ConversationHost {

    private final WfcBaseActivity activity;
    private final ConversationTitleHelper titleHelper;

    public WfcBaseActivityConversationHost(WfcBaseActivity activity) {
        this.activity = activity;
        this.titleHelper = new ConversationTitleHelper(activity, activity.getToolbar(), activity::setTitle);
    }

    @Override
    public void setConversationTitle(CharSequence title, CharSequence subTitle, boolean silent, boolean earpiece) {
        titleHelper.setTitle(title, subTitle, silent, earpiece);
    }

    @Override
    public CharSequence getConversationTitle() {
        return activity.getTitle();
    }

    @Override
    public void closeConversation() {
        activity.finish();
    }

    @Override
    public long getHighlightMessageId() {
        return activity.getIntent().getLongExtra("highlightMessageId", 0);
    }
}
