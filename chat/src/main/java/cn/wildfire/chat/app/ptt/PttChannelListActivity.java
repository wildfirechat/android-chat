/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.view.MenuItem;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.ptt.ChannelInfo;
import cn.wildfirechat.ptt.PTTClient;

public class PttChannelListActivity extends WfcBaseActivity implements PttChannelListAdapter.OnChannelClickListener {

    @BindView(R.id.recycler_view)
    RecyclerView recyclerView;
    private PttChannelListAdapter adapter;

    @Override
    protected int contentLayout() {
        return R.layout.ptt_list_activity;
    }

    @Override
    protected int menu() {
        return R.menu.ptt_list;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            Intent intent = new Intent(this, CreatePttChannelActivity.class);
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        this.adapter = new PttChannelListAdapter(this);
        recyclerView.setAdapter(adapter);
        adapter.setOnChannelClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        loadChannels();
    }

    private void loadChannels() {
        List<String> channelIds = PTTClient.getInstance().getSubscribedChannels();
        List<ChannelInfo> channelInfos = new ArrayList<>();
        for (String channelId : channelIds) {
            channelInfos.add(PTTClient.getInstance().getChannelInfo(channelId));
        }
        adapter.setChannelInfos(channelInfos);
    }

    @Override
    public void onChannelClick(ChannelInfo channelInfo) {
        Intent intent = new Intent(this, PttActivity.class);
        intent.putExtra("channelId", channelInfo.channelId);
        startActivity(intent);
    }
}
