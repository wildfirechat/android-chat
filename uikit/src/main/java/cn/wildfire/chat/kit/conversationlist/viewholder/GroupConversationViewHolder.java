/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.utils.WfcUtils;

@ConversationInfoType(type = Conversation.ConversationType.Group, line = 0)
@EnableContextMenu
public class GroupConversationViewHolder extends ConversationViewHolder {

    TextView organizationGroupIndicator;

    public GroupConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        organizationGroupIndicator = itemView.findViewById(R.id.organizationGroupIndicator);
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
        groupViewModel.getGroupInfoAsync(conversationInfo.conversation.target, false)
            .observe(fragment, groupInfo -> {
                CharSequence name;
                String portrait;
                if (groupInfo != null) {
                    String tmpName = ChatManager.Instance().getGroupDisplayName(groupInfo);
                    portrait = groupInfo.portrait;
                    if (groupInfo.type == GroupInfo.GroupType.Organization) {
                        organizationGroupIndicator.setVisibility(View.VISIBLE);
                    } else {
                        organizationGroupIndicator.setVisibility(View.GONE);
                    }
                    if (WfcUtils.isExternalTarget(groupInfo.target)) {
                        name = WfcUtils.buildExternalDisplayNameSpannableString(tmpName, 14);
                    } else {
                        name = tmpName;
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
            });

    }

}
