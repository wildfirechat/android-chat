/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetConversationListCallback;

public class PickOrCreateConversationFragment extends Fragment implements PickOrCreateConversationAdapter.OnConversationItemClickListener, PickOrCreateConversationAdapter.OnNewConversationItemClickListener {
    private static final int REQUEST_CODE_PICK_CONVERSATION_TARGET = 100;
    RecyclerView recyclerView;
    private PickOrCreateConversationAdapter adapter;
    private OnPickOrCreateConversationListener listener;
    private OnSelectionChangedListener selectionChangedListener;
    private boolean multiSelectMode;

    public void setListener(OnPickOrCreateConversationListener listener) {
        this.listener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void setMultiSelectMode(boolean isMultiSelect) {
        this.multiSelectMode = isMultiSelect;
        applyMultiSelectMode();
    }

    /**
     * adapter 是在 {@code onCreateView} 里建的，而调用方（转发页）可能在那之前就把模式设过来了
     * （视图重建后恢复多选态）。把模式先记在字段上、建完 adapter 再应用一次，
     * 两种顺序都对。
     */
    private void applyMultiSelectMode() {
        if (adapter == null) {
            return;
        }
        adapter.setMode(multiSelectMode ?
            PickOrCreateConversationAdapter.MODE_MULTI :
            PickOrCreateConversationAdapter.MODE_SINGLE);
        if (!multiSelectMode) {
            adapter.clearSelections();
        }
    }

    public void toggleConversationSelection(ConversationInfo conversation) {
        if (adapter != null) {
            adapter.toggleSelection(conversation);
        }
    }

    public List<ConversationInfo> getSelectedConversations() {
        return adapter != null ? adapter.getSelectedConversations() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pick_or_create_conversation_fragmentn, container, false);
        bindViews(view);
        init();
        return view;
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
    }


    private void init() {
        adapter = new PickOrCreateConversationAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
            Conversation.ConversationType.Group);
        List<Integer> liens = Arrays.asList(0);
        ChatManager.Instance().getConversationListAsync(types, liens, new GetConversationListCallback() {
            @Override
            public void onSuccess(List<ConversationInfo> conversationInfos) {
                adapter.setConversations(conversationInfos);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(int errorCode) {
                Toast.makeText(getActivity(), "error: " + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
        adapter.setOnConversationItemClickListener(this);
        adapter.setNewConversationItemClickListener(this);
        adapter.setOnSelectionChangedListener(count -> {
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(count);
            }
        });
        applyMultiSelectMode();
    }

    @Override
    public void onConversationItemClick(ConversationInfo conversationInfo) {
        if (listener != null && conversationInfo != null) {
            listener.onPickOrCreateConversation(conversationInfo.conversation);
        }
    }

    @Override
    public void onNewConversationItemClick() {
        Intent intent = new Intent(getActivity(), PickOrCreateConversationTargetActivity.class);
        // 必须走 startPageForResult：裸 startActivityForResult 的 requestCode 会被
        // FragmentManager 换成内部生成的码，右栏拿到后送不回本页
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_CONVERSATION_TARGET);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO 现在就支持转发给一个人
        if (requestCode == REQUEST_CODE_PICK_CONVERSATION_TARGET && resultCode == Activity.RESULT_OK) {
            Conversation conversation = null;
            GroupInfo groupInfo = data.getParcelableExtra("groupInfo");
            if (groupInfo != null) {
                conversation = new Conversation(Conversation.ConversationType.Group, groupInfo.target, 0);
            } else {
                UserInfo userInfo = data.getParcelableExtra("userInfo");
                if (userInfo != null) {
                    conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
                }
            }
            if (listener != null && conversation != null) {
                listener.onPickOrCreateConversation(conversation);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public interface OnPickOrCreateConversationListener {
        void onPickOrCreateConversation(Conversation conversation);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
}
