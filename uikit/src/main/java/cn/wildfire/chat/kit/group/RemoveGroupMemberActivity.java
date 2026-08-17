/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「移出群成员」在手机端的宿主壳。整页逻辑住在 {@link RemoveGroupMemberPageFragment}，
 * 手机端与平板右栏共用同一份，见 {@link BasePickGroupMemberPageFragment}。
 */
public class RemoveGroupMemberActivity extends WfcBaseActivity {

    public static final int RESULT_REMOVE_SUCCESS = 2;
    public static final int RESULT_REMOVE_FAIL = 3;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        RemoveGroupMemberPageFragment fragment = RemoveGroupMemberPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
