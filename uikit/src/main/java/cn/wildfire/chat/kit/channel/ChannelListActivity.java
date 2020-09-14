/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ChannelListActivity extends WfcBaseActivity {
    private boolean pick;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        pick = getIntent().getBooleanExtra("pick", false);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int menu() {
        return R.menu.channel_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createChannel();
            return true;
        } else if (item.getItemId() == R.id.subscribe) {
            subscribe();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    void createChannel() {
        Intent intent = new Intent(this, CreateChannelActivity.class);
        startActivity(intent);
        finish();
    }

    void subscribe() {
        Intent intent = new Intent(this, SearchChannelActivity.class);
        startActivity(intent);
    }
}
