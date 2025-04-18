/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@ConversationInfoType(type = Conversation.ConversationType.SecretChat, line = 0)
@EnableContextMenu
public class SecretConversationViewHolder extends ConversationViewHolder {

    public SecretConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        secretChatIndicator.setVisibility(View.VISIBLE);
        SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversationInfo.conversation.target);
        if (secretChatInfo == null) {
            return;
        }
        String userId = secretChatInfo.getUserId();
        if (TextUtils.isEmpty(userId)) {
            return;
        }
        UserInfo userInfo = ChatManagerHolder.gChatManager.getUserInfo(userId, false);
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        String name = userViewModel.getUserDisplayName(userInfo);
        String portrait;
        portrait = userInfo.portrait;
        Glide
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.avatar_def)
            .transform(centerCropTransformation, roundedCornerTransformation)
            .into(portraitImageView);
        nameTextView.setText(name);
    }

}
