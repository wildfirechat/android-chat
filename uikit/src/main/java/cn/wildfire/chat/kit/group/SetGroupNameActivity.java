/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「修改群名称」在手机端的宿主壳。整页逻辑住在 {@link SetGroupNamePageFragment}，
 * 手机端与平板右栏共用同一份。
 */
public class SetGroupNameActivity extends WfcBaseActivity {

    public static final int RESULT_SET_GROUP_NAME_SUCCESS = 100;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        SetGroupNamePageFragment fragment = SetGroupNamePageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
