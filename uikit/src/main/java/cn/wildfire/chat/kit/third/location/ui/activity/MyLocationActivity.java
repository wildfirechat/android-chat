/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.third.location.ui.activity;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.third.location.ui.fragment.MyLocationPageFragment;

/**
 * 发送位置页的空壳。
 * <p>
 * 页面本体是 {@link MyLocationPageFragment}：手机端由本壳装着，平板上同一份实现直接进右栏
 * （压在会话上面），标题栏、「发送」菜单、返回都由宿主提供。
 * <p>
 * 结果按 {@code RESULT_OK} + {@code "location"} 回传给 {@code LocationExt}，
 * 平板上由右栏投递，手机端由系统投递，两端的 {@code onActivityResult} 参数一致。
 */
public class MyLocationActivity extends WfcBaseActivity {

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
            .replace(R.id.containerFrameLayout, MyLocationPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
