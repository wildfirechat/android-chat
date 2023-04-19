/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings.blacklist;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.*;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class BlacklistViewHolder extends RecyclerView.ViewHolder {
    ImageView portraitImageView;
    TextView userNameTextView;

    public BlacklistViewHolder(View itemView) {
        super(itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        userNameTextView = itemView.findViewById(R.id.userNameTextView);
    }

    public void bind(String userId) {
        UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
        userNameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo));
        Glide.with(itemView.getContext()).load(userInfo.portrait).into(portraitImageView);
    }
}
