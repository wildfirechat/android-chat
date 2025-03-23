/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.widget.Toast;

import androidx.lifecycle.Observer;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;

public class GroupMuteOrAllowActivity extends WfcBaseActivity {
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    SwitchMaterial switchButton;

    protected void bindViews() {
        super.bindViews();
        switchButton = findViewById(R.id.muteSwitchButton);
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_manage_mute_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo != null) {
            init();
        } else {
            finish();
        }
    }

    private void init() {
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(this, new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                if (groupInfos != null) {
                    for (GroupInfo info : groupInfos) {
                        if (info.target.equals(groupInfo.target)) {
                            boolean oMuted = groupInfo.mute == 1;
                            boolean nMuted = info.mute == 1;
                            groupInfo = info;
                            break;
                        }
                    }
                }

            }
        });
        switchButton.setChecked(groupInfo.mute == 1);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            groupViewModel.muteAll(groupInfo.target, isChecked, null, Collections.singletonList(0)).observe(this, booleanOperateResult -> {
                if (!booleanOperateResult.isSuccess()) {
                    switchButton.setChecked(!isChecked);
                    Toast.makeText(this, getString(R.string.mute_operation_failed, booleanOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                } else {
                    initGroupMemberMuteListFragment(true);
                }
            });
        });

        initGroupMemberMuteListFragment(false);
    }

    private GroupMemberMuteOrAllowListFragment fragment;

    private void initGroupMemberMuteListFragment(boolean forceUpdate) {
        // 全局禁言
        if (groupInfo.mute == 1) {
            if (fragment == null || forceUpdate) {
                fragment = GroupMemberMuteOrAllowListFragment.newInstance(groupInfo, true);
            }
        } else {
            if (fragment == null || forceUpdate) {
                fragment = GroupMemberMuteOrAllowListFragment.newInstance(groupInfo, false);
            }
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
