package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.OnTouch;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.IMServiceStatusViewModel;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.channel.ChannelViewModel;
import cn.wildfire.chat.kit.chatroom.ChatRoomViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardAwareLinearLayout;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

public class ConversationActivity extends WfcBaseActivity implements
        KeyboardAwareLinearLayout.OnKeyboardShownListener,
        KeyboardAwareLinearLayout.OnKeyboardHiddenListener,
        ConversationMessageAdapter.OnPortraitClickListener,
        ConversationMessageAdapter.OnPortraitLongClickListener, ConversationInputPanel.OnConversationInputPanelStateChangeListener {

    public static final int REQUEST_PICK_MENTION_CONTACT = 100;

    private Conversation conversation;
    private boolean loadingNewMessage;
    private boolean shouldContinueLoadNewMessage = false;

    private static final int MESSAGE_LOAD_COUNT_PER_TIME = 20;
    private static final int MESSAGE_LOAD_AROUND = 10;

    @Bind(R.id.rootLinearLayout)
    InputAwareLayout rootLinearLayout;
    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @Bind(R.id.msgRecyclerView)
    RecyclerView recyclerView;

    @Bind(R.id.inputPanelFrameLayout)
    ConversationInputPanel inputPanel;

    private ConversationMessageAdapter adapter;
    private boolean moveToBottom = true;
    private ConversationViewModel conversationViewModel;
    private UserViewModel userViewModel;
    private IMServiceStatusViewModel imServiceStatusViewModel;
    private boolean isInitialized = false;
    private ChatRoomViewModel chatRoomViewModel;

    private Handler handler;
    private long initialFocusedMessageId;
    // 用户channel主发起，针对某个用户的会话
    private String channelPrivateChatUser;
    private String conversationTitle = "";
    private SharedPreferences sharedPreferences;
    private LinearLayoutManager layoutManager;

    private GroupInfo groupInfo;
    private boolean showGroupMemberName = false;

    private Observer<UiMessage> messageLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            MessageContent content = uiMessage.message.content;
            if (isDisplayableMessage(uiMessage)) {
                // 消息定位时，如果收到新消息、或者发送消息，需要重新加载消息列表
                if (shouldContinueLoadNewMessage) {
                    shouldContinueLoadNewMessage = false;
                    reloadMessage();
                    return;
                }
                adapter.addNewMessage(uiMessage);
                if (moveToBottom || uiMessage.message.sender.equals(ChatManager.Instance().getUserId())) {
                    UIUtils.postTaskDelay(() -> {

                                int position = adapter.getItemCount() - 1;
                                if (position < 0) {
                                    return;
                                }
                                recyclerView.scrollToPosition(position);
                            },
                            100);
                }
            }
            if (content instanceof TypingMessageContent && uiMessage.message.direction == MessageDirection.Receive) {
                updateTypingStatusTitle((TypingMessageContent) content);
            } else {
                resetConversationTitle();
            }

            if (uiMessage.message.direction == MessageDirection.Receive) {
                conversationViewModel.clearUnreadStatus(conversation);
            }
        }
    };
    private Observer<UiMessage> messageUpdateLiveDatObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            if (isDisplayableMessage(uiMessage)) {
                adapter.updateMessage(uiMessage);
            }
        }
    };

    private Observer<UiMessage> messageRemovedLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            if (isDisplayableMessage(uiMessage)) {
                adapter.removeMessage(uiMessage);
            }
        }
    };

    private boolean isDisplayableMessage(UiMessage uiMessage) {
        MessageContent content = uiMessage.message.content;
        if (content.getPersistFlag() == PersistFlag.Persist
                || content.getPersistFlag() == PersistFlag.Persist_And_Count) {
            return true;
        }
        return false;
    }

    private Observer<Map<String, String>> mediaUploadedLiveDataObserver = new Observer<Map<String, String>>() {
        @Override
        public void onChanged(@Nullable Map<String, String> stringStringMap) {
            for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
                sharedPreferences.edit()
                        .putString(entry.getKey(), entry.getValue())
                        .apply();
            }

        }
    };

    private Observer<List<UserInfo>> userInfoUpdateLiveDataObserver = new Observer<List<UserInfo>>() {
        @Override
        public void onChanged(@Nullable List<UserInfo> userInfos) {
            if (conversation.type == Conversation.ConversationType.Single) {
                conversationTitle = null;
                setTitle();
            }
            int start = layoutManager.findFirstVisibleItemPosition();
            int end = layoutManager.findLastVisibleItemPosition();
            adapter.notifyItemRangeChanged(start, end - start, userInfos);
        }
    };

    private Observer<Object> clearMessageLiveDataObserver = (obj) -> {
        adapter.setMessages(new ArrayList<>());
        adapter.notifyDataSetChanged();
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // you can setup background here
//        getWindow().setBackgroundDrawableResource(R.mipmap.splash);
    }

    @Override
    protected void afterViews() {
        imServiceStatusViewModel = WfcUIKit.getAppScopeViewModel(IMServiceStatusViewModel.class);
        imServiceStatusViewModel.imServiceStatusLiveData().observe(this, aBoolean -> {
            if (!isInitialized && aBoolean) {
                init();
                isInitialized = true;
            }
        });
    }

    private void init() {
        initView();
        sharedPreferences = getSharedPreferences("sticker", Context.MODE_PRIVATE);
        Intent intent = getIntent();
        conversation = intent.getParcelableExtra("conversation");
        conversationTitle = intent.getStringExtra("conversationTitle");
        initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        if (conversation == null) {
            finish();
        }
        setupConversation(conversation);
        conversationViewModel.clearUnreadStatus(conversation);
    }

    @Override
    protected int contentLayout() {
        return R.layout.conversation_activity;
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

    public static Intent buildConversationIntent(Context context, Conversation conversation, String channelPrivateChatUser, long toFocusMessageId) {
        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("conversation", conversation);
        intent.putExtra("toFocusMessageId", toFocusMessageId);
        intent.putExtra("channelPrivateChatUser", channelPrivateChatUser);
        return intent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        conversation = intent.getParcelableExtra("conversation");
        initialFocusedMessageId = intent.getLongExtra("toFocusMessageId", -1);
        channelPrivateChatUser = intent.getStringExtra("channelPrivateChatUser");
        setupConversation(conversation);
    }

    private void initView() {
        handler = new Handler();
        rootLinearLayout.addOnKeyboardShownListener(this);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (adapter.getMessages() == null || adapter.getMessages().isEmpty()) {
                swipeRefreshLayout.setRefreshing(false);
                return;
            }
            loadMoreOldMessages();
        });

        // message list
        adapter = new ConversationMessageAdapter(this);
        adapter.setOnPortraitClickListener(this);
        adapter.setOnPortraitLongClickListener(this);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 向上滑动，不在底部，收到消息时，不滑动到底部, 发送消息时，可以强制置为true
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1)) {
                    moveToBottom = true;
                    if (initialFocusedMessageId != -1 && !loadingNewMessage && shouldContinueLoadNewMessage) {
                        int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                        if (lastVisibleItem > adapter.getItemCount() - 3) {
                            loadMoreNewMessages();
                        }
                    }
                } else {
                    moveToBottom = false;
                }
            }
        });

        inputPanel.init(this, rootLinearLayout);
        inputPanel.setOnConversationInputPanelStateChangeListener(this);
    }

    private void setupConversation(Conversation conversation) {
        if (conversationViewModel == null) {
            conversationViewModel = ViewModelProviders.of(this, new ConversationViewModelFactory(conversation, channelPrivateChatUser)).get(ConversationViewModel.class);

            conversationViewModel.messageLiveData().observeForever(messageLiveDataObserver);
            conversationViewModel.messageUpdateLiveData().observeForever(messageUpdateLiveDatObserver);
            conversationViewModel.messageRemovedLiveData().observeForever(messageRemovedLiveDataObserver);
            conversationViewModel.mediaUpdateLiveData().observeForever(mediaUploadedLiveDataObserver);
            conversationViewModel.clearMessageLiveData().observeForever(clearMessageLiveDataObserver);

            userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            userViewModel.userInfoLiveData().observeForever(userInfoUpdateLiveDataObserver);
        } else {
            conversationViewModel.setConversation(conversation, channelPrivateChatUser);
        }

        if (conversation.type == Conversation.ConversationType.Group) {
            GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
            groupViewModel.groupInfoUpdateLiveData().observe(this, groupInfos -> {
                for (GroupInfo info : groupInfos) {
                    if (info.target.equals(groupInfo.target)) {
                        groupInfo = info;
                        setTitle();
                        adapter.notifyDataSetChanged();
                    }
                }
            });

            showGroupMemberName = "1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));
            userViewModel.settingUpdatedLiveData().observe(this, o -> {
                boolean showGroupMemberName = "1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));
                if (this.showGroupMemberName != showGroupMemberName) {
                    this.showGroupMemberName = showGroupMemberName;
                    adapter.notifyDataSetChanged();
                }
            });
        }

        inputPanel.setupConversation(conversationViewModel, conversation);

        MutableLiveData<List<UiMessage>> messages;
        if (initialFocusedMessageId != -1) {
            shouldContinueLoadNewMessage = true;
            messages = conversationViewModel.loadAroundMessages(initialFocusedMessageId, MESSAGE_LOAD_AROUND);
        } else {
            messages = conversationViewModel.getMessages();
        }

        // load message
        swipeRefreshLayout.setRefreshing(true);
        messages.observe(this, uiMessages -> {
            swipeRefreshLayout.setRefreshing(false);
            adapter.setMessages(uiMessages);
            adapter.notifyDataSetChanged();

            if (adapter.getItemCount() > 1) {
                int initialMessagePosition;
                if (initialFocusedMessageId != -1) {
                    initialMessagePosition = adapter.getMessagePosition(initialFocusedMessageId);
                    if (initialMessagePosition != -1) {
                        recyclerView.scrollToPosition(initialMessagePosition);
                        adapter.highlightFocusMessage(initialMessagePosition);
                    }
                } else {
                    moveToBottom = true;
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });
        if (conversation.type == Conversation.ConversationType.ChatRoom) {
            joinChatRoom();
        }

        setTitle();
    }

    private void joinChatRoom() {
        chatRoomViewModel = ViewModelProviders.of(this).get(ChatRoomViewModel.class);
        chatRoomViewModel.joinChatRoom(conversation.target)
                .observe(this, new Observer<OperateResult<Boolean>>() {
                    @Override
                    public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                        if (booleanOperateResult.isSuccess()) {
                            String welcome = "欢迎 %s 加入聊天室";
                            TipNotificationContent content = new TipNotificationContent();
                            String userId = userViewModel.getUserId();
                            UserInfo userInfo = userViewModel.getUserInfo(userId, false);
                            if (userInfo != null) {
                                content.tip = String.format(welcome, userInfo.displayName);
                            } else {
                                content.tip = String.format(welcome, "<" + userId + ">");
                            }
                            conversationViewModel.sendMessage(content);
                            loadMoreOldMessages();
                            setChatRoomConversationTitle();

                        } else {
                            Toast.makeText(ConversationActivity.this, "加入聊天室失败", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void quitChatRoom() {
        String welcome = "%s 离开了聊天室";
        TipNotificationContent content = new TipNotificationContent();
        String userId = userViewModel.getUserId();
        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        if (userInfo != null) {
            content.tip = String.format(welcome, userInfo.displayName);
        } else {
            content.tip = String.format(welcome, "<" + userId + ">");
        }
        conversationViewModel.sendMessage(content);
        chatRoomViewModel.quitChatRoom(conversation.target);
    }

    private void setChatRoomConversationTitle() {
        chatRoomViewModel.getChatRoomInfo(conversation.target, System.currentTimeMillis())
                .observe(this, chatRoomInfoOperateResult -> {
                    if (chatRoomInfoOperateResult.isSuccess()) {
                        ChatRoomInfo chatRoomInfo = chatRoomInfoOperateResult.getResult();
                        conversationTitle = chatRoomInfo.title;
                        setTitle(conversationTitle);
                    }
                });
    }

    private void setTitle() {
        if (!TextUtils.isEmpty(conversationTitle)) {
            setTitle(conversationTitle);
        }

        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(conversation.target, false);
            conversationTitle = userViewModel.getUserDisplayName(userInfo);
        } else if (conversation.type == Conversation.ConversationType.Group) {
            if (groupInfo != null) {
                conversationTitle = groupInfo.name;
            }
        } else if (conversation.type == Conversation.ConversationType.Channel) {
            ChannelViewModel channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
            ChannelInfo channelInfo = channelViewModel.getChannelInfo(conversation.target, false);
            if (channelInfo != null) {
                conversationTitle = channelInfo.name;
            }

            if (!TextUtils.isEmpty(channelPrivateChatUser)) {
                UserInfo channelPrivateChatUserInfo = userViewModel.getUserInfo(channelPrivateChatUser, false);
                if (channelPrivateChatUserInfo != null) {
                    conversationTitle += "@" + userViewModel.getUserDisplayName(channelPrivateChatUserInfo);
                } else {
                    conversationTitle += "@<" + channelPrivateChatUser + ">";
                }
            }
        }
        setTitle(conversationTitle);
    }

    @Override
    protected int menu() {
        return R.menu.conversation;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_conversation_info) {
            showConversationInfo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showConversationInfo() {
        Intent intent = new Intent(ConversationActivity.this, ConversationInfoActivity.class);
        intent.putExtra("conversationInfo", ChatManager.Instance().getConversation(conversation));
        startActivity(intent);
    }

    @OnTouch({R.id.contentLayout, R.id.msgRecyclerView})
    boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && inputPanel.extension.canHideOnScroll()) {
            inputPanel.collapse();
        }
        return false;
    }

    @Override
    public void onPortraitClick(UserInfo userInfo) {
        Intent intent = new Intent(this, UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @Override
    public void onPortraitLongClick(UserInfo userInfo) {
        // TODO panel insert
        int position = inputPanel.editText.getSelectionEnd();
        position = position >= 0 ? position : 0;
        if (conversation.type == Conversation.ConversationType.Group) {
            SpannableString spannableString = mentionSpannable(userInfo);
            inputPanel.editText.getEditableText().insert(position, spannableString);
        } else {
            inputPanel.editText.getEditableText().insert(position, userViewModel.getUserDisplayName(userInfo));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode >= ConversationExtension.REQUEST_CODE_MIN) {
            inputPanel.extension.onActivityResult(requestCode, resultCode, data);
            return;
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_PICK_MENTION_CONTACT) {
            boolean isMentionAll = data.getBooleanExtra("mentionAll", false);
            SpannableString spannableString;
            if (isMentionAll) {
                spannableString = mentionAllSpannable();
            } else {
                String userId = data.getStringExtra("userId");
                UserInfo userInfo = userViewModel.getUserInfo(userId, false);
                spannableString = mentionSpannable(userInfo);
            }
            int position = inputPanel.editText.getSelectionEnd();
            position = position > 0 ? position - 1 : 0;
            inputPanel.editText.getEditableText().replace(position, position + 1, spannableString);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private SpannableString mentionAllSpannable() {
        String text = "@所有人 ";
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new MentionSpan(true), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private SpannableString mentionSpannable(UserInfo userInfo) {
        String text = "@" + userInfo.displayName + " ";
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new MentionSpan(userInfo.uid), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    protected void onPause() {
        super.onPause();
        inputPanel.onActivityPause();
    }

    @Override
    protected void onDestroy() {
        if (conversation.type == Conversation.ConversationType.ChatRoom) {
            quitChatRoom();
        }

        super.onDestroy();
        conversationViewModel.messageLiveData().removeObserver(messageLiveDataObserver);
        conversationViewModel.messageUpdateLiveData().removeObserver(messageUpdateLiveDatObserver);
        conversationViewModel.messageRemovedLiveData().removeObserver(messageRemovedLiveDataObserver);
        conversationViewModel.mediaUpdateLiveData().removeObserver(mediaUploadedLiveDataObserver);
        conversationViewModel.clearMessageLiveData().removeObserver(clearMessageLiveDataObserver);
        userViewModel.userInfoLiveData().removeObserver(userInfoUpdateLiveDataObserver);
        inputPanel.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (rootLinearLayout.getCurrentInput() != null) {
            rootLinearLayout.hideAttachedInput(true);
            inputPanel.collapse();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onKeyboardShown() {
        inputPanel.onKeyboardShown();
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onKeyboardHidden() {
        inputPanel.onKeyboardHidden();
    }

    private void reloadMessage() {
        conversationViewModel.getMessages().observe(this, uiMessages -> {
            adapter.setMessages(uiMessages);
            adapter.notifyDataSetChanged();
        });
    }

    private void loadMoreOldMessages() {
        long fromMessageId = Long.MAX_VALUE;
        long fromMessageUid = Long.MAX_VALUE;
        if (adapter.getMessages() != null && !adapter.getMessages().isEmpty()) {
            fromMessageId = adapter.getItem(0).message.messageId;
            fromMessageUid = adapter.getItem(0).message.messageUid;
        }

        conversationViewModel.loadOldMessages(fromMessageId, fromMessageUid, MESSAGE_LOAD_COUNT_PER_TIME)
                .observe(this, uiMessages -> {
                    adapter.addMessagesAtHead(uiMessages);

                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void loadMoreNewMessages() {
        loadingNewMessage = true;
        adapter.showLoadingNewMessageProgressBar();
        conversationViewModel.loadNewMessages(adapter.getItem(adapter.getItemCount() - 2).message.messageId, MESSAGE_LOAD_COUNT_PER_TIME)
                .observe(this, messages -> {
                    loadingNewMessage = false;
                    adapter.dismissLoadingNewMessageProgressBar();

                    if (messages == null || messages.isEmpty()) {
                        shouldContinueLoadNewMessage = false;
                    }
                    if (messages != null && !messages.isEmpty()) {
                        adapter.addMessagesAtTail(messages);
                    }
                });
    }

    private void updateTypingStatusTitle(TypingMessageContent typingMessageContent) {
        String typingDesc = "";
        switch (typingMessageContent.getType()) {
            case TypingMessageContent.TYPING_TEXT:
                typingDesc = "对方正在输入";
                break;
            case TypingMessageContent.TYPING_VOICE:
                typingDesc = "对方正在录音";
                break;
            case TypingMessageContent.TYPING_CAMERA:
                typingDesc = "对方正在拍照";
                break;
            case TypingMessageContent.TYPING_FILE:
                typingDesc = "对方正在发送文件";
                break;
            case TypingMessageContent.TYPING_LOCATION:
                typingDesc = "对方正在发送位置";
                break;
            default:
                typingDesc = "unknown";
                break;
        }
        setTitle(typingDesc);
        handler.postDelayed(resetConversationTitleRunnable, 5000);
    }

    private Runnable resetConversationTitleRunnable = this::resetConversationTitle;

    private void resetConversationTitle() {
        if (!TextUtils.equals(conversationTitle, getTitle())) {
            setTitle(conversationTitle);
            handler.removeCallbacks(resetConversationTitleRunnable);
        }
    }

    @Override
    public void onInputPanelExpanded() {
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onInputPanelCollapsed() {
        // do nothing
    }
}
