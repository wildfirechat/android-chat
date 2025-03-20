/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.os.Bundle;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class OrganizationMemberListActivity extends WfcBaseActivity {
    private int organizationId;
    private boolean pick;

    @Override
    protected void afterViews() {
        this.organizationId = getIntent().getIntExtra("organizationId", 0);
        this.pick = getIntent().getBooleanExtra("pick", false);
        OrganizationMemberListFragment fragment = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putInt("organizationId", organizationId);
        args.putBoolean("pick", pick);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}
