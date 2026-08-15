/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.view.MenuItem;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfirechat.model.Conversation;

/**
 * 平板右栏里的会话页：{@link ConversationFragment} 加上一层 {@link WfcPage} 适配。
 * <p>
 * 手机端独立会话页里，菜单、返回键、启动参数是由 {@code ConversationActivity} 转发给
 * {@code ConversationFragment} 的；右栏没有 Activity，改由 {@code PanePageFragment} 通过
 * {@link WfcPage} 转发。这里的四个方法与 {@code ConversationActivity} 中对应的转发逐行等价。
 * <p>
 * <strong>做成子类而不是让 {@code ConversationFragment} 直接实现 {@code WfcPage}</strong>：
 * 会话页是全仓库最长、手机端最核心的一个类，本子类手机端永不实例化，
 * 因此对手机端的影响可以被静态地证明为零。
 */
public class ConversationPanePage extends ConversationFragment implements WfcPage {

    @Override
    public int pageMenu() {
        // 与 ConversationActivity.menu() 相同
        return R.menu.conversation;
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        return onConversationMenuItemSelected(item);
    }

    @Override
    public boolean onPageBackPressed() {
        return onBackPressed();
    }

    /**
     * 标题由会话页自己通过 {@link ConversationHost#setConversationTitle} 写到右栏 toolbar 上，
     * 这里返回空串而不是 null，避免在会话标题算出来之前先闪一下 manifest 里的 Activity label。
     */
    @Override
    public CharSequence pageTitle() {
        return "";
    }

    /**
     * 参数解析与 {@code ConversationActivity.init()} / {@code onNewIntent()} 完全一致，
     * 因此两条路径共用同一套 extra 约定，新增参数时不会漏掉右栏。
     */
    @Override
    public void onPageIntent(Intent intent) {
        Conversation conversation = intent.getParcelableExtra("conversation");
        if (conversation == null) {
            return;
        }
        setupConversation(conversation,
            intent.getStringExtra("conversationTitle"),
            intent.getLongExtra("toFocusMessageId", -1),
            intent.getStringExtra("channelPrivateChatUser"),
            intent.getBooleanExtra("isPreJoinedChatRoom", false));
    }

    /**
     * 同一个会话被再次打开（栈里已经有这一层，右栏刚退回到它）。
     * <p>
     * 只有「这次还要定位到某条消息」才重建——从按日期查找、链接记录点回本会话就是这种。
     * 单纯的重复打开（点头像进资料页再点发消息）什么都不做：重建会把草稿和滚动位置丢掉。
     */
    @Override
    public void onNewPageIntent(Intent intent) {
        if (intent.getLongExtra("toFocusMessageId", -1) > 0
            || intent.getLongExtra("highlightMessageId", 0) != 0) {
            onPageIntent(intent);
        }
    }
}
