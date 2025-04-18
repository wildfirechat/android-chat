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

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
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
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
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
        contactViewModel.acceptFriendRequest(friendRequest.target).observe(fragment, errorCode -> {
            if (errorCode == 0) {
                this.friendRequest.status = 1;
                acceptButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(fragment.getActivity(),
                    fragment.getString(R.string.contact_request_accept_error, errorCode),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onBind(FriendRequest friendRequest) {
        this.friendRequest = friendRequest;
        UserInfo userInfo = userViewModel.getUserInfo(friendRequest.target, false);

        if (userInfo != null) {
            nameTextView.setText(userViewModel.getUserDisplayNameEx(userInfo));
        } else {
            nameTextView.setText(fragment.getString(R.string.contact_unknown,
                "<" + friendRequest.target + ">"));
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
                acceptStatusTextView.setText(R.string.contact_request_accepted);
                break;
            default:
                acceptButton.setVisibility(View.GONE);
                acceptStatusTextView.setText(R.string.contact_request_rejected);
                break;
        }

        if (userInfo != null) {
            Glide.with(fragment).load(userInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop()).into(portraitImageView);
        }
    }

}
