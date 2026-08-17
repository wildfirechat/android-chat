/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.content.Context;
import android.content.Intent;
import android.view.MenuItem;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

public class ConversationActivity extends WfcBaseActivity {
    private boolean isInitialized = false;
    private ConversationFragment conversationFragment;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    private void setConversationBackground() {
        // 设置聊天背景
//        conversationFragment.setConversationBackgroundImage("https://static.wildfirechat.net/web_wfc_bg2.jpeg");
    }

    @Override
    protected void afterViews() {
        IMServiceStatusViewModel imServiceStatusViewModel =new ViewModelProvider(this).get(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, aBoolean -> {
            if (!isInitialized && aBoolean) {
                init();
                isInitialized = true;
            }
        });
        // 配置变化（Pad 解锁了横竖屏/分屏）后 FragmentManager 已经把会话页恢复出来了，
        // 无条件 add 会在恢复出来的这个之上再叠一层，见 PAD_ADAPTATION_REVIEW.md P1。
        Fragment restored = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (restored instanceof ConversationFragment) {
            conversationFragment = (ConversationFragment) restored;
        } else {
            conversationFragment = new ConversationFragment();
            getSupportFragmentManager().beginTransaction()
                .add(R.id.containerFrameLayout, conversationFragment, "content")
                .commit();
        }

        setAppBarLayoutElevation(1);
        setConversationBackground();
    }

    @Override
    protected int menu() {
        return R.menu.conversation;
    }

    public ConversationFragment getConversationFragment() {
        return conversationFragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 「会话信息」的处理已下沉到 ConversationFragment（平板双栏右栏复用同一套逻辑），这里只做转发
        if (conversationFragment != null && conversationFragment.onConversationMenuItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!conversationFragment.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Conversation conversation = intent.getParcelableExtra("conversation");
        if (conversation == null) {
            finish();
            return;
        }
        long initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        String channelPrivateChatUser = intent.getStringExtra("channelPrivateChatUser");
        String conversationTitle = intent.getStringExtra("conversationTitle");
        boolean isPreJoinedChatRoom = intent.getBooleanExtra("isPreJoinedChatRoom", false);
        conversationFragment.setupConversation(conversation, conversationTitle, initialFocusedMessageId, channelPrivateChatUser, isPreJoinedChatRoom);
    }


    private void init() {
        Intent intent = getIntent();
        Conversation conversation = intent.getParcelableExtra("conversation");
        String conversationTitle = intent.getStringExtra("conversationTitle");
        boolean isPreJoinedChatRoom = intent.getBooleanExtra("isPreJoinedChatRoom", false);
        long initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        if (conversation == null) {
            finish();
            return;
        }
        conversationFragment.setupConversation(conversation, conversationTitle, initialFocusedMessageId, null, isPreJoinedChatRoom);
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line) {
        return buildConversationIntent(context, type, target, line, -1);
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line, long toFocusMessageId) {
        Conversation conversation = new Conversation(type, target, line);
        return buildConversationIntent(context, conversation, null, toFocusMessageId);
    }

    public static Intent buildConversationIntent(Context context, Conversation.ConversationType type, String target, int line, String channelPrivateChatUser) {
        Conversation conversation = new Conversation(type, target, line);
        return buildConversationIntent(context, conversation, null, -1);
    }

    public static Intent buildChatRoomConversationIntent(Context context, String chatRoomId, int line, String title, boolean joined) {
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, chatRoomId, line);
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("conversationTitle", title);
        intent.putExtra("isPreJoinedChatRoom", joined);
        return intent;
    }

    public static Intent buildConversationIntent(Context context, Conversation conversation, String channelPrivateChatUser, long toFocusMessageId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("toFocusMessageId", toFocusMessageId);
        intent.putExtra("channelPrivateChatUser", channelPrivateChatUser);
        return intent;
    }
}
