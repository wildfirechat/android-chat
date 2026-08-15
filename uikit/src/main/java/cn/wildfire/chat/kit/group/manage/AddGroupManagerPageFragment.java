/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.BasePickGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 「添加群管理员」整页。逐行搬自 {@link AddGroupManagerActivity}。
 */
public class AddGroupManagerPageFragment extends BasePickGroupMemberPageFragment {

    @Nullable
    public static AddGroupManagerPageFragment fromIntent(Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (args == null) {
            return null;
        }
        AddGroupManagerPageFragment fragment = new AddGroupManagerPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int pageMenu() {
        return R.menu.group_manage_add_manager;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.confirm;
    }

    @Override
    protected int confirmLabelRes() {
        return R.string.contact_pick_confirm;
    }

    @Override
    protected int confirmLabelWithCountRes() {
        return R.string.contact_pick_confirm_with_count;
    }

    @Override
    protected void onConfirm() {
        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        ArrayList<String> memberIds = checkedMemberIds();
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.adding)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        groupViewModel.setGroupManager(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    WfcPageCompat.finishPage(this);
                } else {
                    Toast.makeText(getActivity(),
                        getString(R.string.set_group_manager_error, result.getErrorCode()),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}
