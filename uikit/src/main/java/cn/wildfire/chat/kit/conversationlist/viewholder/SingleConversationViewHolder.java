/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UserInfo;

@ConversationInfoType(type = Conversation.ConversationType.Single, line = 0)
@EnableContextMenu
public class SingleConversationViewHolder extends ConversationViewHolder implements Observer<UserInfo> {
    private LiveData<UserInfo> userInfoLiveData;
    private UserViewModel userViewModel = null;

    public SingleConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        if (userViewModel == null) {
            userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        }
        if (userInfoLiveData != null) {
            userInfoLiveData.removeObserver(this);
        }
        userInfoLiveData = userViewModel.getUserInfoAsync(conversationInfo.conversation.target, false);
        userInfoLiveData.observe(fragment, this);
    }

    public void removeLiveDataObserver() {
        super.removeLiveDataObserver();
        if (userInfoLiveData != null) {
            userInfoLiveData.removeObserver(this);
        }
    }

    @Override
    public void onChanged(UserInfo userInfo) {
        CharSequence name = this.userViewModel.getUserDisplayNameEx(userInfo);
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
