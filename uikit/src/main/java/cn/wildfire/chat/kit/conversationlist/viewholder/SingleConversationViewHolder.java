/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

@ConversationInfoType(type = Conversation.ConversationType.Single, line = 0)
@EnableContextMenu
public class SingleConversationViewHolder extends ConversationViewHolder {
    public SingleConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.getUserInfoAsync(conversationInfo.conversation.target, false)
            .observe(fragment, userInfo -> {
                CharSequence name = userViewModel.getUserDisplayNameEx(userInfo);
                String portrait;
                portrait = userInfo.portrait;
                Glide
                    .with(fragment)
                    .load(portrait)
                    .placeholder(R.mipmap.avatar_def)
                    .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
                    .into(portraitImageView);
                nameTextView.setText(name);
            });
    }

}
