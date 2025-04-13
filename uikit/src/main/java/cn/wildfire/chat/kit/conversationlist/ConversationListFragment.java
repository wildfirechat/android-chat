/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationViewModel;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class ConversationListFragment extends ProgressFragment {
    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    private static final List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
        Conversation.ConversationType.Group,
        Conversation.ConversationType.Channel,
        Conversation.ConversationType.SecretChat);
    private static final List<Integer> lines = Arrays.asList(0);

    private ConversationListViewModel conversationListViewModel;
    private SettingViewModel settingViewModel;
    private StatusNotificationViewModel statusNotificationViewModel;
    private LinearLayoutManager layoutManager;
    private OnClickConversationItemListener onClickConversationItemListener;

    @Override
    protected int contentLayout() {
        return R.layout.conversationlist_frament;
    }

    @Override
    protected void afterViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        init();
    }

    public void setOnClickConversationItemListener(OnClickConversationItemListener listener) {
        this.onClickConversationItemListener = listener;
        if (adapter != null) {
            adapter.setOnClickConversationItemListener(listener);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void init() {
        adapter = new ConversationListAdapter(this);
        if (onClickConversationItemListener != null) {
            adapter.setOnClickConversationItemListener(onClickConversationItemListener);
        }
        conversationListViewModel = new ViewModelProvider(this, new ConversationListViewModelFactory(types, lines))
            .get(ConversationListViewModel.class);
        conversationListViewModel.conversationListLiveData().observe(this, conversationInfos -> {
            showContent();
            adapter.setConversationInfos(conversationInfos);
        });
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        OverScrollDecoratorHelper.setUpOverScroll(recyclerView, OverScrollDecoratorHelper.ORIENTATION_VERTICAL);

        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(this, new Observer<List<UserInfo>>() {
            @Override
            public void onChanged(List<UserInfo> userInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });
        userViewModel.domainInfoLiveData().observe(this, new Observer<DomainInfo>() {
            @Override
            public void onChanged(DomainInfo domainInfo) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });
        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(this, new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                int start = layoutManager.findFirstVisibleItemPosition();
                int end = layoutManager.findLastVisibleItemPosition();
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });

        statusNotificationViewModel = WfcUIKit.getAppScopeViewModel(StatusNotificationViewModel.class);
        statusNotificationViewModel.statusNotificationLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                adapter.updateStatusNotification(statusNotificationViewModel.getNotificationItems());
            }
        });
        conversationListViewModel.connectionStatusLiveData().observe(this, status -> {
            ConnectionStatusNotification connectionStatusNotification = new ConnectionStatusNotification();
            switch (status) {
                case ConnectionStatus.ConnectionStatusConnecting:
                    connectionStatusNotification.setValue(getString(R.string.connecting));
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusReceiveing:
                    connectionStatusNotification.setValue(getString(R.string.syncing));
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusConnected:
                    statusNotificationViewModel.hideStatusNotification(connectionStatusNotification);
                    loadAndShowPCOnlineNotification();
                    break;
                case ConnectionStatus.ConnectionStatusUnconnected:
                    connectionStatusNotification.setValue(getString(R.string.connection_failed));
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                default:
                    break;
            }
        });
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        settingViewModel.settingUpdatedLiveData().observe(this, o -> {
            if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
                return;
            }
            conversationListViewModel.reloadConversationList(true);
            conversationListViewModel.reloadConversationUnreadStatus();

            loadAndShowPCOnlineNotification();
        });

        if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusConnected) {
            loadAndShowPCOnlineNotification();
        }
        reloadConversations();
    }

    public void scrollToNextUnreadConversation() {
        if (layoutManager == null || adapter == null) {
            return;
        }
        int start = layoutManager.findFirstVisibleItemPosition();
        int nextUnreadConversationPosition = adapter.getNextUnreadConversationPosition(start);
        //  支持循环滚动，后面没有未读会话时，从头开始找
        if (nextUnreadConversationPosition == -1 && start > adapter.headerCount()) {
            nextUnreadConversationPosition = adapter.getNextUnreadConversationPosition(0);
        }

        if (nextUnreadConversationPosition != -1) {
            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                @Override
                protected int getVerticalSnapPreference() {
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };

            smoothScroller.setTargetPosition(nextUnreadConversationPosition);
            layoutManager.startSmoothScroll(smoothScroller);
        }
    }

    private void loadAndShowPCOnlineNotification() {
        List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
        statusNotificationViewModel.clearStatusNotificationByType(PCOnlineStatusNotification.class);
        if (pcOnlineInfos != null && !pcOnlineInfos.isEmpty()) {
            for (PCOnlineInfo info : pcOnlineInfos) {
                PCOnlineStatusNotification notification = new PCOnlineStatusNotification(info);
                statusNotificationViewModel.showStatusNotification(notification);

                SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
                sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
            }
        }
    }

    private void reloadConversations() {
        if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
            return;
        }
        conversationListViewModel.reloadConversationList();
        conversationListViewModel.reloadConversationUnreadStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
