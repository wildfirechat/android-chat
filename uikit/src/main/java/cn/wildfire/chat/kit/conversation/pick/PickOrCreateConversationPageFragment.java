/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.search.OnResultItemClickListener;
import cn.wildfire.chat.kit.search.SearchFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.MaxSizeRecyclerView;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「挑一个会话，或者现建一个」整页：顶部搜索框 + 已选头像栏 + 会话列表，
 * 转发（{@link cn.wildfire.chat.kit.conversation.forward.ForwardPageFragment}）与会议邀请
 * （{@code ConferenceInvitePageFragment}）共用。
 * <p>
 * 逐行搬自 {@link PickOrCreateConversationActivity}。那个类是抽象 Activity，页面无法脱离
 * Activity 存在，因此永远进不了平板右栏；现在整页收敛到本 Fragment，手机端（薄壳 Activity）
 * 与右栏共用同一份实现。
 * <p>
 * 三处从 Activity 迁到 Fragment 时必须改写的地方，逐一记在对应方法上：
 * 子 Fragment 装在 {@code childFragmentManager}、搜索浮层的返回改由
 * {@link #onPageBackPressed()} 处理、关闭页面走 {@code WfcPageCompat}。
 */
public abstract class PickOrCreateConversationPageFragment extends Fragment implements WfcPage {

    private static final String TAG_SEARCH = "search";
    private static final String BACK_STACK_SEARCH = "search-back";

    private EditText editText;
    private MaxSizeRecyclerView selectedAvatarsRecyclerView;
    private SelectedConversationAdapter selectedConversationAdapter;
    private SearchFragment searchFragment;
    private List<SearchableModule> searchableModules;

    protected boolean isMultiSelectMode = false;
    protected UserViewModel userViewModel;
    protected GroupViewModel groupViewModel;
    protected final Map<String, String> tempPortraitMap = new HashMap<>();

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pick_or_create_conversation_page_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        bindViews(view);
        setupPickFragment();
        initSearch();
    }

    private void bindViews(View view) {
        editText = view.findViewById(R.id.searchEditText);
        selectedAvatarsRecyclerView = view.findViewById(R.id.selectedAvatarsRecyclerView);

        selectedConversationAdapter = new SelectedConversationAdapter();
        selectedConversationAdapter.setTempPortraitMap(tempPortraitMap);
        selectedConversationAdapter.setOnItemClickListener(info -> {
            String key = info.conversation.type + "_" + info.conversation.target;
            tempPortraitMap.remove(key);
            onSelectedConversationRemoved(info);
        });

        selectedAvatarsRecyclerView.setLayoutManager(
            new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
        selectedAvatarsRecyclerView.setAdapter(selectedConversationAdapter);

        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                search(s);
            }
        });
    }

    /**
     * 会话列表子页。装在 {@code childFragmentManager} 而不是 Activity 的 FragmentManager：
     * 右栏里一个 Activity 同时挂着五条导航栈，用 Activity 那一层的 FragmentManager 会让
     * 各页的子 Fragment 混在一起。
     * <p>
     * 与改造前的另一点差别：先查有没有，没有才 add。原来在 {@code afterViews} 里无条件 add，
     * 配置变化后会在恢复出来的那个之上再叠一个（手机端锁竖屏所以没暴露）。
     */
    private void setupPickFragment() {
        PickOrCreateConversationFragment fragment = pickFragment();
        if (fragment == null) {
            fragment = new PickOrCreateConversationFragment();
            getChildFragmentManager()
                .beginTransaction()
                .add(R.id.containerFrameLayout, fragment)
                .commit();
        }
        // 监听器不是可保存的状态，重建后必须重新挂上
        fragment.setListener(this::onPickOrCreateConversation);
        onPickFragmentReady(fragment);
    }

    /**
     * 会话列表子页已就绪（新建或重建后）。子类在这里装配额外回调 —— 转发页的多选计数就挂在这。
     * <p>
     * 改造前 {@code ForwardActivity} 用 {@code Handler.post} 等这个子 Fragment 出现，
     * 现在由本回调精确通知，不用再赌一帧。
     */
    protected void onPickFragmentReady(PickOrCreateConversationFragment fragment) {
    }

    /**
     * 会话列表子页。<strong>不能用 {@code findFragmentById}</strong>：搜索浮层加在同一个容器里，
     * 那个方法返回的是最后加进去的那个（搜索中就是 SearchFragment）。
     */
    @Nullable
    protected PickOrCreateConversationFragment pickFragment() {
        for (Fragment fragment : getChildFragmentManager().getFragments()) {
            if (fragment instanceof PickOrCreateConversationFragment) {
                return (PickOrCreateConversationFragment) fragment;
            }
        }
        return null;
    }

    /**
     * 已选头像栏里点掉了一个。默认取消它的勾选，与改造前
     * {@code ForwardActivity} 装的那个 {@code OnConversationRemovedListener} 等价。
     */
    protected void onSelectedConversationRemoved(ConversationInfo conversationInfo) {
        PickOrCreateConversationFragment fragment = pickFragment();
        if (fragment != null) {
            fragment.toggleConversationSelection(conversationInfo);
        }
    }

    protected void clearSearch() {
        editText.setText("");
        editText.clearFocus();
    }

    protected void updateSelectedAvatars(@Nullable List<ConversationInfo> conversations) {
        if (selectedAvatarsRecyclerView == null) {
            return;
        }
        if (conversations == null || conversations.isEmpty()) {
            selectedAvatarsRecyclerView.setVisibility(View.GONE);
            return;
        }
        selectedAvatarsRecyclerView.setVisibility(View.VISIBLE);
        selectedConversationAdapter.setConversations(conversations);
        selectedAvatarsRecyclerView.scrollToPosition(conversations.size() - 1);
    }

    // ==================== 搜索浮层 ====================

    private void initSearch() {
        searchableModules = new ArrayList<>();
        SearchableModule module = new ContactSearchModule();
        module.setOnResultItemListener(new OnResultItemClickListener<UserInfo>() {
            @Override
            public void onResultItemClick(Fragment fragment, View itemView, View view, UserInfo userInfo) {
                Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
                if (isMultiSelectMode) {
                    onSearchResultClicked(conversation, userInfo.displayName, userInfo.portrait);
                } else {
                    onPickOrCreateConversation(conversation);
                }
            }
        });
        searchableModules.add(module);

        module = new GroupSearchViewModule();
        module.setOnResultItemListener(new OnResultItemClickListener<GroupSearchResult>() {
            @Override
            public void onResultItemClick(Fragment fragment, View itemView, View view, GroupSearchResult gr) {
                Conversation conversation = new Conversation(Conversation.ConversationType.Group, gr.groupInfo.target, 0);
                if (isMultiSelectMode) {
                    String name = !TextUtils.isEmpty(gr.groupInfo.remark) ? gr.groupInfo.remark : gr.groupInfo.name;
                    onSearchResultClicked(conversation, name, gr.groupInfo.portrait);
                } else {
                    onPickOrCreateConversation(conversation);
                }
            }
        });
        searchableModules.add(module);
    }

    void search(Editable editable) {
        String keyword = editable.toString().trim();
        FragmentManager fm = getChildFragmentManager();
        if (!TextUtils.isEmpty(keyword)) {
            if (fm.findFragmentByTag(TAG_SEARCH) == null) {
                searchFragment = new SearchFragment();
                fm.beginTransaction()
                    .add(R.id.containerFrameLayout, searchFragment, TAG_SEARCH)
                    .addToBackStack(BACK_STACK_SEARCH)
                    .commit();
            }
            // 事务还没执行，SearchFragment 的视图尚不存在，post 到下一帧再喂关键字
            ChatManager.Instance().getMainHandler().post(() -> {
                if (isAdded() && searchFragment != null) {
                    searchFragment.search(keyword, searchableModules);
                }
            });
        } else {
            fm.popBackStackImmediate();
        }
    }

    /**
     * 搜索结果被点中（仅多选模式下）。单选模式直接走 {@link #onPickOrCreateConversation}。
     */
    protected void onSearchResultClicked(Conversation conversation, String name, String portrait) {
    }

    protected abstract void onPickOrCreateConversation(Conversation conversation);

    // ==================== WfcPage ====================

    /**
     * 返回键。搜索浮层开着时先收起它 —— 等价于改造前
     * {@code PickOrCreateConversationActivity.onBackPressed()} 里 {@code super} 弹返回栈那一步；
     * 没有浮层则返回 false，由宿主关闭整页（手机端 finish，右栏出栈）。
     */
    @Override
    public boolean onPageBackPressed() {
        FragmentManager fm = getChildFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            clearSearch();
            return true;
        }
        return false;
    }
}
