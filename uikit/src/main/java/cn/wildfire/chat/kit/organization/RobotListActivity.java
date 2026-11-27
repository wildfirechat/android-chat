/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization;

import android.os.Bundle;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;

/**
 * 组织架构机器人列表在手机端的宿主壳。页面本体是 {@link RobotListFragment}，
 * 手机端由本壳装着，平板上同一份实现直接进右栏。
 * <p>
 * 作为选人页使用时由调用方通过 {@link WfcPageCompat#startPageForResult} 打开：
 * 单选点击即回传，多选由「完成」菜单回传 {@link #RESULT_ROBOT_IDS}。
 */
public class RobotListActivity extends WfcBaseActivity {
    public static final String PARAM_PICK = "pick";
    public static final String PARAM_MULTI_SELECT = "multiSelect";
    public static final String PARAM_MAX_COUNT = "maxCount";
    public static final String PARAM_INITIAL_CHECKED_IDS = "initialCheckedIds";
    public static final String PARAM_UNCHECKABLE_IDS = "uncheckableIds";

    /** 选中的机器人 userId 列表，ArrayList&lt;String&gt; */
    public static final String RESULT_ROBOT_IDS = "robotIds";
    /** 选中的机器人，ArrayList&lt;Robot&gt;，用于展示名称/头像 */
    public static final String RESULT_ROBOTS = "robots";
    /** 单选时回传的 UserInfo，键名与联系人选人保持一致 */
    public static final String RESULT_USER_INFO = "userInfo";

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
            .replace(R.id.containerFrameLayout, RobotListFragment.fromIntent(getIntent()))
            .commit();
    }
}
