/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.third.location.ui.activity;

import android.content.Context;
import android.content.Intent;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.third.location.ui.fragment.ShowLocationPageFragment;

/**
 * 查看位置消息页的空壳。
 * <p>
 * 页面本体是 {@link ShowLocationPageFragment}：手机端由本壳装着，平板上同一份实现直接进右栏
 * （压在会话上面），标题栏、返回都由宿主提供。
 */
public class ShowLocationActivity extends WfcBaseActivity {

    public static Intent buildShowLocationIntent(Context context, double lat, double lng, String title) {
        Intent intent = new Intent(context, ShowLocationActivity.class);
        intent.putExtra("Lat", lat);
        intent.putExtra("Long", lng);
        intent.putExtra("title", title);
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
            .replace(R.id.containerFrameLayout, ShowLocationPageFragment.fromIntent(getIntent()))
            .commit();
    }
}
