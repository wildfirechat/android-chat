package cn.wildfire.chat.conversationlist.viewholder;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import cn.wildfire.chat.annotation.ConversationInfoType;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfirechat.model.Conversation;

@ConversationInfoType(type = Conversation.ConversationType.Channel, line = 0)
@EnableContextMenu
public class ChannelConversationViewHolder extends ConversationViewHolder {

    public ChannelConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

}
