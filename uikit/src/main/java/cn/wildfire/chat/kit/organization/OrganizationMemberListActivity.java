/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.os.Bundle;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 组织架构成员列表在手机端的宿主壳。菜单里的搜索框装配住在
 * {@link OrganizationMemberListFragment}（它才是被搜索操作的那一方），两端共用。
 */
public class OrganizationMemberListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        OrganizationMemberListFragment fragment = new OrganizationMemberListFragment();
        Bundle args = new Bundle();
        args.putInt("organizationId", getIntent().getIntExtra("organizationId", 0));
        args.putBoolean("pick", getIntent().getBooleanExtra("pick", false));
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
