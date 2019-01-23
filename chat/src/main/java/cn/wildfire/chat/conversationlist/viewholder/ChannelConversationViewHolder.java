package cn.wildfire.chat.conversationlist.viewholder;

import android.view.View;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.GlideApp;
import cn.wildfire.chat.annotation.ConversationInfoType;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.remote.ChatManager;

@ConversationInfoType(type = Conversation.ConversationType.Channel, line = 0)
@EnableContextMenu
public class ChannelConversationViewHolder extends ConversationViewHolder {

    public ChannelConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(conversationInfo.conversation.target, false);
        if (channelInfo != null) {
            nameTextView.setText(channelInfo.name);
            GlideApp
                    .with(fragment)
                    .load(channelInfo.portrait)
                    .placeholder(R.mipmap.ic_channel)
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .into(portraitImageView);
        }
    }

}
