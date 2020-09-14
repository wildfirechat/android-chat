/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.search.OnResultItemClickListener;
import cn.wildfire.chat.kit.search.SearchFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.UserInfo;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

abstract public class PickOrCreateConversationActivity extends WfcBaseActivity {
    private SearchFragment searchFragment;
    private List<SearchableModule> searchableModules;

    @BindView(R2.id.searchEditText)
    EditText editText;


    @Override
    protected void afterViews() {
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
                onPickOrCreateConversation(conversation);
            }
        });
        searchableModules.add(module);

        module = new GroupSearchViewModule();
        module.setOnResultItemListener(new OnResultItemClickListener<GroupSearchResult>() {
            @Override
            public void onResultItemClick(Fragment fragment, View itemView, View view, GroupSearchResult gr) {
                Conversation conversation = new Conversation(Conversation.ConversationType.Group, gr.groupInfo.target, 0);
                onPickOrCreateConversation(conversation);
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

    @OnTextChanged(value = R2.id.searchEditText, callback = AFTER_TEXT_CHANGED)
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

    @Override
    protected int contentLayout() {
        return R.layout.pick_or_create_conversation_activity;
    }
}
