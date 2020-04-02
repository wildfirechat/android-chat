package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
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
        UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(conversationInfo.conversation.target, false);
        UserViewModel userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        String name = userViewModel.getUserDisplayName(userInfo);
        String portrait;
        portrait = userInfo.portrait;
        GlideApp
                .with(fragment)
                .load(portrait)
                .placeholder(UIUtils.getRoundedDrawable(R.mipmap.avatar_def, 4))
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(4)))
                .into(portraitImageView);
        nameTextView.setText(name);
    }

}
