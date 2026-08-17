/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「移出群成员」整页。逐行搬自 {@link RemoveGroupMemberActivity}，只是宿主从 Activity 换成
 * {@link cn.wildfire.chat.kit.page.WfcPageHost}，于是手机端与平板右栏共用同一份。
 * <p>
 * 结果仍按原来的 {@code RESULT_REMOVE_SUCCESS} / {@code RESULT_REMOVE_FAIL} 回传。
 */
public class RemoveGroupMemberPageFragment extends BasePickGroupMemberPageFragment {

    private GroupViewModel groupViewModel;

    @Nullable
    public static RemoveGroupMemberPageFragment fromIntent(Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (args == null) {
            return null;
        }
        RemoveGroupMemberPageFragment fragment = new RemoveGroupMemberPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        if (groupInfo == null) {
            return;
        }
        // 管理员移不动群主
        GroupMember self = groupViewModel.getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        if (self != null && self.type == GroupMember.GroupMemberType.Manager) {
            pickUserViewModel.addUncheckableIds(Collections.singletonList(groupInfo.owner));
        }
    }

    @Override
    public int pageMenu() {
        return R.menu.group_remove_member;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.remove;
    }

    @Override
    protected int confirmLabelRes() {
        return R.string.delete;
    }

    @Override
    protected int confirmLabelWithCountRes() {
        return R.string.delete_with_count;
    }

    @Override
    protected void onConfirm() {
        ArrayList<String> memberIds = checkedMemberIds();
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.deleting)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        groupViewModel.removeGroupMember(groupInfo, memberIds, null, Collections.singletonList(0))
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result) {
                    Intent data = new Intent();
                    data.putStringArrayListExtra("memberIds", memberIds);
                    WfcPageCompat.setPageResult(this, RemoveGroupMemberActivity.RESULT_REMOVE_SUCCESS, data);
                    Toast.makeText(getActivity(), getString(R.string.del_member_success), Toast.LENGTH_SHORT).show();
                } else {
                    WfcPageCompat.setPageResult(this, RemoveGroupMemberActivity.RESULT_REMOVE_FAIL, null);
                    Toast.makeText(getActivity(), getString(R.string.del_member_fail), Toast.LENGTH_SHORT).show();
                }
                WfcPageCompat.finishPage(this);
            });
    }
}
