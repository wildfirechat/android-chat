/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.TextEditPageFragment;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.ModifyGroupInfoType;

/**
 * 「修改群名称」整页，逐行搬自 {@link SetGroupNameActivity}。
 * 结果仍按原来的 {@code RESULT_SET_GROUP_NAME_SUCCESS} + {@code groupName} 回传。
 */
public class SetGroupNamePageFragment extends TextEditPageFragment {

    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    public static SetGroupNamePageFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        SetGroupNamePageFragment fragment = new SetGroupNamePageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static SetGroupNamePageFragment fromIntent(Intent intent) {
        GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
        return groupInfo == null ? null : newInstance(groupInfo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments() == null ? null : getArguments().getParcelable("groupInfo");
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_set_name_activity;
    }

    @Override
    protected int editTextId() {
        return R.id.nameEditText;
    }

    @Override
    public int pageMenu() {
        return R.menu.group_set_group_name;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.confirm;
    }

    @Override
    protected void onPageViewCreated(@NonNull View view) {
        if (groupInfo != null) {
            setText(groupInfo.name);
        }
    }

    @Override
    protected void onConfirm(String text) {
        groupInfo.name = text;
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.processing)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        groupViewModel.modifyGroupInfo(groupInfo.target, ModifyGroupInfoType.Modify_Group_Name,
                groupInfo.name, null, Collections.singletonList(0))
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Toast.makeText(getActivity(), getString(R.string.modify_group_name_success), Toast.LENGTH_SHORT).show();
                    Intent data = new Intent();
                    data.putExtra("groupName", groupInfo.name);
                    WfcPageCompat.setPageResult(this, SetGroupNameActivity.RESULT_SET_GROUP_NAME_SUCCESS, data);
                    finishPage();
                } else {
                    Toast.makeText(getActivity(),
                        getString(R.string.modify_group_name_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
