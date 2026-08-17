/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.group.BasePickGroupMemberPageFragment;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 「禁言群成员 / 加白名单」整页。逐行搬自 {@link MuteGroupMemberActivity}。
 * <p>
 * 群已全员禁言（{@code groupMuted}）时本页是「把人加进白名单」，否则是「禁言这些人」，
 * 两种形态共用同一个页面，只是文案与调用的接口不同 —— 与改造前一致。
 */
public class MuteGroupMemberPageFragment extends BasePickGroupMemberPageFragment {

    private static final String ARG_GROUP_MUTED = "groupMuted";

    private boolean groupMuted;

    @Nullable
    public static MuteGroupMemberPageFragment fromIntent(Intent intent) {
        Bundle args = argsFromIntent(intent);
        if (args == null) {
            return null;
        }
        args.putBoolean(ARG_GROUP_MUTED, intent.getBooleanExtra(ARG_GROUP_MUTED, false));
        MuteGroupMemberPageFragment fragment = new MuteGroupMemberPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupMuted = getArguments() != null && getArguments().getBoolean(ARG_GROUP_MUTED, false);
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
            .content(groupMuted ? R.string.adding_whitelist : R.string.muting_group_member)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        Observer<OperateResult<Boolean>> observer = result -> {
            dialog.dismiss();
            if (result.isSuccess()) {
                WfcPageCompat.finishPage(this);
            } else {
                Toast.makeText(getActivity(), getString(
                        groupMuted ? R.string.add_whitelist_error : R.string.set_mute_error, result.getErrorCode()),
                    Toast.LENGTH_SHORT).show();
            }
        };
        if (groupMuted) {
            groupViewModel.allowGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(getViewLifecycleOwner(), observer);
        } else {
            groupViewModel.muteGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(getViewLifecycleOwner(), observer);
        }
    }
}
