/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

/**
 * 直播聊天区域 Fragment（主播和观众页面共用）
 * <p>
 * 以 callId 作为聊天室 ID，加入聊天室并展示消息列表。
 * 每 3 分钟 re-join 一次以保持聊天室活跃。
 * </p>
 */
public class LiveMessageFragment extends Fragment {

    private static final String ARG_CALL_ID = "callId";
    private static final String ARG_SHOW_INPUT = "showInput";
    private static final long KEEP_ALIVE_INTERVAL = 3 * 60 * 1000L;
    private static final long MSG_TIMEOUT = 30 * 1000L;

    private RecyclerView messageRecyclerView;
    private TextView inputTextView;

    private String callId;
    private boolean showInput = true;
    private MessageViewModel messageViewModel;
    private final List<UiMessage> messages = new ArrayList<>();
    private MessageAdapter messageAdapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable keepAliveRunnable;
    private Runnable timeoutRunnable;

    public static LiveMessageFragment newInstance(String callId) {
        return newInstance(callId, true);
    }

    public static LiveMessageFragment newInstance(String callId, boolean showInput) {
        LiveMessageFragment fragment = new LiveMessageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, callId);
        args.putBoolean(ARG_SHOW_INPUT, showInput);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callId = getArguments() != null ? getArguments().getString(ARG_CALL_ID) : null;
        showInput = getArguments() == null || getArguments().getBoolean(ARG_SHOW_INPUT, true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_message, container, false);
        messageRecyclerView = view.findViewById(R.id.messageRecyclerView);
        inputTextView = view.findViewById(R.id.inputTextView);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messageAdapter = new MessageAdapter();
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        messageRecyclerView.setAdapter(messageAdapter);

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);
        messageViewModel.messageLiveData().observe(getViewLifecycleOwner(), uiMessages -> {
            if (uiMessages == null || callId == null) return;
            for (UiMessage uiMessage : uiMessages) {
                if (uiMessage.message.messageId == 0 || uiMessage.message.content.notLoaded > 0) continue;
                Conversation conv = uiMessage.message.conversation;
                if (conv.type == Conversation.ConversationType.ChatRoom
                        && conv.line == 0
                        && conv.target.equals(callId)) {
                    if (!contains(uiMessage)) {
                        messages.add(uiMessage);
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        messageRecyclerView.scrollToPosition(messages.size() - 1);
                    }
                }
            }
        });

        if (inputTextView != null) {
            inputTextView.setVisibility(showInput ? View.VISIBLE : View.GONE);
            inputTextView.setOnClickListener(v -> showInputDialog());
        }

        joinChatRoom();
        startKeepAlive();
        startTimeoutCleanup();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
        leaveChatRoom();
    }

    private void joinChatRoom() {
        if (callId != null) {
            ChatManager.Instance().joinChatRoom(callId, null);
        }
    }

    private void leaveChatRoom() {
        if (callId != null) {
            ChatManager.Instance().quitChatRoom(callId, null);
        }
    }

    private void startKeepAlive() {
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                joinChatRoom();
                handler.postDelayed(this, KEEP_ALIVE_INTERVAL);
            }
        };
        handler.postDelayed(keepAliveRunnable, KEEP_ALIVE_INTERVAL);
    }

    private void startTimeoutCleanup() {
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int lastExpired = -1;
                for (int i = 0; i < messages.size(); i++) {
                    if (now - messages.get(i).message.serverTime > MSG_TIMEOUT) {
                        lastExpired = i;
                    }
                }
                if (lastExpired >= 0) {
                    int count = lastExpired + 1;
                    messages.subList(0, count).clear();
                    messageAdapter.notifyItemRangeRemoved(0, count);
                }
                handler.postDelayed(this, MSG_TIMEOUT);
            }
        };
        handler.postDelayed(timeoutRunnable, MSG_TIMEOUT);
    }

    private boolean contains(UiMessage uiMessage) {
        for (UiMessage m : messages) {
            if (m.message.messageId != 0 && m.message.messageId == uiMessage.message.messageId) return true;
            if (m.message.messageUid != 0 && m.message.messageUid == uiMessage.message.messageUid) return true;
        }
        return false;
    }

    private void showInputDialog() {
        LiveMessageInputDialogFragment dialog = LiveMessageInputDialogFragment.newInstance(callId);
        dialog.show(getChildFragmentManager(), "liveInput");
    }

    // ---- Adapter ----

    static class TextMessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderTextView;
        TextView messageContentTextView;

        TextMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            messageContentTextView = itemView.findViewById(R.id.messageContentTextView);
        }

        void bind(UiMessage message) {
            senderTextView.setText(ChatManager.Instance().getUserDisplayName(message.message.sender) + ":");
            messageContentTextView.setText(message.message.digest());
        }
    }

    class MessageAdapter extends RecyclerView.Adapter<TextMessageViewHolder> {
        @NonNull
        @Override
        public TextMessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.live_message_item, parent, false);
            return new TextMessageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TextMessageViewHolder holder, int position) {
            holder.bind(messages.get(position));
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }
}
