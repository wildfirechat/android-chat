package cn.wildfire.chat.conversationlist.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.annotation.ConversationInfoType;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

@ConversationInfoType(type = Conversation.ConversationType.ChatRoom, line = 0)
@EnableContextMenu
public class ChatRoomConversationViewHolder extends ConversationViewHolder {

    public ChatRoomConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {

    }

}
