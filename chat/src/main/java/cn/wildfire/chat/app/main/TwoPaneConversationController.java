/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.os.Bundle;
import android.view.View;

import java.util.List;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.ConversationHost;
import cn.wildfire.chat.kit.conversation.ConversationTitleHelper;
import cn.wildfire.chat.kit.conversationlist.ConversationListFragment;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 平板双栏主界面「右栏」的全部逻辑：承载 {@link ConversationFragment}、驱动右栏自己的 toolbar
 * 与菜单、维护空状态占位，并把左栏列表的选中态同步过去。
 * <p>
 * 只有 {@code WfcDeviceUtils.isTwoPaneLayout()} 为 true 时 {@link MainActivity} 才会创建它，
 * 手机上这个类根本不会被实例化，因此这里的任何逻辑都影响不到手机端。
 * <p>
 * 它实现 {@link ConversationHost}：会话页原先假设自己独占一个 Activity，会把标题写到全局标题栏、
 * 用 {@code finish()} 关闭自己；在双栏下这些动作都必须落到右栏上，参见阶段 3 的宿主解耦。
 */
public class TwoPaneConversationController implements ConversationHost {

    private static final String STATE_CONVERSATION = "twoPaneConversation";
    private static final String STATE_CONVERSATION_TITLE = "twoPaneConversationTitle";
    private static final String FRAGMENT_TAG = "twoPaneConversation";

    private final FragmentActivity activity;
    private final View appBarLayout;
    private final Toolbar toolbar;
    private final View container;
    private final View emptyView;
    private final ConversationTitleHelper titleHelper;

    private ConversationFragment conversationFragment;
    private Conversation conversation;
    private String conversationTitle;
    /**
     * 当前会话是否在左栏列表中出现过，见 {@link #onConversationListChanged(List)}。
     */
    private boolean conversationSeenInList;

    /**
     * 左栏的会话列表，用于同步选中态高亮。可能晚于本控制器创建，故允许后置注入。
     */
    private ConversationListFragment conversationListFragment;

    public TwoPaneConversationController(FragmentActivity activity) {
        this.activity = activity;
        this.appBarLayout = activity.findViewById(R.id.conversationAppBarLayout);
        this.toolbar = activity.findViewById(R.id.conversationToolbar);
        this.container = activity.findViewById(R.id.conversationContainerFrameLayout);
        this.emptyView = activity.findViewById(R.id.conversationEmptyLinearLayout);
        this.titleHelper = new ConversationTitleHelper(activity, toolbar, toolbar::setTitle);
        this.toolbar.setOnMenuItemClickListener(item ->
            conversationFragment != null && conversationFragment.onConversationMenuItemSelected(item));
    }

    public void setConversationListFragment(ConversationListFragment fragment) {
        this.conversationListFragment = fragment;
        syncListSelection();
    }

    /**
     * 在右栏打开一个会话。已经打开同一个会话时直接返回，避免重复重建导致草稿、滚动位置丢失。
     */
    public void showConversation(Conversation conversation) {
        showConversation(conversation, null, -1, null, false);
    }

    public void showConversation(Conversation conversation, String title, long focusMessageId,
                                 String channelPrivateChatUser, boolean isPreJoinedChatRoom) {
        if (conversation == null) {
            return;
        }
        if (Conversation.equals(this.conversation, conversation) && conversationFragment != null) {
            return;
        }
        this.conversation = conversation;
        this.conversationTitle = title;
        this.conversationSeenInList = false;

        // 每次换会话都换一个全新的 Fragment，而不是复用同一个再 setupConversation：
        // 多选模式、输入框草稿、展开中的表情/扩展面板、密聊退出时的临时文件清理等状态
        // 都挂在 Fragment 上，复用极易串会话。
        ConversationFragment fragment = new ConversationFragment();
        conversationFragment = fragment;
        activity.getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.conversationContainerFrameLayout, fragment, FRAGMENT_TAG)
            .commitNowAllowingStateLoss();

        // commitNow 只把 Fragment 推进到「宿主当前的」生命周期状态。分屏/旋转重建后本方法是在
        // Activity.onCreate 里调用的，此时 onCreateView 还没跑，直接 setupConversation 会因为
        // adapter / inputPanel 尚未创建而 NPE。post 到下一帧，视图必定已就绪。
        // 用局部变量而不是字段，避免快速连点两个会话时把参数应用到后一个 Fragment 上。
        container.post(() -> {
            if (fragment.isAdded()) {
                fragment.setupConversation(conversation, title, focusMessageId, channelPrivateChatUser, isPreJoinedChatRoom);
            }
        });

        emptyView.setVisibility(View.GONE);
        appBarLayout.setVisibility(View.VISIBLE);
        if (toolbar.getMenu().size() == 0) {
            toolbar.inflateMenu(R.menu.conversation);
        }
        syncListSelection();
    }

    public Conversation getCurrentConversation() {
        return conversation;
    }

    /**
     * 返回键：先交给会话页消费（收起表情/扩展面板、退出多选），未消费则返回 false，
     * 由宿主按手机端的语义处理（{@code moveTaskToBack}）。与微信 Pad 一致，返回键不清空右栏。
     */
    public boolean onBackPressed() {
        // getView() 非空才说明会话页的视图已创建，onBackPressed 里访问的输入面板等控件才存在
        return conversationFragment != null && conversationFragment.getView() != null
            && conversationFragment.onBackPressed();
    }

    /**
     * 左栏会话列表更新时调用：右栏打开的会话若已从列表中消失（删除会话、退群等），把右栏清空。
     * <p>
     * 只有「曾经出现在列表里、之后又消失」才算删除。新建的空会话在发出第一条消息前本来就不在
     * 列表中，若不作区分会导致刚点开的新会话立刻被关掉。
     */
    public void onConversationListChanged(List<ConversationInfo> conversationInfos) {
        if (conversation == null || conversationInfos == null) {
            return;
        }
        for (ConversationInfo info : conversationInfos) {
            if (Conversation.equals(info.conversation, conversation)) {
                conversationSeenInList = true;
                return;
            }
        }
        if (conversationSeenInList) {
            closeConversation();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (conversation != null) {
            outState.putParcelable(STATE_CONVERSATION, conversation);
            outState.putString(STATE_CONVERSATION_TITLE, conversationTitle);
        }
    }

    /**
     * 配置变化（旋转、分屏）重建后，恢复右栏原来打开的会话。
     */
    public void restoreInstanceState(Bundle savedInstanceState) {
        Conversation saved = getSavedConversation(savedInstanceState);
        if (saved != null) {
            showConversation(saved, savedInstanceState.getString(STATE_CONVERSATION_TITLE), -1, null, false);
            return;
        }
        // 没有会话要恢复，但 FragmentManager 可能已自动重建了上一次的会话页，清掉它，
        // 否则会盖在空状态占位上显示一个没有会话的空聊天页。
        Fragment stale = activity.getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (stale != null) {
            activity.getSupportFragmentManager()
                .beginTransaction()
                .remove(stale)
                .commitNowAllowingStateLoss();
        }
    }

    /**
     * 窗口从双栏变窄到单栏时，{@link MainActivity} 用它把右栏会话交还给独立会话页。
     */
    public static Conversation getSavedConversation(Bundle savedInstanceState) {
        return savedInstanceState == null ? null : savedInstanceState.getParcelable(STATE_CONVERSATION);
    }

    private void syncListSelection() {
        if (conversationListFragment != null) {
            conversationListFragment.setSelectedConversation(conversation);
        }
    }

    // ==================== ConversationHost ====================

    @Override
    public void setConversationTitle(CharSequence title, CharSequence subTitle, boolean silent, boolean earpiece) {
        titleHelper.setTitle(title, subTitle, silent, earpiece);
    }

    @Override
    public CharSequence getConversationTitle() {
        return toolbar.getTitle();
    }

    /**
     * 双栏下「关闭会话」不是结束 Activity，而是清空右栏回到空状态。
     * 触发场景与手机端一致：聊天室加入失败、密聊信息缺失。
     */
    @Override
    public void closeConversation() {
        if (conversationFragment != null) {
            activity.getSupportFragmentManager()
                .beginTransaction()
                .remove(conversationFragment)
                .commitAllowingStateLoss();
            conversationFragment = null;
        }
        conversation = null;
        conversationTitle = null;
        conversationSeenInList = false;
        titleHelper.reset();
        toolbar.setTitle(null);
        toolbar.setSubtitle(null);
        toolbar.getMenu().clear();
        appBarLayout.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        syncListSelection();
    }

    /**
     * 双栏宿主的 Intent 里没有 highlightMessageId —— 需要高亮定位的入口（通知点击、搜索结果）
     * 在阶段 5 统一收口到 ConversationRouter 时再通过参数传入。
     */
    @Override
    public long getHighlightMessageId() {
        return 0;
    }
}
