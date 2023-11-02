/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;

@ConversationInfoType(type = Conversation.ConversationType.Group, line = 0)
@EnableContextMenu
public class GroupConversationViewHolder extends ConversationViewHolder {

    TextView organizationGroupIndicator;

    public GroupConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        organizationGroupIndicator =itemView.findViewById(R.id.organizationGroupIndicator);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        GroupInfo groupInfo = ChatManagerHolder.gChatManager.getGroupInfo(conversationInfo.conversation.target, false);
        String name;
        String portrait;
        if (groupInfo != null) {
            name = !TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name;
            portrait = groupInfo.portrait;
            if (groupInfo.type == GroupInfo.GroupType.Organization) {
                organizationGroupIndicator.setVisibility(View.VISIBLE);
            } else {
                organizationGroupIndicator.setVisibility(View.GONE);
            }
        } else {
            name = "群聊";
            portrait = null;
        }

       Glide
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_group_chat)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
            .into(portraitImageView);
        nameTextView.setText(name);

    }

}
