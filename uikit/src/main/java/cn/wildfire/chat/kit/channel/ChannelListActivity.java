/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.os.Bundle;

import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 频道列表在手机端的宿主壳。菜单（订阅频道）住在 {@link ChannelListFragment}，两端共用。
 */
public class ChannelListActivity extends WfcBaseActivity {
    private boolean pick;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        pick = getIntent().getBooleanExtra("pick", false);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("pick", pick);
        ChannelListFragment fragment = new ChannelListFragment();
        fragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();
    }
}
