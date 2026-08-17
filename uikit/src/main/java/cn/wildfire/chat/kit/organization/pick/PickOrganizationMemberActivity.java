/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.pick;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 在组织架构里选人页的空壳。
 * <p>
 * 页面本体是 {@link PickOrganizationMemberPageFragment}：手机端由本壳装着，平板上同一份实现
 * 直接进右栏，标题栏、「选择」菜单、返回都由宿主提供。这里只保留调用方要用的参数名。
 */
public class PickOrganizationMemberActivity extends WfcBaseActivity {
    public static final String PARAM_ORGANIZATION_ID = "organizationId";
    public static final String PARAM_MAX_PICK_COUNT = "maxPickCount";
    public static final String PARAM_INITIAL_CHECKED_EMPLOYEES = "initialCheckedEmployees";
    public static final String PARAM_INITIAL_CHECKED_ORANIZATIONS = "initialCheckedOrganizations";
    public static final String PARAM_UNCHECKABLE_IDS = "uncheckableIds";

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 add 会再叠一层
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, PickOrganizationMemberPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
