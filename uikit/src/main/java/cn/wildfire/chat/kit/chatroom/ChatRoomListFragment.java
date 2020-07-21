package cn.wildfire.chat.kit.chatroom;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.Conversation;

public class ChatRoomListFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatroom_list_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick({R2.id.chatRoomTextView_0, R2.id.chatRoomTextView_1, R2.id.chatRoomTextView_2})
    void joinChatRoom(View view) {
        String roomId = "chatroom1";
        String title = "野火IM聊天室1";
        if (view.getId() == R.id.chatRoomTextView_1) {
            roomId = "chatroom2";
            title = "野火IM聊天室2";
        } else if (view.getId() == R.id.chatRoomTextView_2) {
            roomId = "chatroom3";
            title = "野火IM聊天室3";
        }

        //todo 这里应该是先进入到ConversationActivity界面，然后在界面内joinchatroom？
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, roomId);
        intent.putExtra("conversation", conversation);
        intent.putExtra("conversationTitle", title);
        startActivity(intent);
    }
}
