/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.service.IMService;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.GeneralCallback;

public class GroupInfoActivity extends WfcBaseActivity {
    private String userId;
    private String groupId;
    private String from;
    private GroupInfo groupInfo;
    private boolean isJoined;
    private GroupViewModel groupViewModel;
    TextView groupNameTextView;
    EditText reasonEditText;
    ImageView groupPortraitImageView;
    Button actionButton;
    Button actionButtonByApprove;

    private MaterialDialog dialog;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.actionButton).setOnClickListener(v -> action());
        findViewById(R.id.actionButtonByApprove).setOnClickListener(v -> actionByApprove());
    }


    protected void bindViews() {
        super.bindViews();
        groupNameTextView = findViewById(R.id.groupNameTextView);
        reasonEditText = findViewById(R.id.reasonEditText);
        groupPortraitImageView = findViewById(R.id.portraitImageView);
        actionButton = findViewById(R.id.actionButton);
        actionButtonByApprove = findViewById(R.id.actionButtonByApprove);
    }

    @Override
    protected void afterViews() {
        Intent intent = getIntent();
        groupId = intent.getStringExtra("groupId");
        from = intent.getStringExtra("from");
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
        updateActionButtonStatus();
    }

    private void updateActionButtonStatus() {
        if (groupInfo.memberDt < -1) {
            // 已退出群组
            actionButton.setText(R.string.join_group_chat);
            actionButtonByApprove.setVisibility(View.VISIBLE);
        } else if (groupInfo.memberDt == -1) {
            // 未加入
            actionButton.setText(R.string.join_group_chat);
            actionButtonByApprove.setVisibility(View.VISIBLE);

        } else {
            // 已加入群组
            this.isJoined = true;
            actionButton.setText(R.string.enter_group_chat);
            actionButtonByApprove.setVisibility(View.GONE);
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
            String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_QRCode, this.from);
            groupViewModel.addGroupMember(groupInfo, Collections.singletonList(userId), null, Collections.singletonList(0), memberExtra).observe(this, new Observer<Boolean>() {
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

    private void actionByApprove() {
        EditText reasonEdit = findViewById(R.id.reasonEditText);
        String reason = reasonEdit.getText().toString();

        if (TextUtils.isEmpty(reason)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("提示")
                    .setMessage("请填写申请理由")
                    .setPositiveButton("确定", null)
                    .show();
            return;
        }

        IMService.Instance().submitGroupApply(groupId, userId, reason, new GeneralCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(GroupInfoActivity.this, "申请已提交，请等待审核", Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onFail(int errorCode) {
                runOnUiThread(() -> {
                    String errorMsg = "申请失败，错误码：" + errorCode;
                    if (errorCode == 409) {
                        errorMsg = "您已提交过申请，请勿重复提交";
                    }
                    new MaterialAlertDialogBuilder(GroupInfoActivity.this)
                            .setTitle("提交失败")
                            .setMessage(errorMsg)
                            .setPositiveButton("确定", null)
                            .show();
                });
            }
        });
    }
}
