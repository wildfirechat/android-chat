/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「加群成员」在手机端的宿主壳。
 * <p>
 * 整页逻辑（列表 + 确定菜单 + 加人）住在 {@link AddGroupMemberPageFragment}，
 * 手机端与平板右栏共用同一份。改造前这些代码在本类里，页面因此无法脱离 Activity 存在。
 */
public class AddGroupMemberActivity extends WfcBaseActivity {

    public static final int RESULT_ADD_SUCCESS = 2;
    public static final int RESULT_ADD_FAIL = 3;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        AddGroupMemberPageFragment fragment = AddGroupMemberPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
