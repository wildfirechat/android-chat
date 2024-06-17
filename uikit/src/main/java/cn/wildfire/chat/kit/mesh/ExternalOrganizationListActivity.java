/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ExternalOrganizationListActivity extends WfcBaseActivity {

    @Override
    protected void afterViews() {
        ExternalOrganizationListFragment fragment = new ExternalOrganizationListFragment();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }
}
