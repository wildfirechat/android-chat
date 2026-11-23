/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.PCOnlineStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationViewModel;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfire.chat.kit.viewmodel.UserOnlineStateViewModel;
import cn.wildfire.chat.kit.utils.AgentRobotService;
import cn.wildfire.chat.kit.utils.AgentState;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.model.UserOnlineState;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnUserOnlineEventListener;
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper;

public class ConversationListFragment extends ProgressFragment {

    /**
     * 不过滤：普通会话 + AI 会话都展示（选择会话、备份等场景沿用这个模式）。
     */
    public static final int MODE_ALL = 0;
    /**
     * 主界面「消息」tab：只展示普通会话（排除与本人 DSH 机器人的单聊）。
     */
    public static final int MODE_NORMAL = 1;
    /**
     * 主界面「办公」tab：只展示 AI 会话（line=2 的会话 + 与本人 DSH 机器人的单聊）。
     */
    public static final int MODE_AI = 2;

    private static final String ARG_MODE = "conversationListMode";

    private static final List<Conversation.ConversationType> TYPES = Arrays.asList(Conversation.ConversationType.Single,
        Conversation.ConversationType.Group,
        Conversation.ConversationType.Channel,
        Conversation.ConversationType.SecretChat);
    /** 普通消息 line 0 + AI 消息 line 2。 */
    private static final List<Integer> LINES_ALL = Arrays.asList(0, 2);
    /** 普通消息只有 line 0。 */
    private static final List<Integer> LINES_NORMAL = Collections.singletonList(0);

    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    private OnUserOnlineEventListener userOnlineEventListener;
    private UserOnlineStateViewModel userOnlineStateViewModel;
    private java.util.List<String> watchedAiOwnerIds = new java.util.ArrayList<>();

    private ConversationListViewModel conversationListViewModel;
    private SettingViewModel settingViewModel;
    private StatusNotificationViewModel statusNotificationViewModel;
    private LinearLayoutManager layoutManager;

    private int mode = MODE_ALL;
    /** 最近一次从 IM 拉到的原始会话列表（未过滤），机器人 id 异步到达后用它重新过滤。 */
    private List<ConversationInfo> rawConversationInfos;

    /** 单聊 DSH 机器人 id 异步到达：刷新列表（机器人单聊在「消息」/「办公」之间换 tab）与 AI 徽标/状态圆点 */
    private final AgentRobotService.Callback agentRobotCallback = robotId -> {
        if (!isAdded() || adapter == null || layoutManager == null) {
            return;
        }
        // 机器人 id 到达后 AI 判定可能翻转（单聊在消息/办公之间换 tab）：
        // 重新过滤后还要按最新列表重订 AI 机器人在线状态（单聊也订阅）
        watchAiOwnersOnlineState(applyConversationInfos(rawConversationInfos));
    };
    private OnClickConversationItemListener onClickConversationItemListener;
    // 平板双栏的选中态，手机端不使用
    private Conversation selectedConversation;
    private boolean hasSelectedConversation;

    /**
     * 创建带过滤模式实例。「消息」传 {@link #MODE_NORMAL}，「办公」传 {@link #MODE_AI}；
     * 选择会话等复用场景直接用默认构造（{@link #MODE_ALL}）。
     * <p>
     * 模式放在 arguments 里，配置变化（旋转/分屏/切 tab 重建）后能从 FragmentManager
     * 恢复的实例上读回来——主界面同时存在两个 {@code ConversationListFragment} 实例，
     * 不靠 arguments 无法区分哪个是「办公」。
     */
    public static ConversationListFragment newInstance(int mode) {
        ConversationListFragment fragment = new ConversationListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    public int getMode() {
        Bundle args = getArguments();
        return args == null ? MODE_ALL : args.getInt(ARG_MODE, MODE_ALL);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getMode();
    }

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

    /**
     * 平板双栏专用：高亮右栏当前打开的会话。传 null 表示右栏为空。
     * <p>
     * 可能在 {@link #init()} 之前被调用（宿主先于列表视图就绪），故先暂存，视图就绪后再应用。
     */
    public void setSelectedConversation(Conversation conversation) {
        this.selectedConversation = conversation;
        this.hasSelectedConversation = true;
        if (adapter != null) {
            adapter.setSelectedConversation(conversation);
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
        if (hasSelectedConversation) {
            adapter.setSelectedConversation(selectedConversation);
        }
        // 「办公」需要 line 0（与 DSH 机器人的单聊）+ line 2（AI 群），拉全量后按 Agent 判定过滤；
        // 「消息」只需要 line 0，拉回来再排除 DSH 机器人单聊。
        List<Integer> lines = mode == MODE_NORMAL ? LINES_NORMAL : LINES_ALL;
        conversationListViewModel = new ViewModelProvider(this, new ConversationListViewModelFactory(TYPES, lines))
            .get(ConversationListViewModel.class);
        conversationListViewModel.conversationListLiveData().observe(this, conversationInfos -> {
            rawConversationInfos = conversationInfos;
            showContent();
            watchAiOwnersOnlineState(applyConversationInfos(conversationInfos));
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
                adapter.notifyUserInfosUpdated(userInfos, start, end);
            }
        });
        userViewModel.domainInfoLiveData().observe(this, new Observer<DomainInfo>() {
            @Override
            public void onChanged(DomainInfo domainInfo) {
                // 域信息影响的是全局展示（外部用户昵称等），出现的频率很低，
                // 这里仍然刷新整个可见区域即可；用户/群信息已改为按行精准刷新。
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
                adapter.notifyGroupInfosUpdated(groupInfos, start, end);
            }
        });

        // AI 在线状态变化（含 AI 群群主上线/下线）：刷新可见行——Agent 状态圆点/徽标按在线状态置灰
        userOnlineEventListener = userOnlineStateMap -> {
            if (adapter == null || layoutManager == null) {
                return;
            }
            int start = layoutManager.findFirstVisibleItemPosition();
            int end = layoutManager.findLastVisibleItemPosition();
            if (start >= 0 && end >= start) {
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        };
        ChatManager.Instance().addUserOnlineEventListener(userOnlineEventListener);

        // AI 群（line 2）：列表级订阅群主（AI 机器人）在线状态，AI 不在线时状态圆点/徽标置灰；
        // 回调 LiveData 刷新可见行（初始无缓存时也能拿到状态）
        userOnlineStateViewModel = new ViewModelProvider(this).get(UserOnlineStateViewModel.class);
        userOnlineStateViewModel.getUserOnlineStateLiveData().observe(this, userOnlineStateMap -> {
            if (adapter == null || layoutManager == null) {
                return;
            }
            int start = layoutManager.findFirstVisibleItemPosition();
            int end = layoutManager.findLastVisibleItemPosition();
            if (start >= 0 && end >= start) {
                adapter.notifyItemRangeChanged(start, end - start + 1);
            }
        });

        // 连接状态 / PC 在线提醒这类「系统通知条」只挂在消息 tab 上；
        // 办公 tab 是 AI 会话专用列表，重复展示同一份通知没有意义。
        if (mode != MODE_AI) {
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
        }
        AgentRobotService.Instance().addListener(agentRobotCallback);
        AgentRobotService.Instance().ensureLoaded(null);
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);
        settingViewModel.settingUpdatedLiveData().observe(this, o -> {
            if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
                return;
            }
            conversationListViewModel.reloadConversationList(true);
            conversationListViewModel.reloadConversationUnreadStatus();
            if (mode != MODE_AI) {
                loadAndShowPCOnlineNotification();
            }
        });

        if (mode != MODE_AI && ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusConnected) {
            loadAndShowPCOnlineNotification();
        }
        reloadConversations();
    }

    /**
     * 应用当前模式对应的会话过滤：
     * <ul>
     *   <li>{@link #MODE_ALL}：原样展示；</li>
     *   <li>{@link #MODE_AI}：只留下 {@link AgentState#isAgentConversation} 为真的会话
     *       （line=2 的 AI 群 + 与本人 DSH 机器人的单聊）；</li>
     *   <li>{@link #MODE_NORMAL}：反过来，排除 AI 会话。</li>
     * </ul>
     * 过滤放在客户端做，是因为「与本人 DSH 机器人的单聊」在 SDK 层和普通单聊同一条 line，
     * IM 的 line 过滤参数表达不了这个例外。
     */
    private List<ConversationInfo> applyConversationInfos(List<ConversationInfo> conversationInfos) {
        if (mode == MODE_ALL || conversationInfos == null) {
            if (adapter != null) {
                adapter.setConversationInfos(conversationInfos);
            }
            return conversationInfos;
        }
        List<ConversationInfo> filtered = new ArrayList<>(conversationInfos.size());
        for (ConversationInfo info : conversationInfos) {
            Conversation conversation = info == null ? null : info.conversation;
            if (conversation == null) {
                continue;
            }
            boolean agent = AgentState.isAgentConversation(conversation);
            if (mode == MODE_AI ? agent : !agent) {
                filtered.add(info);
            }
        }
        if (adapter != null) {
            adapter.setConversationInfos(filtered);
        }
        return filtered;
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
        if (statusNotificationViewModel == null) {
            return;
        }
        List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
        statusNotificationViewModel.clearStatusNotificationByType(PCOnlineStatusNotification.class);
        if (pcOnlineInfos != null && !pcOnlineInfos.isEmpty()) {
            // 与 HarmonyOS 版一致：全部在线设备合并成一条提醒，点击后传入完整列表
            PCOnlineStatusNotification notification = new PCOnlineStatusNotification(pcOnlineInfos);
            statusNotificationViewModel.showStatusNotification(notification);

            SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
            sp.edit().putBoolean("wfc_uikit_had_pc_session", true).commit();
        }
    }

    private void reloadConversations() {
        if (ChatManager.Instance().getConnectionStatus() == ConnectionStatus.ConnectionStatusReceiveing) {
            return;
        }
        conversationListViewModel.reloadConversationList();
        conversationListViewModel.reloadConversationUnreadStatus();
    }

    /**
     * AI 会话：收集当前列表里 AI 机器人的 uid 批量订阅在线状态（先退订旧的）。
     * AI 群取群主（AI 机器人），单聊 DSH 会话取对端机器人，统一用
     * {@link AgentState#agentOwnerId(Conversation)} 取；离线时列表状态圆点/徽标置灰。
     */
    private void watchAiOwnersOnlineState(List<ConversationInfo> conversationInfos) {
        if (userOnlineStateViewModel == null || conversationInfos == null) {
            return;
        }
        List<String> owners = new java.util.ArrayList<>();
        for (ConversationInfo info : conversationInfos) {
            Conversation conv = info.conversation;
            if (conv == null || !AgentState.isAgentConversation(conv)) {
                continue;
            }
            String ownerId = AgentState.agentOwnerId(conv);
            if (!TextUtils.isEmpty(ownerId) && !owners.contains(ownerId)) {
                owners.add(ownerId);
            }
        }
        if (!watchedAiOwnerIds.isEmpty()) {
            userOnlineStateViewModel.unwatchOnlineState(Conversation.ConversationType.Single.getValue(),
                watchedAiOwnerIds.toArray(new String[0]));
            watchedAiOwnerIds.clear();
        }
        if (!owners.isEmpty()) {
            userOnlineStateViewModel.watchUserOnlineState(Conversation.ConversationType.Single.getValue(),
                owners.toArray(new String[0]));
            watchedAiOwnerIds.addAll(owners);
        }
    }

    public void onDestroyView() {
        AgentRobotService.Instance().removeListener(agentRobotCallback);
        if (userOnlineEventListener != null) {
            ChatManager.Instance().removeUserOnlineEventListener(userOnlineEventListener);
            userOnlineEventListener = null;
        }
        if (userOnlineStateViewModel != null && !watchedAiOwnerIds.isEmpty()) {
            userOnlineStateViewModel.unwatchOnlineState(Conversation.ConversationType.Single.getValue(),
                watchedAiOwnerIds.toArray(new String[0]));
            watchedAiOwnerIds.clear();
        }
        super.onDestroyView();
    }

}
