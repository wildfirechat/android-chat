package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UserInfo;

@ConversationInfoType(type = Conversation.ConversationType.Single, line = 0)
@EnableContextMenu
public class SingleConversationViewHolder extends ConversationViewHolder {
    public SingleConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfox(conversationInfo.conversation.target, false);
        String name;
        String portrait;
        name = userInfo.displayName;
        portrait = userInfo.portrait;
        GlideApp
                .with(fragment)
                .load(portrait)
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(10))
                .into(portraitImageView);
        nameTextView.setText(name);
    }

}
