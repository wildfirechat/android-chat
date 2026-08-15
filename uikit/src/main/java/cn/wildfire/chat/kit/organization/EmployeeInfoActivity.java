/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserInfoFragment;
import cn.wildfirechat.model.UserInfo;

/**
 * 组织架构里的成员详情，与用户资料是同一个页面，只是入口不同。
 * <p>
 * 改造前本类持有一份与 {@code UserInfoActivity} <strong>逐行重复</strong>的
 * {@code R.menu.user_info} 处理代码（约 150 行）。现在那份逻辑住在
 * {@link UserInfoFragment} 里，两个入口和平板右栏共用同一份。
 */
public class EmployeeInfoActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        UserInfo userInfo = getIntent().getParcelableExtra("userInfo");
        String groupId = getIntent().getStringExtra("groupId");
        if (userInfo == null) {
            finish();
        } else {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFrameLayout, UserInfoFragment.newInstance(userInfo, groupId))
                .commit();
        }
    }
}
