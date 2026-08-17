/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
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

/**
 * 「挑一个会话，或者现建一个」页的旧基类。
 * <p>
 * <strong>本仓库内已无子类</strong>：整页下沉到了 {@link PickOrCreateConversationPageFragment}，
 * 手机端由薄壳 Activity（{@code ForwardActivity}、{@code ConferenceInviteActivity}）装着，
 * 平板上同一份实现直接进右栏。
 * <p>
 * <strong>新增这类页面请继承 {@link PickOrCreateConversationPageFragment}</strong>，别再从这里派生
 * —— 从 Activity 派生的页面无法进入右栏，且会与 Fragment 那一份各自漂移。
 * 本类仅为已经继承它的外部集成方保留。
 *
 * @deprecated 用 {@link PickOrCreateConversationPageFragment}
 */
@Deprecated
abstract public class PickOrCreateConversationActivity extends WfcBaseActivity {
    private SearchFragment searchFragment;
    private List<SearchableModule> searchableModules;

    EditText editText;
    MaxSizeRecyclerView selectedAvatarsRecyclerView;
    private SelectedConversationAdapter selectedConversationAdapter;
    private List<ConversationInfo> selectedConversations = new ArrayList<>();

    protected boolean isMultiSelectMode = false;
    protected UserViewModel userViewModel;
    protected GroupViewModel groupViewModel;
    protected Map<String, String> tempPortraitMap = new HashMap<>();

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
    }

    public interface OnConversationRemovedListener {
        void onConversationRemoved(ConversationInfo conversation);
    }

    private OnConversationRemovedListener conversationRemovedListener;

    public void setOnConversationRemovedListener(OnConversationRemovedListener listener) {
        this.conversationRemovedListener = listener;
    }

    public OnConversationRemovedListener getOnConversationRemovedListener() {
        return conversationRemovedListener;
    }

    protected void bindViews() {
        super.bindViews();
        editText = findViewById(R.id.searchEditText);
        selectedAvatarsRecyclerView = findViewById(R.id.selectedAvatarsRecyclerView);

        selectedConversationAdapter = new SelectedConversationAdapter();
        selectedConversationAdapter.setTempPortraitMap(tempPortraitMap);
        selectedConversationAdapter.setOnItemClickListener(info -> {
            if (conversationRemovedListener != null) {
                String key = info.conversation.type + "_" + info.conversation.target;
                tempPortraitMap.remove(key);
                conversationRemovedListener.onConversationRemoved(info);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        selectedAvatarsRecyclerView.setLayoutManager(layoutManager);
        selectedAvatarsRecyclerView.setAdapter(selectedConversationAdapter);

        editText.addTextChangedListener(new SimpleTextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                search(s);
            }
        });
    }

    protected void clearSearch() {
        editText.setText("");
        editText.clearFocus();
    }

    protected void updateSelectedAvatars(List<ConversationInfo> conversations) {
        this.selectedConversations = conversations;
        if (conversations == null || conversations.isEmpty()) {
            selectedAvatarsRecyclerView.setVisibility(View.GONE);
            return;
        }

        selectedAvatarsRecyclerView.setVisibility(View.VISIBLE);
        selectedConversationAdapter.setConversations(conversations);
        selectedAvatarsRecyclerView.scrollToPosition(conversations.size() - 1);
    }

    // Deprecated methods and fields removed


    @Override
    protected void afterViews() {
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        PickOrCreateConversationFragment pickOrCreateConversationFragment = new PickOrCreateConversationFragment();
        pickOrCreateConversationFragment.setListener(this::onPickOrCreateConversation);
        getSupportFragmentManager()
            .beginTransaction()
            .add(R.id.containerFrameLayout, pickOrCreateConversationFragment)
            .commit();
        initSearch();
    }

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        editText.setText("");
        editText.clearFocus();
    }

    void search(Editable editable) {
        String keyword = editable.toString().trim();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!TextUtils.isEmpty(keyword)) {
            if (fragmentManager.findFragmentByTag("search") == null) {
                searchFragment = new SearchFragment();
                fragmentManager.beginTransaction()
                    .add(R.id.containerFrameLayout, searchFragment, "search")
                    .addToBackStack("search-back")
                    .commit();
            }
            new Handler().post(() -> {
                searchFragment.search(keyword, searchableModules);
            });
        } else {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    protected abstract void onPickOrCreateConversation(Conversation conversation);

    protected void onSearchResultClicked(Conversation conversation, String name, String portrait) {
    }

    @Override
    protected int contentLayout() {
        return R.layout.pick_or_create_conversation_activity;
    }
}
