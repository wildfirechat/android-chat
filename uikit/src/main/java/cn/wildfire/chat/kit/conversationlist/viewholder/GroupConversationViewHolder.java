/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ConversationInfoType;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.utils.WfcUtils;

@ConversationInfoType(type = Conversation.ConversationType.Group, line = 0)
@EnableContextMenu
public class GroupConversationViewHolder extends ConversationViewHolder implements Observer<GroupInfo> {
    private LiveData<GroupInfo> groupLiveData = null;

    TextView organizationGroupIndicator;

    public GroupConversationViewHolder(Fragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        organizationGroupIndicator = itemView.findViewById(R.id.organizationGroupIndicator);
    }

    public void removeLiveDataObserver() {
        super.removeLiveDataObserver();
        if (groupLiveData != null) {
            groupLiveData.removeObserver(this);
        }
    }

    @Override
    protected void onBindConversationInfo(ConversationInfo conversationInfo) {
        if (groupViewModel == null) {
            groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        }
        if (groupLiveData != null) {
            groupLiveData.removeObserver(this);
        }
        groupLiveData = groupViewModel.getGroupInfoAsync(conversationInfo.conversation.target, false);
        groupLiveData.observe(fragment, this);

    }

    @Override
    public void onChanged(GroupInfo groupInfo) {
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
            name = fragment.getString(R.string.group_chat);
            portrait = null;
        }

        Glide
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_group_chat)
            .transform(centerCropTransformation, roundedCornerTransformation)
            .into(portraitImageView);
        nameTextView.setText(name);
    }
}
