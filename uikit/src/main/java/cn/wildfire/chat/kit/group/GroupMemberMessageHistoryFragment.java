/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetMessageCallback;

public class GroupMemberMessageHistoryFragment extends Fragment {
    RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private boolean loading;

    private String groupId;
    private String groupMemberId;
    private GroupMemberMessageHistoryAdapter adapter;

    public static GroupMemberMessageHistoryFragment newInstance(String groupId, String groupMemberId) {
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        args.putString("groupMemberId", groupMemberId);
        GroupMemberMessageHistoryFragment fragment = new GroupMemberMessageHistoryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        groupId = args.getString("groupId");
        groupMemberId = args.getString("groupMemberId");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_member_message_history_fragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1) && !loading) {
                    int lastVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition();
                    if (lastVisibleItem > adapter.getItemCount() - 3) {
                        loadMoreOldMessages();
                    }
                }
            }
        });
        adapter = new GroupMemberMessageHistoryAdapter();
        recyclerView.setAdapter(adapter);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        init();
        return view;
    }

    private void init() {
        this.loading = true;
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId, 0);
        ChatManager.Instance().getUserMessages(groupMemberId, conversation, 0, true, 20, new GetMessageCallback() {
            @Override
            public void onSuccess(List<Message> messages, boolean hasMore) {
                Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                loading = false;
                Collections.reverse(messages);
                adapter.setMessages(messages);
            }

            @Override
            public void onFail(int errorCode) {
                loading = false;
            }
        });
    }

    private void loadMoreOldMessages() {
        this.loading = true;
        List<Message> messages = adapter.getMessages();
        if (messages == null || messages.size() == 0) {
            return;
        }

        long oldestMessageId = messages.get(messages.size() - 1).messageId;

        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId, 0);
        ChatManager.Instance().getUserMessages(groupMemberId, conversation, oldestMessageId, true, 20, new GetMessageCallback() {
            @Override
            public void onSuccess(List<Message> messages, boolean hasMore) {
                Activity activity = getActivity();
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                loading = false;
                Collections.reverse(messages);
                adapter.addMessages(messages);
            }

            @Override
            public void onFail(int errorCode) {
                loading = false;
            }
        });
    }
}
