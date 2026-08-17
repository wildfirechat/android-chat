/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「从群成员里选人」在手机端的宿主壳（发起群语音/视频、多人通话加人）。
 * <p>
 * 整页逻辑（列表 + 确认菜单 + 回传结果）住在 {@link PickGroupMemberPageFragment}，
 * 手机端与平板右栏共用同一份。本类<strong>不再继承</strong> {@link BasePickGroupMemberActivity}
 * —— 那个基类把 PickUserViewModel 的装配和确认菜单放在 Activity 上，页面就没法脱离 Activity 存在，
 * 也就永远进不了右栏。另外三个兄弟页（移出成员、禁言、加管理员）仍走老基类，可按同一模板迁移。
 */
public class PickGroupMemberActivity extends WfcBaseActivity {

    /**
     * 结果里携带的成员 id 列表。键名与改造前一致，调用方（会话页、多人通话页）无需改动。
     */
    public static final String EXTRA_RESULT = "pickedMemberIds";

    public static final String GROUP_INFO = BasePickGroupMemberActivity.GROUP_INFO;
    public static final String UNCHECKABLE_MEMBER_IDS = BasePickGroupMemberActivity.UNCHECKABLE_MEMBER_IDS;
    public static final String CHECKED_MEMBER_IDS = BasePickGroupMemberActivity.CHECKED_MEMBER_IDS;
    public static final String MAX_COUNT = BasePickGroupMemberActivity.MAX_COUNT;

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        PickGroupMemberPageFragment fragment = PickGroupMemberPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
