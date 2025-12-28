package cn.wildfire.chat.kit.group.manage;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.JoinGroupRequest;
import cn.wildfirechat.model.UserInfo;

public class JoinGroupRequestViewHolder extends RecyclerView.ViewHolder {
    private JoinGroupRequest request;
    private final JoinGroupRequestListFragment fragment;
    private final ImageView portraitImageView;
    private final TextView nameTextView;
    private final TextView introTextView;
    private final TextView acceptStatusTextView;
    private final Button acceptButton;

    public JoinGroupRequestViewHolder(JoinGroupRequestListFragment fragment, View itemView) {
        super(itemView);
        this.fragment = fragment;
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        introTextView = itemView.findViewById(R.id.introTextView);
        acceptStatusTextView = itemView.findViewById(R.id.acceptStatusTextView);
        acceptButton = itemView.findViewById(R.id.acceptButton);
    }

    public void onBind(JoinGroupRequest request) {
        this.request = request;
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(request.memberId, false);
        if (userInfo != null) {
            Glide.with(fragment).load(userInfo.portrait).into(portraitImageView);
            nameTextView.setText(userViewModel.getUserDisplayName(userInfo));
        }

        if (!TextUtils.isEmpty(request.reason)) {
            introTextView.setText(request.reason);
        } else {
            introTextView.setText("No reason");
        }

        if (request.status == 0) {
            acceptButton.setVisibility(View.VISIBLE);
            acceptStatusTextView.setVisibility(View.GONE);
            acceptButton.setOnClickListener(v -> {
                GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
                groupViewModel.handleJoinGroupRequest(request.groupId, request.memberId, request.requestUserId, 1, null, Collections.singletonList(0)).observe(fragment, result -> {
                    if (result.isSuccess()) {
                        acceptButton.setVisibility(View.GONE);
                        acceptStatusTextView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(fragment.getActivity(), "Accept failed", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        } else {
            acceptButton.setVisibility(View.GONE);
            acceptStatusTextView.setVisibility(View.VISIBLE);
            if (request.status == 1) {
                acceptStatusTextView.setText(R.string.contact_request_accepted);
            } else {
                acceptStatusTextView.setText(R.string.contact_request_rejected);
            }
        }
    }
}
