/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 选联系人（可多选）页的空壳。
 * <p>
 * 页面本体是 {@link PickContactPageFragment}：手机端由本壳装着，平板上同一份实现直接进右栏，
 * 标题栏、「完成」菜单、返回都由宿主提供。这里只保留调用方要用的参数名和 intent 构造器。
 */
public class PickContactActivity extends WfcBaseActivity {
    public static final String PARAM_MAX_COUNT = "maxCount";
    public static final String PARAM_INITIAL_CHECKED_IDS = "initialCheckedIds";
    public static final String PARA_UNCHECKABLE_IDS = "uncheckableIds";
    public static final String RESULT_PICKED_USERS = "pickedUsers";

    public static Intent buildPickIntent(Context context, int maxCount, ArrayList<String> initialChecedIds, ArrayList<String> uncheckableIds) {
        Intent intent = new Intent(context, PickContactActivity.class);
        intent.putExtra(PARAM_MAX_COUNT, maxCount);
        intent.putExtra(PARAM_INITIAL_CHECKED_IDS, initialChecedIds);
        intent.putExtra(PARA_UNCHECKABLE_IDS, uncheckableIds);
        return intent;
    }

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
            .replace(R.id.containerFrameLayout, PickContactPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
