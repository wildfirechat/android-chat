package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
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
        String name;
        String portrait;
        if (channelInfo != null) {
            name = channelInfo.name;
            portrait = channelInfo.portrait;
        } else {
            name = "Channel<" + conversationInfo.conversation.target + ">";
            portrait = null;
        }
        nameTextView.setText(name);
        GlideApp
                .with(fragment)
                .load(portrait)
                .placeholder(R.mipmap.ic_channel)
                .transforms(new CenterCrop(), new RoundedCorners(10))
                .into(portraitImageView);
    }

    @ConversationContextMenuItem(tag = ConversationContextMenuItemTags.TAG_UNSUBSCRIBE,
            title = "取消收听",
            confirm = true,
            confirmPrompt = "确认取消订阅频道？",
            priority = 0)
    public void unSubscribeChannel(View itemView, ConversationInfo conversationInfo) {
        conversationListViewModel.unSubscribeChannel(conversationInfo);
    }
}
