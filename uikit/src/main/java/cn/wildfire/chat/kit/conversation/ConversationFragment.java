/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.channel.ChannelViewModel;
import cn.wildfire.chat.kit.chatroom.ChatRoomViewModel;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.multimsg.MultiMessageAction;
import cn.wildfire.chat.kit.conversation.multimsg.MultiMessageActionManager;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.group.PickGroupMemberActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardAwareLinearLayout;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.notification.TipNotificationContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChatRoomInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.UserSettingScope;

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

    private static final int MESSAGE_LOAD_COUNT_PER_TIME = 20;
    private static final int MESSAGE_LOAD_AROUND = 10;

    @BindView(R2.id.rootLinearLayout)
    InputAwareLayout rootLinearLayout;
    @BindView(R2.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R2.id.msgRecyclerView)
    RecyclerView recyclerView;

    @BindView(R2.id.inputPanelFrameLayout)
    ConversationInputPanel inputPanel;

    @BindView(R2.id.multiMessageActionContainerLinearLayout)
    LinearLayout multiMessageActionContainerLinearLayout;

    @BindView(R2.id.unreadCountLinearLayout)
    LinearLayout unreadCountLinearLayout;
    @BindView(R2.id.unreadCountTextView)
    TextView unreadCountTextView;
    @BindView(R2.id.unreadMentionCountTextView)
    TextView unreadMentionCountTextView;

    private ConversationMessageAdapter adapter;
    private boolean moveToBottom = true;
    private ConversationViewModel conversationViewModel;
    private SettingViewModel settingViewModel;
    private MessageViewModel messageViewModel;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private ChatRoomViewModel chatRoomViewModel;

    private Handler handler;
    private long initialFocusedMessageId;
    private long firstUnreadMessageId;
    // 用户channel主发起，针对某个用户的会话
    private String channelPrivateChatUser;
    private String conversationTitle = "";
    private LinearLayoutManager layoutManager;

    // for group
    private GroupInfo groupInfo;
    private GroupMember groupMember;
    private boolean showGroupMemberName = false;
    private Observer<List<GroupMember>> groupMembersUpdateLiveDataObserver;
    private Observer<List<GroupInfo>> groupInfosUpdateLiveDataObserver;
    private Observer<Object> settingUpdateLiveDataObserver;

    private Observer<UiMessage> messageLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            if (!isMessageInCurrentConversation(uiMessage)) {
                return;
            }
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

            if (getLifecycle().getCurrentState() == Lifecycle.State.RESUMED && uiMessage.message.direction == MessageDirection.Receive) {
                conversationViewModel.clearUnreadStatus(conversation);
            }
        }
    };
    private Observer<UiMessage> messageUpdateLiveDatObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            if (!isMessageInCurrentConversation(uiMessage)) {
                return;
            }
            if (isDisplayableMessage(uiMessage)) {
                adapter.updateMessage(uiMessage);
            }
        }
    };

    private Observer<UiMessage> messageRemovedLiveDataObserver = new Observer<UiMessage>() {
        @Override
        public void onChanged(@Nullable UiMessage uiMessage) {
            // 当通过server api删除消息时，只知道消息的uid
            if (uiMessage.message.conversation != null && !isMessageInCurrentConversation(uiMessage)) {
                return;
            }
            if (uiMessage.message.messageId == 0 || isDisplayableMessage(uiMessage)) {
                adapter.removeMessage(uiMessage);
            }
        }
    };

    private boolean isDisplayableMessage(UiMessage uiMessage) {
        return uiMessage.message.messageId != 0;
    }

    private Observer<Map<String, String>> mediaUploadedLiveDataObserver = new Observer<Map<String, String>>() {
        @Override
        public void onChanged(@Nullable Map<String, String> stringStringMap) {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("sticker", Context.MODE_PRIVATE);
            for (Map.Entry<String, String> entry : stringStringMap.entrySet()) {
                sharedPreferences.edit()
                    .putString(entry.getKey(), entry.getValue())
                    .apply();
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

    private void initGroupObservers() {
        groupMembersUpdateLiveDataObserver = groupMembers -> {
            if (groupMembers == null || groupInfo == null) {
                return;
            }
            for (GroupMember member : groupMembers) {
                if (member.groupId.equals(groupInfo.target) && member.memberId.equals(userViewModel.getUserId())) {
                    groupMember = member;
                    updateGroupMuteStatus();
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
                    updateGroupMuteStatus();
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
        ButterKnife.bind(this, view);
        initView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (conversationViewModel != null && conversation != null) {
            conversationViewModel.clearUnreadStatus(conversation);
        }
    }

    public void setupConversation(Conversation conversation, String title, long focusMessageId, String target) {
        this.conversation = conversation;
        this.conversationTitle = title;
        this.initialFocusedMessageId = focusMessageId;
        this.channelPrivateChatUser = target;
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

        settingViewModel = ViewModelProviders.of(this).get(SettingViewModel.class);
        conversationViewModel = WfcUIKit.getAppScopeViewModel(ConversationViewModel.class);
        conversationViewModel.clearConversationMessageLiveData().observeForever(clearConversationMessageObserver);
        messageViewModel = ViewModelProviders.of(this).get(MessageViewModel.class);

        messageViewModel.messageLiveData().observeForever(messageLiveDataObserver);
        messageViewModel.messageUpdateLiveData().observeForever(messageUpdateLiveDatObserver);
        messageViewModel.messageRemovedLiveData().observeForever(messageRemovedLiveDataObserver);
        messageViewModel.mediaUpdateLiveData().observeForever(mediaUploadedLiveDataObserver);

        messageViewModel.messageDeliverLiveData().observe(getActivity(), stringLongMap -> {
            if (conversation == null) {
                return;
            }
            Map<String, Long> deliveries = ChatManager.Instance().getMessageDelivery(conversation);
            adapter.setDeliveries(deliveries);
        });

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
            boolean show = "1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));
            if (showGroupMemberName != show) {
                showGroupMemberName = show;
                adapter.notifyDataSetChanged();
            }
            reloadMessage();
        };
        settingViewModel.settingUpdatedLiveData().observeForever(settingUpdateLiveDataObserver);
    }

    private void setupConversation(Conversation conversation) {

        if (conversation.type == Conversation.ConversationType.Group) {
            groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            initGroupObservers();
            groupViewModel.getGroupMembers(conversation.target, true);
            groupInfo = groupViewModel.getGroupInfo(conversation.target, true);
            groupMember = groupViewModel.getGroupMember(conversation.target, userViewModel.getUserId());
            showGroupMemberName = "1".equals(userViewModel.getUserSetting(UserSettingScope.GroupHideNickname, groupInfo.target));

            updateGroupMuteStatus();
        }
        userViewModel.getUserInfo(userViewModel.getUserId(), true);

        inputPanel.setupConversation(conversation);

        if (conversation.type != Conversation.ConversationType.ChatRoom) {
            loadMessage(initialFocusedMessageId);
        } else {
            joinChatRoom();
        }

        ConversationInfo conversationInfo = ChatManager.Instance().getConversation(conversation);
        int unreadCount = conversationInfo.unreadCount.unread + conversationInfo.unreadCount.unreadMention + conversationInfo.unreadCount.unreadMentionAll;
        if (unreadCount > 10 && unreadCount < 300) {
            firstUnreadMessageId = ChatManager.Instance().getFirstUnreadMessageId(conversation);
            showUnreadMessageCountLabel(unreadCount);
        }
        conversationViewModel.clearUnreadStatus(conversation);

        setTitle();
    }

    private void loadMessage(long focusMessageId) {

        MutableLiveData<List<UiMessage>> messages;
        if (focusMessageId != -1) {
            shouldContinueLoadNewMessage = true;
            messages = conversationViewModel.loadAroundMessages(conversation, channelPrivateChatUser, focusMessageId, MESSAGE_LOAD_AROUND);
        } else {
            messages = conversationViewModel.getMessages(conversation, channelPrivateChatUser);
        }

        // load message
        swipeRefreshLayout.setRefreshing(true);
        adapter.setDeliveries(ChatManager.Instance().getMessageDelivery(conversation));
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

    private void updateGroupMuteStatus() {
        if (groupInfo == null || groupMember == null) {
            return;
        }
        if (groupInfo.mute == 1
            && groupMember.type != GroupMember.GroupMemberType.Owner
            && groupMember.type != GroupMember.GroupMemberType.Manager
            && groupMember.type != GroupMember.GroupMemberType.Allowed) {
            inputPanel.disableInput("全员禁言中");
        } else {
            inputPanel.enableInput();
        }
    }

    @OnClick(R2.id.unreadCountTextView)
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
                            content.tip = String.format(welcome, userViewModel.getUserDisplayName(userInfo));
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
            content.tip = String.format(welcome, userViewModel.getUserDisplayName(userInfo));
        } else {
            content.tip = String.format(welcome, "<" + userId + ">");
        }
        messageViewModel.sendMessage(conversation, content);
        chatRoomViewModel.quitChatRoom(conversation.target);
    }

    private void setChatRoomConversationTitle() {
        chatRoomViewModel.getChatRoomInfo(conversation.target, System.currentTimeMillis())
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
            conversationTitle = userViewModel.getUserDisplayName(userInfo);
        } else if (conversation.type == Conversation.ConversationType.Group) {
            if (groupInfo != null) {
                conversationTitle = groupInfo.name + "(" + groupInfo.memberCount + "人)";
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
        setActivityTitle(conversationTitle);
    }

    private void setActivityTitle(String title) {
        Activity activity = getActivity();
        if (activity != null) {
            activity.setTitle(title);
        }
    }

    @OnTouch({R2.id.contentLayout, R2.id.msgRecyclerView})
    boolean onTouch(View view, MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN && inputPanel.extension.canHideOnScroll()) {
//            inputPanel.collapse();
//        }
        inputPanel.closeConversationInputPanel();
        return false;
    }

    @Override
    public void onPortraitClick(UserInfo userInfo) {
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
            inputPanel.editText.getEditableText().append(userViewModel.getUserDisplayName(userInfo));
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
                    UserInfo userInfo = userViewModel.getUserInfo(userId, false);
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
        String text = "@" + userInfo.displayName + " ";
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

        if (conversation.type == Conversation.ConversationType.ChatRoom) {
            quitChatRoom();
        }

        messageViewModel.messageLiveData().removeObserver(messageLiveDataObserver);
        messageViewModel.messageUpdateLiveData().removeObserver(messageUpdateLiveDatObserver);
        messageViewModel.messageRemovedLiveData().removeObserver(messageRemovedLiveDataObserver);
        messageViewModel.mediaUpdateLiveData().removeObserver(mediaUploadedLiveDataObserver);
        userViewModel.userInfoLiveData().removeObserver(userInfoUpdateLiveDataObserver);
        conversationViewModel.clearConversationMessageLiveData().removeObserver(clearConversationMessageObserver);
        settingViewModel.settingUpdatedLiveData().removeObserver(settingUpdateLiveDataObserver);

        unInitGroupObservers();
        inputPanel.onDestroy();
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
        conversationViewModel.getMessages(conversation, channelPrivateChatUser).observe(this, uiMessages -> {
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

        conversationViewModel.loadOldMessages(conversation, channelPrivateChatUser, fromMessageId, fromMessageUid, MESSAGE_LOAD_COUNT_PER_TIME)
            .observe(this, uiMessages -> {
                adapter.addMessagesAtHead(uiMessages);

                swipeRefreshLayout.setRefreshing(false);
            });
    }

    private void loadMoreNewMessages() {
        loadingNewMessage = true;
        adapter.showLoadingNewMessageProgressBar();
        conversationViewModel.loadNewMessages(conversation, channelPrivateChatUser, adapter.getItem(adapter.getItemCount() - 2).message.messageId, MESSAGE_LOAD_COUNT_PER_TIME)
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
        setActivityTitle(typingDesc);
        handler.postDelayed(resetConversationTitleRunnable, 5000);
    }

    private Runnable resetConversationTitleRunnable = this::resetConversationTitle;

    private void resetConversationTitle() {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        if (!TextUtils.equals(conversationTitle, getActivity().getTitle())) {
            setActivityTitle(conversationTitle);
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
        Map<String, Long> deliveries = adapter.getDeliveries();
        Map<String, Long> readEntries = adapter.getReadEntries();
        int deliveryCount = 0;
        if (deliveries != null) {
            for (Map.Entry<String, Long> delivery : deliveries.entrySet()) {
                if (delivery.getValue() >= message.serverTime) {
                    deliveryCount++;
                }
            }
        }
        int readCount = 0;
        if (readEntries != null) {
            for (Map.Entry<String, Long> readEntry : readEntries.entrySet()) {
                if (readEntry.getValue() >= message.serverTime) {
                    readCount++;
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("已送达人数：")
            .append(deliveryCount)
            .append("\n")
            .append("未送达人数：")
            .append(groupInfo.memberCount - 1 - deliveryCount)
            .append("\n")
            .append("已读人数：")
            .append(readCount)
            .append("\n")
            .append("未读人数：")
            .append(groupInfo.memberCount - 1 - readCount)
        ;
        new MaterialDialog.Builder(getActivity())
            .title("消息回执")
            .content(builder.toString())
            .build()
            .show();
    }
}
