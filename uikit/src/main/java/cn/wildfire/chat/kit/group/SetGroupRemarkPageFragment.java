/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.TextEditPageFragment;
import cn.wildfirechat.model.GroupInfo;

/**
 * 「设置群备注」整页，逐行搬自 {@link SetGroupRemarkActivity}。
 * <p>
 * 允许留空（清掉备注），所以保存菜单一直可用 —— 与改造前一致。
 */
public class SetGroupRemarkPageFragment extends TextEditPageFragment {

    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    public static SetGroupRemarkPageFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        SetGroupRemarkPageFragment fragment = new SetGroupRemarkPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static SetGroupRemarkPageFragment fromIntent(Intent intent) {
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
        return R.layout.group_set_remark_activity;
    }

    @Override
    protected int editTextId() {
        return R.id.remarkEditText;
    }

    @Override
    public int pageMenu() {
        return R.menu.group_set_group_remark;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.confirm;
    }

    @Override
    protected boolean allowEmptyText() {
        return true;
    }

    @Override
    protected void onPageViewCreated(@NonNull View view) {
        if (groupInfo != null) {
            setText(groupInfo.remark);
        }
    }

    @Override
    protected void onConfirm(String text) {
        if (TextUtils.equals(text, groupInfo.remark)) {
            finishPage();
            return;
        }
        groupInfo.remark = text;
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.processing)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        groupViewModel.setGroupRemark(groupInfo.target, groupInfo.remark)
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Toast.makeText(getActivity(), getString(R.string.modify_group_remark_success), Toast.LENGTH_SHORT).show();
                    finishPage();
                } else {
                    Toast.makeText(getActivity(),
                        getString(R.string.modify_group_remark_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
