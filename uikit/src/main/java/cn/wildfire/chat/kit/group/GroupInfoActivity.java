/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;

public class GroupInfoActivity extends WfcBaseActivity {
    private String userId;
    private String groupId;
    private GroupInfo groupInfo;
    private boolean isJoined;
    private GroupViewModel groupViewModel;
    TextView groupNameTextView;
    ImageView groupPortraitImageView;
    Button actionButton;

    private MaterialDialog dialog;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.actionButton).setOnClickListener(v -> action());
    }

    protected void bindViews() {
        super.bindViews();
        groupNameTextView = findViewById(R.id.groupNameTextView);
        groupPortraitImageView = findViewById(R.id.portraitImageView);
        actionButton = findViewById(R.id.actionButton);
    }

    @Override
    protected void afterViews() {
        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);

        groupViewModel.groupInfoUpdateLiveData().observe(this, groupInfos -> {
            for (GroupInfo info : groupInfos) {
                if (info.target.equals(groupId)) {
                    this.groupInfo = info;
                    dismissLoading();
                    showGroupInfo(info);
                    updateActionButtonStatus();
                }
            }
        });


        groupInfo = groupViewModel.getGroupInfo(groupId, true);

        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userId = userViewModel.getUserId();

        // 本地没有相关群组信息
        if (groupInfo.updateDt == 0) {
            showLoading();
            return;
        }

        showGroupInfo(groupInfo);
    }

    private void updateActionButtonStatus() {
        if (groupInfo.memberDt < -1) {
            // 已退出群组
            actionButton.setText("加入群聊");
        } else if (groupInfo.memberDt == -1) {
            // 未加入
            actionButton.setText("加入群聊");
        } else {
            // 已加入群组
            this.isJoined = true;
            actionButton.setText("进入群聊");
        }
    }

    private void showLoading() {
        if (dialog == null) {
            dialog = new MaterialDialog.Builder(this)
                .progress(true, 100)
                .build();
            dialog.show();
        }
    }

    private void dismissLoading() {
        if (dialog == null || !dialog.isShowing()) {
            return;
        }
        dialog.dismiss();
        dialog = null;
    }

    private void showGroupInfo(GroupInfo groupInfo) {
        if (groupInfo == null) {
            return;
        }
        Glide.with(this)
            .load(groupInfo.portrait)
            .placeholder(R.mipmap.ic_group_chat)
            .into(groupPortraitImageView);
        groupNameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_info_activity;
    }

    void action() {
        if (isJoined) {
            Intent intent = ConversationActivity.buildConversationIntent(this, Conversation.ConversationType.Group, groupId, 0);
            startActivity(intent);
            finish();
        } else {
            groupViewModel.addGroupMember(groupInfo, Collections.singletonList(userId), null, Collections.singletonList(0)).observe(this, new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean aBoolean) {
                    if (aBoolean) {
                        Intent intent = ConversationActivity.buildConversationIntent(GroupInfoActivity.this, Conversation.ConversationType.Group, groupId, 0);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(GroupInfoActivity.this, R.string.add_member_fail, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}
