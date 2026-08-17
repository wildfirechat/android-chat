/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;


public class ChannelListFragment extends Fragment implements ChannelListAdapter.OnChannelClickListener, WfcPage {
    RecyclerView recyclerView;
    private ChannelViewModel channelViewModel;
    private ChannelListAdapter channelListAdapter;
    private boolean pick;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            pick = args.getBoolean("pick", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.channel_list_frament, container, false);
        bindViews(view);
        init();
        return view;
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChannel();
    }

    private void init() {
        channelViewModel =new ViewModelProvider(getActivity()).get(ChannelViewModel.class);

        channelListAdapter = new ChannelListAdapter();
        channelListAdapter.setOnChannelClickListener(this);

        recyclerView.setAdapter(channelListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        channelViewModel.channelInfoLiveData().observe(this, channelInfos -> {
            if (channelInfos != null) {
                refreshChannel();
            }
        });
    }

    private void refreshChannel() {
        List<ChannelInfo> followedChannels = channelViewModel.getListenedChannels();
        channelListAdapter.setFollowedChannels(followedChannels);
        channelListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChannelClick(ChannelInfo channelInfo) {
        if (pick) {
            Intent intent = new Intent();
            intent.putExtra("channelInfo", channelInfo);
            WfcPageCompat.setPageResult(this, Activity.RESULT_OK, intent);
            WfcPageCompat.finishPage(this);
        } else {
            Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Channel, channelInfo.channelId, 0);
            ConversationRouter.open(this, intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    // ==================== WfcPage：菜单 ====================
    // 改造前写在 ChannelListActivity 里，右栏需要时只能再表达一遍；现在两端共用这一份。
    // 选择器形态（pick）没有菜单，与 Activity 版本一致 —— 它那时是整个 Activity 只在非 pick 下被打开。

    @Override
    public int pageMenu() {
        return pick ? 0 : R.menu.channel_list;
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.subscribe) {
            WfcPageCompat.startPage(this, new Intent(getActivity(), SearchChannelActivity.class));
            return true;
        }
        return false;
    }
}
