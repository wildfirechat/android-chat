/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.group.JoinGroupFragment;
import cn.wildfire.chat.kit.domain.TGroupJoinRequests;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class JoinGroupListRequestViewHolder extends RecyclerView.ViewHolder {
    private JoinGroupFragment fragment;
    private JoinGroupRequestListAdapter adapter;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;


    ImageView portraitImageView;
    TextView nameTextView;
    TextView introTextView;
    Button acceptButton;
    Button rejectButton;
    TextView groupStatusTextView;


    public JoinGroupListRequestViewHolder(JoinGroupFragment fragment, JoinGroupRequestListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        bindViews(itemView);
        bindEvents(itemView);
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.agreeButton).setOnClickListener(_v -> accept());
        itemView.findViewById(R.id.rejectButton).setOnClickListener(_v -> reject());
    }

    void accept() {
        handleApproval(1);
    }

    private void reject() {
        handleApproval(2);
    }

    private void handleApproval(int status) {
        int position = getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
            List<TGroupJoinRequests> requests = adapter.getGroupJoinRequests();
            if (requests != null) {
                TGroupJoinRequests request = requests.get(position);
                // 添加空指针保护
                MutableLiveData<Integer> integerMutableLiveData = contactViewModel.handleGroupRequest(
                    request.getRequestId(),
                    request.getGroupId(), 
                    request.getApplicantId(), 
                    status
                );
            }
        }
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        introTextView = itemView.findViewById(R.id.introTextView);
        acceptButton = itemView.findViewById(R.id.agreeButton);
        rejectButton = itemView.findViewById(R.id.rejectButton);
        groupStatusTextView = itemView.findViewById(R.id.groupStatusTextView);
    }

 /*   void accept() {
       *//* contactViewModel.acceptFriendRequest(friendRequest.target).observe(fragment, errorCode -> {
            if (errorCode == 0) {
                this.friendRequest.status = 1;
                acceptButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(fragment.getActivity(),
                    fragment.getString(R.string.contact_request_accept_error, errorCode),
                    Toast.LENGTH_SHORT).show();
            }
        });*//*
        Log.e("weiAndKe", "点击同意入群按钮");
    }*/

    public void onBind(TGroupJoinRequests groupJoinRequests) {
        UserInfo userInfo = userViewModel.getUserInfo(groupJoinRequests.getApplicantId(), false);

        // 显示用户名
        if (userInfo != null) {
            nameTextView.setText(userViewModel.getUserDisplayNameEx(userInfo));
        } else {
            nameTextView.setText(fragment.getString(R.string.contact_unknown,
                    "<" + groupJoinRequests.getApplicantId() + ">"));
        }

        // 显示申请加入的群组名称
        GroupInfo groupInfo = ChatManager.Instance().getGroupInfo(
                groupJoinRequests.getGroupId(),
                false // 从缓存获取
        );
        String groupName = groupInfo != null ? groupInfo.name : "未知群组";
        introTextView.setText("申请加入 " + groupName);  // 直接显示群组名称，不再显示备注

        // 更新状态显示逻辑
        if (groupJoinRequests.getStatus() == 0) {
            acceptButton.setVisibility(View.VISIBLE);
            rejectButton.setVisibility(View.VISIBLE);
            groupStatusTextView.setVisibility(View.GONE);
        } else {
            acceptButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            groupStatusTextView.setVisibility(View.VISIBLE);
            if (groupJoinRequests.getStatus() == 1) {
                groupStatusTextView.setText(R.string.contact_request_accepted);
            } else {
                groupStatusTextView.setText(R.string.contact_request_rejected);
            }
        }

        if (userInfo != null) {
            Glide.with(fragment).load(userInfo.portrait)
                    .apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop())
                    .into(portraitImageView);
        }
    }

}
