/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.channel.ChannelViewModel;
import cn.wildfire.chat.kit.chatroom.ChatRoomViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.multimsg.MultiMessageAction;
import cn.wildfire.chat.kit.conversation.multimsg.MultiMessageActionManager;
import cn.wildfire.chat.kit.conversation.receipt.GroupMessageReceiptActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.PickGroupMemberActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.viewmodel.UserOnlineStateViewModel;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardAwareLinearLayout;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.EnterChannelChatMessageContent;
import cn.wildfirechat.message.LeaveChannelChatMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MultiCallOngoingMessageContent;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratingMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.message.notification.RecallMessageContent;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;
import cn.wildfirechat.utils.WfcUtils;

public class ConversationFragment extends Fragment implements
    KeyboardAwareLinearLayout.OnKeyboardShownListener,
    KeyboardAwareLinearLayout.OnKeyboardHiddenListener,
    ConversationMessageAdapter.OnPortraitClickListener,
    ConversationMessageAdapter.OnPortraitLongClickListener,
    ConversationInputPanel.OnConversationInputPanelStateChangeListener,
    ConversationMessageAdapter.OnMessageCheckListener, ConversationMessageAdapter.OnMessageReceiptClickListener {

    private static final String TAG = "convFragment";

    public static final int REQUEST_PICK_MENTION_CONTACT = 100;
    public static final int REQUEST_CODE_GROUP_VIDEO_CHAT = 101;
    public static final int REQUEST_CODE_GROUP_AUDIO_CHAT = 102;

    private Conversation conversation;
    private boolean loadingNewMessage;
    private boolean shouldContinueLoadNewMessage = false;
    private boolean isPreJoinedChatRoom = true;

    private static final int MESSAGE_LOAD_COUNT_PER_TIME = 20;
    private static final int MESSAGE_LOAD_AROUND = 10;
    private static final long TYPING_INTERNAL = 10 * 1000;

    InputAwareLayout rootLinearLayout;
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;

    RecyclerView ongoingCallRecyclerView;

    ConversationInputPanel inputPanel;

    LinearLayout multiMessageActionContainerLinearLayout;

    LinearLayout unreadCountLinearLayout;
    TextView unreadCountTextView;
    TextView unreadMentionCountTextView;

    private ConversationMessageAdapter adapter;
    private OngoingCallAdapter ongoingCallAdapter;
    private boolean moveToBottom = true;
    private ConversationViewModel conversationViewModel;
    private SettingViewModel settingViewModel;
    private UserOnlineStateViewModel userOnlineStateViewModel;
    private MessageViewModel messageViewModel;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private ChatRoomViewModel chatRoomViewModel;

    private Handler handler;
    private long initialFocusedMessageId;
    private long firstUnreadMessageId;
    // 实现定向消息，主要用户频道主发起针对某个用户的会话
    private String targetUser;
    private CharSequence conversationTitle = "";
    private LinearLayoutManager layoutManager;

    // for group
    private GroupInfo groupInfo;
    private GroupMember groupMember;
    private boolean showGroupMemberName = false;
    private Observer<List<GroupMember>> groupMembersUpdateLiveDataObserver;
    private Observer<List<GroupInfo>> groupInfosUpdateLiveDataObserver;
    private Observer<Object> settingUpdateLiveDataObserver;
    private Map<String, Message> ongoingCalls;

    private Map<String, Message> typingMessageMap;

    private String backgroundImageUri = null;
    private int backgroundImageResId = 0;

    private Observer<UiMessage> messageLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            if (!isMessageInCurrentConversation(uiMessage)) {
                return;
            }
            MessageContent content = uiMessage.message.content;

            if (content instanceof MultiCallOngoingMessageContent) {
                MultiCallOngoingMessageContent ongoingCall = (MultiCallOngoingMessageContent) content;
                AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
                if (ongoingCall.getInitiator().equals(ChatManager.Instance().getUserId())
                    || ongoingCall.getTargets().contains(ChatManager.Instance().getUserId())
                    || (callSession != null && callSession.getState() != AVEngineKit.CallState.Idle)) {
                    return;
                }

                if (ongoingCalls == null) {
                    ongoingCalls = new HashMap<>();
                }
                ongoingCalls.put(ongoingCall.getCallId(), uiMessage.message);

                if (ongoingCalls.size() > 0) {
                    ongoingCallRecyclerView.setVisibility(View.VISIBLE);
                    if (ongoingCallAdapter == null) {
                        ongoingCallAdapter = new OngoingCallAdapter();
                        ongoingCallRecyclerView.setAdapter(ongoingCallAdapter);
                        ongoingCallRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    }
                    ongoingCallAdapter.setOngoingCalls(new ArrayList<>(ongoingCalls.values()));
                } else {
                    ongoingCallRecyclerView.setVisibility(View.GONE);
                    ongoingCallAdapter.setOngoingCalls(null);
                }
                cleanExpiredOngoingCalls();

                return;
            }

            if (isDisplayableMessage(uiMessage) && !(content instanceof RecallMessageContent)) {
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
                if (typingMessageMap == null) {
                    typingMessageMap = new HashMap<>();
                }
                long now = System.currentTimeMillis();
                if (now - uiMessage.message.serverTime + ChatManager.Instance().getServerDeltaTime() < TYPING_INTERNAL) {
                    typingMessageMap.put(uiMessage.message.sender, uiMessage.message);
                }
                updateTypingStatusTitle();
            } else {
                if (uiMessage.message.direction == MessageDirection.Receive && typingMessageMap != null) {
                    typingMessageMap.remove(uiMessage.message.sender);
                }
                updateTypingStatusTitle();
            }

            if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED && uiMessage.message.direction == MessageDirection.Receive) {
                conversationViewModel.clearUnreadStatus(conversation);
            }
        }
    };
    private Observer<UiMessage> messageUpdateLiveDatObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            // 聊天室，对方撤回消息
            if (conversation == null) {
                return;
            }

            // message deleted
            if (uiMessage.message.conversation == null) {
                adapter.removeMessage(uiMessage);
                return;
            }
            if (uiMessage.message.content instanceof RecallMessageContent) {
                Message msg = ChatManager.Instance().getMessageByUid(uiMessage.message.messageUid);
                if (msg == null) {
                    adapter.removeMessage(uiMessage);
                    return;
                }
            }
            if (!isMessageInCurrentConversation(uiMessage)) {
                return;
            }
            if (isDisplayableMessage(uiMessage)) {
                adapter.updateMessage(uiMessage);
            }
            if (uiMessage.progress == 100) {
                uiMessage.progress = 0;
                messageViewModel.playAudioMessage(uiMessage);
            }
            if (uiMessage.audioPlayCompleted) {
                uiMessage.audioPlayCompleted = false;
                if (uiMessage.continuousPlayAudio) {
                    uiMessage.continuousPlayAudio = false;
                    playNextAudioMessage(uiMessage);
                }
            }
        }
    };

    private void playNextAudioMessage(UiMessage uiMessage) {
        List<UiMessage> messages = adapter.getMessages();
        boolean found = false;
        UiMessage toPlayAudioMessage = null;
        for (int i = 0; i < messages.size(); i++) {
            UiMessage uimsg = messages.get(i);
            if (found) {
                if (uimsg.message.content instanceof SoundMessageContent
                    && uimsg.message.direction == MessageDirection.Receive
                    && uimsg.message.status != MessageStatus.Played) {
                    toPlayAudioMessage = uimsg;
                    break;
                }
            } else {
                if (uimsg.message.messageUid == uiMessage.message.messageUid) {
                    found = true;
                }
            }
        }
        if (toPlayAudioMessage != null) {
            File file = DownloadManager.mediaMessageContentFile(toPlayAudioMessage.message);
            if (file == null) {
                return;
            }
            toPlayAudioMessage.continuousPlayAudio = true;
            if (file.exists()) {
                messageViewModel.playAudioMessage(toPlayAudioMessage);
            } else {
                messageViewModel.downloadMedia(toPlayAudioMessage, file);
            }
        }
    }

    private Observer<UiMessage> messageRemovedLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            // 当通过server api删除消息时，只知道消息的uid
            adapter.removeMessage(uiMessage);
        }
    };

    private boolean isDisplayableMessage(UiMessage uiMessage) {
        return uiMessage.message.messageId != 0 || uiMessage.message.content instanceof StreamingTextGeneratingMessageContent;
    }

    private Observer<Pair<String, Long>> messageStartBurnLiveDataObserver = new Observer<Pair<String, Long>>() {
        @Override
        public void onChanged(@Nullable Pair<String, Long> pair) {
            // 当通过server api删除消息时，只知道消息的uid
//            if (uiMessage.message.conversation != null && !isMessageInCurrentConversation(uiMessage)) {
//                return;
//            }
//            if (uiMessage.message.messageId == 0 || isDisplayableMessage(uiMessage)) {
//                adapter.removeMessage(uiMessage);
//            }
//            adapter.removeMessage();
            // TODO
        }
    };

    private Observer<List<Long>> messageBurnedLiveDataObserver = new Observer<List<Long>>() {
        @Override
        public void onChanged(@Nullable List<Long> messageIds) {
            for (int i = 0; i < messageIds.size(); i++) {
                adapter.removeMessageById(messageIds.get(i));
            }
        }
    };

    private Observer<Conversation> clearConversationMessageObserver = new Observer<Conversation>() {
        @Override
        public void onChanged(Conversation conversation) {
            if (conversation.equals(ConversationFragment.this.conversation)) {
                adapter.setMessages(null);
                adapter.notifyDataSetChanged();
            }
        }
    };

    private Observer<List<UserInfo>> userInfoUpdateLiveDataObserver = new Observer<List<UserInfo>>() {
        @Override
        public void onChanged(@Nullable List<UserInfo> userInfos) {
            if (conversation == null) {
                return;
            }
            if (conversation.type == Conversation.ConversationType.Single) {
                conversationTitle = null;
                setTitle();
            }
            int start = layoutManager.findFirstVisibleItemPosition();
            int end = layoutManager.findLastVisibleItemPosition();
            adapter.notifyItemRangeChanged(start, end - start + 1, userInfos);
        }
    };

    private Observer<Map<String, UserOnlineState>> userOnlineStateLiveDataObserver = new Observer<Map<String, UserOnlineState>>() {
        @Override
        public void onChanged(Map<String, UserOnlineState> userOnlineStateMap) {
            if (conversation == null) {
                return;
            }
            if (conversation.type == Conversation.ConversationType.Single) {
                conversationTitle = null;
                setTitle();
            }
        }
    };

    private void initGroupObservers() {
        groupMembersUpdateLiveDataObserver = groupMembers -> {
            if (groupMembers == null || groupInfo == null) {
                return;
            }
            for (GroupMember member : groupMembers) {
                if (member.groupId.equals(groupInfo.target) && member.memberId.equals(userViewModel.getUserId())) {
                    groupMember = member;
                    updateGroupConversationInputStatus();
                    break;
                }
            }
        };

        groupInfosUpdateLiveDataObserver = groupInfos -> {
            if (groupInfo == null || groupInfos == null) {
                return;
            }
            for (GroupInfo info : groupInfos) {
                if (info.target.equals(groupInfo.target)) {
                    groupInfo = info;
                    updateGroupConversationInputStatus();
                    setTitle();
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        };


        groupViewModel.groupInfoUpdateLiveData().observeForever(groupInfosUpdateLiveDataObserver);
        groupViewModel.groupMembersUpdateLiveData().observeForever(groupMembersUpdateLiveDataObserver);
    }

    private void unInitGroupObservers() {
        if (groupViewModel == null) {
            return;
        }
        groupViewModel.groupInfoUpdateLiveData().removeObserver(groupInfosUpdateLiveDataObserver);
        groupViewModel.groupMembersUpdateLiveData().removeObserver(groupMembersUpdateLiveDataObserver);
    }

    private boolean isMessageInCurrentConversation(UiMessage message) {
        if (conversation == null || message == null || message.message == null) {
            return false;
        }
        return conversation.equals(message.message.conversation);
    }

    public ConversationInputPanel getConversationInputPanel() {
        return inputPanel;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversation_activity, container, false);
        bindViews(view);
        bindEvents(view);
        initView();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.unreadCountTextView).setOnClickListener(_v -> onUnreadCountTextViewClick());
    }

    private void bindViews(View view) {
        rootLinearLayout = view.findViewById(R.id.rootLinearLayout);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.msgRecyclerView);
        ongoingCallRecyclerView = view.findViewById(R.id.ongoingCallRecyclerView);
        inputPanel = view.findViewById(R.id.inputPanelFrameLayout);
        multiMessageActionContainerLinearLayout = view.findViewById(R.id.multiMessageActionContainerLinearLayout);
        unreadCountLinearLayout = view.findViewById(R.id.unreadCountLinearLayout);
        unreadCountTextView = view.findViewById(R.id.unreadCountTextView);
        unreadMentionCountTextView = view.findViewById(R.id.unreadMentionCountTextView);

        view.findViewById(R.id.contentLayout).setOnTouchListener((v, event) -> ConversationFragment.this.onTouch(v, event));
        recyclerView.setOnTouchListener((v, event) -> ConversationFragment.this.onTouch(v, event));

    }

    @Override
    public void onResume() {
        super.onResume();
        if (conversationViewModel != null && conversation != null) {
            conversationViewModel.clearUnreadStatus(conversation);
        }
    }

    public void setupConversation(Conversation conversation, String title, long focusMessageId, String target) {
        setupConversation(conversation, title, focusMessageId, target, false);
    }

    public void setupConversation(Conversation conversation, String title, long focusMessageId, String target, boolean isJoinedChatRoom) {
        if (this.conversation != null) {
            if ((this.conversation.type == Conversation.ConversationType.Single && !ChatManager.Instance().isMyFriend(this.conversation.target))
                || this.conversation.type == Conversation.ConversationType.Group) {
                userOnlineStateViewModel.unwatchOnlineState(this.conversation.type.getValue(), new String[]{this.conversation.target});
            }
            this.adapter = new ConversationMessageAdapter(this);
            this.recyclerView.setAdapter(this.adapter);
        }

        // TODO 要支持在线状态是才监听
        if ((conversation.type == Conversation.ConversationType.Single && !ChatManager.Instance().isMyFriend(conversation.target))
            || conversation.type == Conversation.ConversationType.Group) {
            userOnlineStateViewModel.watchUserOnlineState(conversation.type.getValue(), new String[]{conversation.target});
        }

        this.conversation = conversation;
        this.conversationTitle = title;
        this.initialFocusedMessageId = focusMessageId;
        this.targetUser = target;
        this.isPreJoinedChatRoom = isJoinedChatRoom;
        setupConversation(conversation);
    }

    private void initView() {
        handler = new Handler();
        rootLinearLayout.addOnKeyboardShownListener(this);

        swipeRefreshLayout.setOnRefreshListener(this::loadMoreOldMessages);

        // message list
        adapter = new ConversationMessageAdapter(this);
        adapter.setOnPortraitClickListener(this);
        adapter.setOnMessageReceiptClickListener(this);
        adapter.setOnPortraitLongClickListener(this);
        adapter.setOnMessageCheckListener(this);
        layoutManager = new LinearLayoutManager(getActivity());
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
                    if ((initialFocusedMessageId != -1 || firstUnreadMessageId != 0) && !loadingNewMessage && shouldContinueLoadNewMessage) {
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

        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        conversationViewModel.clearConversationMessageLiveData().observeForever(clearConversationMessageObserver);
        conversationViewModel.secretConversationStateLiveData().observe(getViewLifecycleOwner(), new Observer<Pair<String, ChatManager.SecretChatState>>() {
            @Override
            public void onChanged(Pair<String, ChatManager.SecretChatState> stringSecretChatStatePair) {
                if (conversation != null && conversation.type == Conversation.ConversationType.SecretChat && conversation.target.equals(stringSecretChatStatePair.first)) {
                    reloadMessage();
                }
            }
        });
        messageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);

        messageViewModel.messageLiveData().observeForever(messageLiveDataObserver);
        messageViewModel.messageUpdateLiveData().observeForever(messageUpdateLiveDatObserver);
        messageViewModel.messageRemovedLiveData().observeForever(messageRemovedLiveDataObserver);
        messageViewModel.messageStartBurnLiveData().observeForever(messageStartBurnLiveDataObserver);
        messageViewModel.messageBurnedLiveData().observeForever(messageBurnedLiveDataObserver);

        messageViewModel.messageReadLiveData().observe(getActivity(), readEntries -> {
            if (conversation == null) {
                return;
            }
            Map<String, Long> convReadEntities = ChatManager.Instance().getConversationRead(conversation);
            adapter.setReadEntries(convReadEntities);
        });

        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observeForever(userInfoUpdateLiveDataObserver);

        settingUpdateLiveDataObserver = o -> {
            if (groupInfo == null) {
                return;
            }
            boolean show = !"1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));
            if (showGroupMemberName != show) {
                showGroupMemberName = show;
                adapter.notifyDataSetChanged();
            }
            reloadMessage();
        };

        settingViewModel = ViewModelProviders.of(this).get(SettingViewModel.class);
        settingViewModel.settingUpdatedLiveData().observeForever(settingUpdateLiveDataObserver);

        userOnlineStateViewModel = ViewModelProviders.of(this).get(UserOnlineStateViewModel.class);
        userOnlineStateViewModel.getUserOnlineStateLiveData().observe(getViewLifecycleOwner(), userOnlineStateLiveDataObserver);


        if (backgroundImageUri != null) {
            setConversationBackgroundImage(backgroundImageUri);
        } else if (backgroundImageResId != 0) {
            setConversationBackgroundImage(backgroundImageResId);
        }
    }

    private void setupConversation(Conversation conversation) {
        if (conversation.type == Conversation.ConversationType.Group) {
            groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            initGroupObservers();
            groupViewModel.getGroupMembersLiveData(conversation.target, true);
            groupInfo = groupViewModel.getGroupInfo(conversation.target, true);
            groupMember = groupViewModel.getGroupMember(conversation.target, userViewModel.getUserId());
            showGroupMemberName = !"1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));

            updateGroupConversationInputStatus();
        }
        userViewModel.getUserInfo(userViewModel.getUserId(), true);

        inputPanel.setupConversation(conversation, this.targetUser);

        if (conversation.type != Conversation.ConversationType.ChatRoom) {
            loadMessage(initialFocusedMessageId);
        }
        if (conversation.type == Conversation.ConversationType.ChatRoom) {
            chatRoomViewModel = ViewModelProviders.of(this).get(ChatRoomViewModel.class);
            if (!isPreJoinedChatRoom) {
                joinChatRoom();
            } else {
                loadMoreOldMessages(true);
            }
        } else if (conversation.type == Conversation.ConversationType.Channel) {
            EnterChannelChatMessageContent content = new EnterChannelChatMessageContent();
            messageViewModel.sendMessage(conversation, content);
        }

        ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
        int unreadCount = conversationInfo.unreadCount.unread + conversationInfo.unreadCount.unreadMention + conversationInfo.unreadCount.unreadMentionAll;
        if (unreadCount > 10 && unreadCount < 300) {
            firstUnreadMessageId = ChatManager.Instance().getFirstUnreadMessageId(conversation);
            showUnreadMessageCountLabel(unreadCount);
        }
        conversationViewModel.clearUnreadStatus(conversation);

        ongoingCalls = null;

        setTitle();
    }

    public void setConversationBackgroundImage(String uri) {
        if (rootLinearLayout == null) {
            this.backgroundImageUri = uri;
            return;
        }
        ScrollView scrollView = rootLinearLayout.findViewById(R.id.conversationBackgroundScrollView);
        ImageView imageView = rootLinearLayout.findViewById(R.id.conversationBackgroundImageView);
        scrollView.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).into(imageView);
    }

    public void setConversationBackgroundImage(int drawableId) {
        if (rootLinearLayout == null) {
            this.backgroundImageResId = drawableId;
            return;
        }
        ScrollView scrollView = rootLinearLayout.findViewById(R.id.conversationBackgroundScrollView);
        ImageView imageView = rootLinearLayout.findViewById(R.id.conversationBackgroundImageView);
        scrollView.setVisibility(View.VISIBLE);
        Glide.with(this).load(backgroundImageResId).into(imageView);
    }

    private void loadMessage(long focusMessageId) {

        MutableLiveData<List<UiMessage>> messages;
        if (focusMessageId != -1) {
            shouldContinueLoadNewMessage = true;
            messages = conversationViewModel.loadAroundMessages(conversation, targetUser, focusMessageId, MESSAGE_LOAD_AROUND);
        } else {
            messages = conversationViewModel.getMessages(conversation, targetUser, false);
        }

        // load message
        swipeRefreshLayout.setRefreshing(true);
        adapter.setReadEntries(ChatManager.Instance().getConversationRead(conversation));
        messages.observe(this, uiMessages -> {
            swipeRefreshLayout.setRefreshing(false);
            adapter.setMessages(uiMessages);
            adapter.notifyDataSetChanged();

            if (adapter.getItemCount() > 1) {
                int initialMessagePosition;
                if (focusMessageId != -1) {
                    initialMessagePosition = adapter.getMessagePosition(focusMessageId);
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
    }

    private void updateGroupConversationInputStatus() {
        if (groupInfo == null || groupMember == null) {
            return;
        }
        if (groupInfo.mute == 1
            && groupMember.type != GroupMember.GroupMemberType.Owner
            && groupMember.type != GroupMember.GroupMemberType.Manager
            && groupMember.type != GroupMember.GroupMemberType.Allowed) {
            inputPanel.disableInput("全员禁言中");
        } else if (groupMember.type == GroupMember.GroupMemberType.Muted) {
            inputPanel.disableInput("你已被禁言");
        } else if (groupInfo.deleted == 1) {
            inputPanel.disableInput("群组已被解散");
        } else {
            inputPanel.enableInput();
        }
    }

    void onUnreadCountTextViewClick() {
        hideUnreadMessageCountLabel();
        shouldContinueLoadNewMessage = true;
        loadMessage(firstUnreadMessageId);
    }

    private void showUnreadMessageCountLabel(int count) {
        unreadCountLinearLayout.setVisibility(View.VISIBLE);
        unreadCountTextView.setVisibility(View.VISIBLE);
        unreadCountTextView.setText(count + "条消息");
    }

    private void hideUnreadMessageCountLabel() {
        unreadCountTextView.setVisibility(View.GONE);
    }

    private void showUnreadMentionCountLabel(int count) {
        unreadCountLinearLayout.setVisibility(View.VISIBLE);
        unreadMentionCountTextView.setVisibility(View.VISIBLE);
        unreadMentionCountTextView.setText(count + "条@消息");
    }

    private void hideUnreadMentionCountLabel() {
        unreadMentionCountTextView.setVisibility(View.GONE);
    }

    private void joinChatRoom() {
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
                            content.tip = String.format(welcome, userViewModel.getUserDisplayNameEx(userInfo));
                        } else {
                            content.tip = String.format(welcome, "<" + userId + ">");
                        }
                        handler.postDelayed(() -> {
                            messageViewModel.sendMessage(conversation, content);
                        }, 1000);
                        setChatRoomConversationTitle();

                    } else {
                        Toast.makeText(getActivity(), "加入聊天室失败", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
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
            content.tip = String.format(welcome, userViewModel.getUserDisplayNameEx(userInfo));
        } else {
            content.tip = String.format(welcome, "<" + userId + ">");
        }
        Message message = new Message();
        message.conversation = conversation;
        message.content = content;
        messageViewModel.sendMessageEx(message).observe(this, voidOperateResult -> chatRoomViewModel.quitChatRoom(conversation.target));
    }

    private void setChatRoomConversationTitle() {
        chatRoomViewModel.getChatRoomInfo(conversation.target, 0)
            .observe(this, chatRoomInfoOperateResult -> {
                if (chatRoomInfoOperateResult.isSuccess()) {
                    ChatRoomInfo chatRoomInfo = chatRoomInfoOperateResult.getResult();
                    conversationTitle = chatRoomInfo.title;
                    setActivityTitle(conversationTitle);
                }
            });
    }

    private void setTitle() {
        if (!TextUtils.isEmpty(conversationTitle)) {
            setActivityTitle(conversationTitle);
        }

        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(conversation.target, false);
            conversationTitle = userViewModel.getUserDisplayNameEx(userInfo);

            UserOnlineState userOnlineState = ChatManager.Instance().getUserOnlineStateMap().get(userInfo.uid);
            if (userOnlineState != null) {
                String onlineDesc = userOnlineState.desc();
                if (!TextUtils.isEmpty(onlineDesc)) {
                    conversationTitle += " (" + onlineDesc + ")";
                }
            }
        } else if (conversation.type == Conversation.ConversationType.Group) {
            if (groupInfo != null) {
                String tmpTitle = ChatManager.Instance().getGroupDisplayName(groupInfo);
                if (WfcUtils.isExternalTarget(groupInfo.target)) {
                    conversationTitle = WfcUtils.buildExternalDisplayNameSpannableString(tmpTitle, 14);
                } else {
                    conversationTitle = tmpTitle;
                }
            }
        } else if (conversation.type == Conversation.ConversationType.Channel) {
            ChannelViewModel channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
            ChannelInfo channelInfo = channelViewModel.getChannelInfo(conversation.target, false);
            if (channelInfo != null) {
                conversationTitle = channelInfo.name;
            }

            if (!TextUtils.isEmpty(targetUser)) {
                UserInfo channelPrivateChatUserInfo = userViewModel.getUserInfo(targetUser, false);
                if (channelPrivateChatUserInfo != null) {
                    conversationTitle = userViewModel.getUserDisplayNameEx(channelPrivateChatUserInfo) + "@" + conversationTitle;
                } else {
                    conversationTitle = "<" + targetUser + ">" + "@" + conversationTitle;
                }
            }
        } else if (conversation.type == Conversation.ConversationType.SecretChat) {
            String userId = ChatManager.Instance().getSecretChatInfo(conversation.target).getUserId();
            UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(userId, false);
            conversationTitle = userViewModel.getUserDisplayName(userInfo);
        }

        setActivityTitle(conversationTitle);
    }

    private void setActivityTitle(CharSequence title) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle(title);
        }
    }

    boolean onTouch(View view, MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN && inputPanel.extension.canHideOnScroll()) {
//            inputPanel.collapse();
//        }
        inputPanel.closeConversationInputPanel();
        return false;
    }

    @Override
    public void onPortraitClick(UserInfo userInfo) {
        if (conversation.type == Conversation.ConversationType.Channel && TextUtils.isEmpty(targetUser)) {
            Intent intent = ConversationActivity.buildConversationIntent(getActivity(), this.conversation, userInfo.uid, -1);
            startActivity(intent);
            return;
        }

        if (groupInfo != null && groupInfo.privateChat == 1) {
            boolean allowPrivateChat = false;
            GroupMember groupMember = groupViewModel.getGroupMember(groupInfo.target, userViewModel.getUserId());
            if (groupMember != null && groupMember.type == GroupMember.GroupMemberType.Normal) {
                GroupMember targetGroupMember = groupViewModel.getGroupMember(groupInfo.target, userInfo.uid);
                if (targetGroupMember != null && (targetGroupMember.type == GroupMember.GroupMemberType.Owner || targetGroupMember.type == GroupMember.GroupMemberType.Manager)) {
                    allowPrivateChat = true;
                }
            } else if (groupMember != null && (groupMember.type == GroupMember.GroupMemberType.Owner || groupMember.type == GroupMember.GroupMemberType.Manager)) {
                allowPrivateChat = true;
            }

            if (!allowPrivateChat) {
                Toast.makeText(getActivity(), "禁止群成员私聊", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (userInfo.deleted == 0) {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            intent.putExtra("userInfo", userInfo);
            if (conversation.type == Conversation.ConversationType.Group) {
                intent.putExtra("groupId", conversation.target);
            }
            startActivity(intent);
        }
    }

    @Override
    public void onPortraitLongClick(UserInfo userInfo) {
        if (conversation.type == Conversation.ConversationType.Group) {
            SpannableString spannableString = mentionSpannable(userInfo);
            int position = inputPanel.editText.getSelectionEnd();
            inputPanel.editText.getEditableText().append(" ");
            inputPanel.editText.getEditableText().replace(position, position + 1, spannableString);
        } else {
            inputPanel.editText.getEditableText().append(userViewModel.getUserDisplayNameEx(userInfo));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode >= ConversationExtension.REQUEST_CODE_MIN) {
            boolean result = inputPanel.extension.onActivityResult(requestCode, resultCode, data);
            if (result) {
                return;
            }
            Log.d(TAG, "extension can not handle " + requestCode);
        }
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_MENTION_CONTACT) {
                boolean isMentionAll = data.getBooleanExtra("mentionAll", false);
                SpannableString spannableString;
                if (isMentionAll) {
                    spannableString = mentionAllSpannable();
                } else {
                    String userId = data.getStringExtra("userId");
                    UserInfo userInfo = userViewModel.getUserInfo(userId, this.groupInfo.target, false);
                    spannableString = mentionSpannable(userInfo);
                }
                int position = inputPanel.editText.getSelectionEnd();
                position = position > 0 ? position - 1 : 0;
                inputPanel.editText.getEditableText().replace(position, position + 1, spannableString);

            } else if (requestCode == REQUEST_CODE_GROUP_AUDIO_CHAT || requestCode == REQUEST_CODE_GROUP_VIDEO_CHAT) {
                onPickGroupMemberToVoipChat(data, requestCode == REQUEST_CODE_GROUP_AUDIO_CHAT);
            }
        }
    }

    private SpannableString mentionAllSpannable() {
        String text = "@所有人 ";
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new MentionSpan(true), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private SpannableString mentionSpannable(UserInfo userInfo) {
        String text = "@" + ChatManager.Instance().getGroupMemberDisplayName(userInfo) + " ";
        SpannableString spannableString = new SpannableString(text);
        spannableString.setSpan(new MentionSpan(userInfo.uid), 0, spannableString.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    @Override
    public void onPause() {
        super.onPause();
        inputPanel.onActivityPause();
        messageViewModel.stopPlayAudio();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (conversation == null) {
            return;
        }

        if ((conversation.type == Conversation.ConversationType.Single && !ChatManager.Instance().isMyFriend(conversation.target))
            || conversation.type == Conversation.ConversationType.Group) {
            userOnlineStateViewModel.unwatchOnlineState(conversation.type.getValue(), new String[]{conversation.target});
        }

        if (conversation.type == Conversation.ConversationType.ChatRoom) {
            if (!isPreJoinedChatRoom) {
                quitChatRoom();
            }
        } else if (conversation.type == Conversation.ConversationType.Channel) {
            LeaveChannelChatMessageContent content = new LeaveChannelChatMessageContent();
            messageViewModel.sendMessage(conversation, content);
        }

        messageViewModel.messageLiveData().removeObserver(messageLiveDataObserver);
        messageViewModel.messageUpdateLiveData().removeObserver(messageUpdateLiveDatObserver);
        messageViewModel.messageRemovedLiveData().removeObserver(messageRemovedLiveDataObserver);
        userViewModel.userInfoLiveData().removeObserver(userInfoUpdateLiveDataObserver);
        conversationViewModel.clearConversationMessageLiveData().removeObserver(clearConversationMessageObserver);
        settingViewModel.settingUpdatedLiveData().removeObserver(settingUpdateLiveDataObserver);

        unInitGroupObservers();
        inputPanel.onDestroy();

        // 退出密聊时，清空相关临时文件
        if (conversation.type == Conversation.ConversationType.SecretChat) {
            List<UiMessage> messages = adapter.getMessages();
            if (messages != null) {
                for (UiMessage uiMsg : messages) {
                    File file = DownloadManager.mediaMessageContentFile(uiMsg.message);
                    if (file != null && file.exists()) {
                        file.delete();
                    }
                }
            }
        }
    }

    boolean onBackPressed() {
        boolean consumed = true;
        if (rootLinearLayout.getCurrentInput() != null) {
            rootLinearLayout.hideAttachedInput(true);
            inputPanel.closeConversationInputPanel();
        } else if (multiMessageActionContainerLinearLayout.getVisibility() == View.VISIBLE) {
            toggleConversationMode();
        } else {
            consumed = false;
        }
        return consumed;
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
        conversationViewModel.getMessages(conversation, targetUser, true).observe(this, uiMessages -> {
            adapter.setMessages(uiMessages);
            adapter.notifyDataSetChanged();
        });
    }

    private void loadMoreOldMessages() {
        loadMoreOldMessages(false);
    }

    private void loadMoreOldMessages(boolean scrollToBottom) {

        conversationViewModel.loadOldMessages(conversation, targetUser, adapter.oldestMessageId, adapter.oldestMessageUid, MESSAGE_LOAD_COUNT_PER_TIME, true)
            .observe(this, uiMessages -> {
                adapter.addMessagesAtHead(uiMessages);

                swipeRefreshLayout.setRefreshing(false);
                if (scrollToBottom) {
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            });
    }

    private void loadMoreNewMessages() {
        loadingNewMessage = true;
        adapter.showLoadingNewMessageProgressBar();
        conversationViewModel.loadNewMessages(conversation, targetUser, adapter.getItem(adapter.getItemCount() - 2).message.messageId, MESSAGE_LOAD_COUNT_PER_TIME)
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

    private void updateTypingStatusTitle() {
        if (this.typingMessageMap == null || this.conversation.type == Conversation.ConversationType.Channel || this.conversation.type == Conversation.ConversationType.ChatRoom) {
            return;
        }
        this.checkUserTyping();
        String typingDesc = "";
        if (typingMessageMap.size() == 0) {
            this.resetConversationTitle();
            return;
        } else if (typingMessageMap.size() == 1) {
            Set<String> users = typingMessageMap.keySet();
            String userId = users.toArray(new String[1])[0];
            UserInfo userInfo;
            if (conversation.type == Conversation.ConversationType.Group) {
                userInfo = ChatManager.Instance().getUserInfo(userId, conversation.target, false);
                String userName = "有人";
                if (!TextUtils.isEmpty(userInfo.friendAlias)) {
                    userName = userInfo.friendAlias;
                } else if (!TextUtils.isEmpty(userInfo.groupAlias)) {
                    userName = userInfo.groupAlias;
                } else if (!TextUtils.isEmpty(userInfo.displayName)) {
                    userName = userInfo.displayName;
                }

                TypingMessageContent typingMessageContent = (TypingMessageContent) typingMessageMap.get(userId).content;
                switch (typingMessageContent.getTypingType()) {
                    case TypingMessageContent.TYPING_TEXT:
                        typingDesc = "正在输入";
                        break;
                    case TypingMessageContent.TYPING_VOICE:
                        typingDesc = "正在录音";
                        break;
                    case TypingMessageContent.TYPING_CAMERA:
                        typingDesc = "正在拍照";
                        break;
                    case TypingMessageContent.TYPING_FILE:
                        typingDesc = "正在发送文件";
                        break;
                    case TypingMessageContent.TYPING_LOCATION:
                        typingDesc = "正在发送位置";
                        break;
                    default:
                        typingDesc = "unknown";
                        break;
                }
                typingDesc = userName + " " + typingDesc;
            } else {
                TypingMessageContent typingMessageContent = (TypingMessageContent) typingMessageMap.get(userId).content;
                switch (typingMessageContent.getTypingType()) {
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
            }
        } else {
            typingDesc = typingMessageMap.size() + "人正在输入";
        }
        setActivityTitle(typingDesc);
        handler.postDelayed(this::updateTypingStatusTitle, 5000);
    }

    private void checkUserTyping() {
        Set<Map.Entry<String, Message>> entries = typingMessageMap.entrySet();
        Iterator<Map.Entry<String, Message>> iterator = entries.iterator();
        long now = System.currentTimeMillis();
        while (iterator.hasNext()) {
            Map.Entry<String, Message> next = iterator.next();
            Message msg = next.getValue();
            if (now - msg.serverTime + ChatManager.Instance().getServerDeltaTime() > TYPING_INTERNAL) {
                iterator.remove();
            }
        }
    }

    private void resetConversationTitle() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (!TextUtils.equals(conversationTitle, getActivity().getTitle())) {
            setActivityTitle(conversationTitle);
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

    public void toggleMultiMessageMode(UiMessage message) {
        inputPanel.setVisibility(View.GONE);
        message.isChecked = true;
        adapter.setMode(ConversationMessageAdapter.MODE_CHECKABLE);
        adapter.notifyDataSetChanged();
        multiMessageActionContainerLinearLayout.setVisibility(View.VISIBLE);
        setupMultiMessageAction();
    }

    public void toggleConversationMode() {
        inputPanel.setVisibility(View.VISIBLE);
        multiMessageActionContainerLinearLayout.setVisibility(View.GONE);
        adapter.setMode(ConversationMessageAdapter.MODE_NORMAL);
        adapter.clearMessageCheckStatus();
        adapter.notifyDataSetChanged();
    }

    public void setInputText(String text) {
        inputPanel.setInputText(text);
    }

    private void setupMultiMessageAction() {
        multiMessageActionContainerLinearLayout.removeAllViews();
        List<MultiMessageAction> actions = MultiMessageActionManager.getInstance().getConversationActions(conversation);
        int width = getResources().getDisplayMetrics().widthPixels;

        for (MultiMessageAction action : actions) {
            action.onBind(this, conversation);
            ImageView imageView = new ImageView(getActivity());
            imageView.setImageResource(action.iconResId());


            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width / actions.size(), LinearLayout.LayoutParams.WRAP_CONTENT);
            multiMessageActionContainerLinearLayout.addView(imageView, layoutParams);
            ViewGroup.LayoutParams p = imageView.getLayoutParams();
            p.height = 70;
            imageView.requestLayout();

            imageView.setOnClickListener(v -> {
                List<UiMessage> checkedMessages = adapter.getCheckedMessages();
                if (action.confirm()) {
                    new MaterialDialog.Builder(getActivity()).content(action.confirmPrompt())
                        .negativeText("取消")
                        .positiveText("确认")
                        .onPositive((dialog, which) -> {
                            action.onClick(checkedMessages);
                            toggleConversationMode();
                        })
                        .build()
                        .show();

                } else {
                    action.onClick(checkedMessages);
                    toggleConversationMode();
                }
            });
        }
    }

    @Override
    public void onMessageCheck(UiMessage message, boolean checked) {
        List<UiMessage> checkedMessages = adapter.getCheckedMessages();
        setAllClickableChildViewState(multiMessageActionContainerLinearLayout, checkedMessages.size() > 0);
    }

    private void setAllClickableChildViewState(View view, boolean enable) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setAllClickableChildViewState(((ViewGroup) view).getChildAt(i), enable);
            }
        }
        if (view.isClickable()) {
            view.setEnabled(enable);
        }
    }

    public void pickGroupMemberToVoipChat(boolean isAudioOnly) {
        Intent intent = new Intent(getActivity(), PickGroupMemberActivity.class);
        GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
        intent.putExtra("groupInfo", groupInfo);
        int maxCount = AVEngineKit.isSupportMultiCall() ? (isAudioOnly ? AVEngineKit.MAX_AUDIO_PARTICIPANT_COUNT - 1 : AVEngineKit.MAX_VIDEO_PARTICIPANT_COUNT - 1) : 1;
        intent.putExtra("maxCount", maxCount);
        startActivityForResult(intent, isAudioOnly ? REQUEST_CODE_GROUP_AUDIO_CHAT : REQUEST_CODE_GROUP_VIDEO_CHAT);
    }

    private void onPickGroupMemberToVoipChat(Intent intent, boolean isAudioOnly) {
        List<String> memberIds = intent.getStringArrayListExtra(PickGroupMemberActivity.EXTRA_RESULT);
        if (memberIds != null && memberIds.size() > 0) {
            if (AVEngineKit.isSupportMultiCall()) {
                WfcUIKit.multiCall(getActivity(), conversation.target, memberIds, isAudioOnly);
            } else {
                WfcUIKit.singleCall(getActivity(), memberIds.get(0), isAudioOnly);
            }
        }
    }

    @Override
    public void onMessageReceiptCLick(Message message) {
        Intent intent = new Intent(getActivity(), GroupMessageReceiptActivity.class);
        intent.putExtra("message", message);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    private void cleanExpiredOngoingCalls() {
        for (Iterator<Map.Entry<String, Message>> it = ongoingCalls.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Message> entry = it.next();
            if (System.currentTimeMillis() - (entry.getValue().serverTime + ChatManager.Instance().getServerDeltaTime()) > 3000) {
                it.remove();
            }
        }
        if (ongoingCalls.size() > 0) {
            handler.postDelayed(this::cleanExpiredOngoingCalls, 1000);
        } else {
            ongoingCallAdapter.setOngoingCalls(null);
            ongoingCallRecyclerView.setVisibility(View.GONE);
        }
    }
}
