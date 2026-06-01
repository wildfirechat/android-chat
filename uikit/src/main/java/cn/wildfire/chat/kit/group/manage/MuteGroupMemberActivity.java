/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.BasePickGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;

public class MuteGroupMemberActivity extends BasePickGroupMemberActivity {
    private TextView confirmTv;
    private List<UIUserInfo> checkedGroupMembers;
    private boolean groupMuted = false;

    @Override
    protected void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos) {
        this.checkedGroupMembers = checkedUserInfos;
        this.updateMenuItemState();
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        groupMuted = getIntent().getBooleanExtra("groupMuted", false);
    }

    @Override
    protected int menu() {
        return R.menu.group_manage_add_manager;
    }


    @Override
    protected void afterMenus(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        View actionView = item.getActionView();
        confirmTv = actionView.findViewById(R.id.confirm_tv);
        confirmTv.setEnabled(false);
        confirmTv.setOnClickListener(v -> muteOrAllowGroupMembers());

        updateMenuItemState();
    }

    private void muteOrAllowGroupMembers() {
        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        List<String> memberIds = new ArrayList<>(checkedGroupMembers.size());
        for (UIUserInfo info : checkedGroupMembers) {
            memberIds.add(info.getUserInfo().uid);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(groupMuted ? R.string.adding_whitelist : R.string.muting_group_member)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        Observer<OperateResult<Boolean>> observer =
            booleanOperateResult -> {
                dialog.dismiss();
                if (booleanOperateResult.isSuccess()) {
                    finish();
                } else {
                    Toast.makeText(this, getString(groupMuted ? R.string.add_whitelist_error : R.string.set_mute_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            };
        if (groupMuted) {
            groupViewModel.allowGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(this, observer);
        } else {
            groupViewModel.muteGroupMember(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
                .observe(this, observer);
        }
    }

    private void updateMenuItemState() {
        if (confirmTv == null) {
            return;
        }
        if (checkedGroupMembers == null || checkedGroupMembers.isEmpty()) {
            confirmTv.setText(R.string.contact_pick_confirm);
            confirmTv.setEnabled(false);
        } else {
            confirmTv.setText(getString(R.string.contact_pick_confirm_with_count, checkedGroupMembers.size()));
            confirmTv.setEnabled(true);
        }
    }
}
