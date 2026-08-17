/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import java.util.Collections;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.ErrorCode;
import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;

/**
 * 扫群二维码之后落地的那一页：显示群名与头像，按钮是「加入群聊」或「进入群聊」。
 * <p>
 * 手机端装在 {@link GroupInfoActivity} 这个空壳里，平板上同一份实现进右栏。
 * 注意跟 {@code ConversationInfoActivity}（已经在群里时的群设置页）不是一回事。
 */
public class GroupInfoFragment extends Fragment {

    private TextView groupNameTextView;
    private ImageView groupPortraitImageView;
    private Button actionButton;

    private String userId;
    private String groupId;
    private String from;
    private GroupInfo groupInfo;
    private boolean isJoined;
    private GroupViewModel groupViewModel;
    private MaterialDialog dialog;

    /**
     * 没有 groupId 就无从查起，返回 null 让调用方放弃。
     */
    @Nullable
    public static GroupInfoFragment fromIntent(@Nullable Intent intent) {
        String groupId = intent == null ? null : intent.getStringExtra("groupId");
        if (TextUtils.isEmpty(groupId)) {
            return null;
        }
        GroupInfoFragment fragment = new GroupInfoFragment();
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        args.putString("from", intent.getStringExtra("from"));
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_info_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        groupNameTextView = view.findViewById(R.id.groupNameTextView);
        groupPortraitImageView = view.findViewById(R.id.portraitImageView);
        actionButton = view.findViewById(R.id.actionButton);
        actionButton.setOnClickListener(v -> action());

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        groupId = args.getString("groupId");
        from = args.getString("from");
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        groupViewModel.groupInfoUpdateLiveData().observe(getViewLifecycleOwner(), groupInfos -> {
            if (groupInfos == null) {
                return;
            }
            for (GroupInfo info : groupInfos) {
                if (info.target.equals(groupId)) {
                    groupInfo = info;
                    dismissLoading();
                    showGroupInfo(info);
                    updateActionButtonStatus();
                }
            }
        });

        groupInfo = groupViewModel.getGroupInfo(groupId, true);

        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userId = userViewModel.getUserId();

        // 本地没有相关群组信息，等上面那个 observer 把远端拉回来的结果送过来
        if (groupInfo.updateDt == 0) {
            showLoading();
            return;
        }

        showGroupInfo(groupInfo);
        updateActionButtonStatus();
    }

    @Override
    public void onDestroyView() {
        // 转菊花的对话框攥着宿主 window，页面提前关掉的话它会漏
        dismissLoading();
        super.onDestroyView();
    }

    private void updateActionButtonStatus() {
        isJoined = groupInfo.memberDt > 0;
        actionButton.setText(isJoined ? R.string.enter_group_chat : R.string.join_group_chat);
    }

    private void showLoading() {
        if (dialog == null) {
            dialog = new MaterialDialog.Builder(requireContext())
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
        if (TextUtils.isEmpty(groupInfo.portrait)) {
            AppServiceProvider appServiceProvider = WfcUIKit.getWfcUIKit().getAppServiceProvider();
            appServiceProvider.getGroupPortrait(groupId, new SimpleCallback<String>() {
                @Override
                public void onUiSuccess(String portrait) {
                    if (isAdded()) {
                        Glide.with(GroupInfoFragment.this)
                            .load(portrait)
                            .placeholder(R.mipmap.ic_group_chat)
                            .into(groupPortraitImageView);
                    }
                }

                @Override
                public void onUiFailure(int code, String msg) {

                }
            });
        } else {
            Glide.with(this)
                .load(groupInfo.portrait)
                .placeholder(R.mipmap.ic_group_chat)
                .into(groupPortraitImageView);
        }
        groupNameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
    }

    private void action() {
        if (isJoined) {
            openConversationAndFinish();
            return;
        }
        String memberExtra = GroupMemberSource.buildGroupMemberSourceExtra(GroupMemberSource.Type_QRCode, from);
        if (groupInfo.joinType == 2) {
            Toast.makeText(getActivity(), "Only admin can invite", Toast.LENGTH_SHORT).show();
        } else if (groupInfo.joinType == 3) {
            sendJoinGroupRequest();
        } else {
            groupViewModel.addGroupMemberEx(groupInfo, Collections.singletonList(userId), null,
                    Collections.singletonList(0), memberExtra)
                .observe(getViewLifecycleOwner(), booleanOperateResult -> {
                    if (booleanOperateResult.isSuccess()) {
                        openConversationAndFinish();
                    } else if (booleanOperateResult.getErrorCode() == ErrorCode.JOIN_GROUP_FAILED_NEED_VERIFY) {
                        sendJoinGroupRequest();
                    } else {
                        Toast.makeText(getActivity(), R.string.add_member_fail, Toast.LENGTH_SHORT).show();
                        WfcPageCompat.finishPage(this);
                    }
                });
        }
    }

    /**
     * 进群聊，并把本页去掉 —— 一张扫码落地页，进去之后没有再返回它的道理。
     */
    private void openConversationAndFinish() {
        Intent intent = ConversationActivity.buildConversationIntent(requireContext(),
            Conversation.ConversationType.Group, groupId, 0);
        if (WfcPageCompat.replaceSelfWithPage(this, intent)) {
            return;
        }
        ConversationRouter.open(this, intent);
        WfcPageCompat.finishPage(this);
    }

    private void sendJoinGroupRequest() {
        new MaterialDialog.Builder(requireContext())
            .title(R.string.join_group_need_verify)
            .input(getString(R.string.join_group_reason_hint), "", (dialog, input) ->
                groupViewModel.sendJoinGroupRequest(groupId, Collections.singletonList(userId), input.toString(), "")
                    .observe(getViewLifecycleOwner(), result -> {
                        dialog.dismiss();
                        if (isAdded()) {
                            Toast.makeText(getActivity(), result.isSuccess()
                                    ? getString(R.string.request_sent)
                                    : getString(R.string.request_failed, result.getErrorCode()),
                                Toast.LENGTH_SHORT).show();
                        }
                        WfcPageCompat.finishPage(this);
                    })).show();
    }
}
