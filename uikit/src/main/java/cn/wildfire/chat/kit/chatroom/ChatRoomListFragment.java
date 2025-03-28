/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.chatroom;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;

public class ChatRoomListFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatroom_list_fragment, container, false);
        bindEvents(view);
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.chatRoomTextView_0).setOnClickListener(this::joinChatRoom);
        view.findViewById(R.id.chatRoomTextView_1).setOnClickListener(this::joinChatRoom);
        view.findViewById(R.id.chatRoomTextView_2).setOnClickListener(this::joinChatRoom);
    }

    void joinChatRoom(View view) {
        String roomId = "chatroom1";
        String title = "绒讯聊天室1";
        if (view.getId() == R.id.chatRoomTextView_1) {
            roomId = "chatroom2";
            title = "绒讯聊天室2";
        } else if (view.getId() == R.id.chatRoomTextView_2) {
            roomId = "chatroom3";
            title = "绒讯聊天室3";
        }

        //todo 这里应该是先进入到ConversationActivity界面，然后在界面内joinchatroom？
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, roomId);
        intent.putExtra("conversation", conversation);
        intent.putExtra("conversationTitle", title);
        startActivity(intent);
    }
}
