package cn.wildfire.chat.conversationlist.viewholder;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfire.chat.annotation.ConversationInfoType;

import cn.wildfirechat.model.Conversation;

@ConversationInfoType(type = Conversation.ConversationType.Single, line = 0)
@EnableContextMenu
public class SingleConversationViewHolder extends ConversationViewHolder {
    public SingleConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

}
