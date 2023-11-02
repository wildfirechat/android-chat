/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.FriendRequest;
import cn.wildfirechat.model.UserInfo;

public class FriendRequestViewHolder extends RecyclerView.ViewHolder {
    private FriendRequestListFragment fragment;
    private FriendRequestListAdapter adapter;
    private FriendRequest friendRequest;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;

    ImageView portraitImageView;
    TextView nameTextView;
    TextView introTextView;
    Button acceptButton;
    TextView acceptStatusTextView;

    public FriendRequestViewHolder(FriendRequestListFragment fragment, FriendRequestListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        bindViews(itemView);
        bindEvents(itemView);
        userViewModel =ViewModelProviders.of(fragment).get(UserViewModel.class);
        contactViewModel = ViewModelProviders.of(fragment).get(ContactViewModel.class);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.acceptButton).setOnClickListener(_v -> accept());
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        introTextView = itemView.findViewById(R.id.introTextView);
        acceptButton = itemView.findViewById(R.id.acceptButton);
        acceptStatusTextView = itemView.findViewById(R.id.acceptStatusTextView);
    }

    void accept() {
        contactViewModel.acceptFriendRequest(friendRequest.target).observe(fragment, aBoolean -> {
            if (aBoolean) {
                this.friendRequest.status = 1;
                acceptButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(fragment.getActivity(), "操作失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBind(FriendRequest friendRequest) {
        this.friendRequest = friendRequest;
        UserInfo userInfo = userViewModel.getUserInfo(friendRequest.target, false);

        if (userInfo != null && !TextUtils.isEmpty(userInfo.displayName)) {
            nameTextView.setText(userInfo.displayName);
        } else {
            nameTextView.setText("<" + friendRequest.target + ">");
        }
        if (!TextUtils.isEmpty(friendRequest.reason)) {
            introTextView.setText(friendRequest.reason);
        }
        // TODO status

        switch (friendRequest.status) {
            case 0:
                acceptButton.setVisibility(View.VISIBLE);
                acceptStatusTextView.setVisibility(View.GONE);
                break;
            case 1:
                acceptButton.setVisibility(View.GONE);
                acceptStatusTextView.setText("已添加");
                break;
            default:
                acceptButton.setVisibility(View.GONE);
                acceptStatusTextView.setText("已拒绝");
                break;
        }

        if (userInfo != null) {
            Glide.with(fragment).load(userInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop()).into(portraitImageView);
        }
    }

}
