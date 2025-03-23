/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.group.BasePickGroupMemberActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;

public class AddGroupManagerActivity extends BasePickGroupMemberActivity {
    private MenuItem menuItem;
    private List<UIUserInfo> checkedGroupMembers;

    @Override
    protected void onGroupMemberChecked(List<UIUserInfo> checkedUserInfos) {
        this.checkedGroupMembers = checkedUserInfos;
        this.updateMenuItemState();
    }

    @Override
    protected int menu() {
        return R.menu.group_manage_add_manager;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.confirm);
        this.updateMenuItemState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            setGroupManager();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setGroupManager() {
        GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        List<String> memberIds = new ArrayList<>(checkedGroupMembers.size());
        for (UIUserInfo info : checkedGroupMembers) {
            memberIds.add(info.getUserInfo().uid);
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.adding)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        groupViewModel.setGroupManager(groupInfo.target, true, memberIds, null, Collections.singletonList(0))
            .observe(this, booleanOperateResult -> {
                dialog.dismiss();
                if (booleanOperateResult.isSuccess()) {
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.set_group_manager_error, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void updateMenuItemState() {
        if (menuItem == null) {
            return;
        }
        if (checkedGroupMembers == null || checkedGroupMembers.isEmpty()) {
            menuItem.setTitle(R.string.contact_pick_confirm);
            menuItem.setEnabled(false);
        } else {
            menuItem.setTitle(getString(R.string.contact_pick_confirm_with_count, checkedGroupMembers.size()));
            menuItem.setEnabled(true);
        }
    }
}
