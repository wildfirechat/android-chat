/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationSearchResult;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ConversationViewHolder extends ResultItemViewHolder<ConversationSearchResult> {
    ImageView portraitImageView;
    TextView nameTextView;
    TextView descTextView;

    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;

    public ConversationViewHolder(Fragment fragment, View itemView) {
        super(fragment, itemView);
        bindViews(itemView);

        userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
    }

    @Override
    public void onBind(String keyword, ConversationSearchResult conversationSearchResult) {
        Conversation conversation = conversationSearchResult.conversation;
        if (conversation.type == Conversation.ConversationType.Single) {
            UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
            if (userInfo != null) {
                Glide.with(fragment).load(userInfo.portrait).apply(new RequestOptions().centerCrop().placeholder(R.mipmap.avatar_def)).into(portraitImageView);
                nameTextView.setText(userViewModel.getUserDisplayNameEx(userInfo));
            }
        } else {
            GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
            if (groupInfo != null) {
                Glide.with(fragment).load(groupInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_chat).centerCrop()).into(portraitImageView);
                nameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
            }
        }

        if (conversationSearchResult.marchedMessage != null) {
            descTextView.setText(conversationSearchResult.marchedMessage.digest());
        } else {
            descTextView.setText(fragment.getString(R.string.matched_records_count, conversationSearchResult.marchedCount));
        }
    }
}
