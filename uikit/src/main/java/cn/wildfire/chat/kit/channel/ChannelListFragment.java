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
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;


public class ChannelListFragment extends Fragment implements ChannelListAdapter.OnChannelClickListener {
    @BindView(R2.id.recyclerView)
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
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshChannel();
    }

    private void init() {
        channelViewModel = ViewModelProviders.of(getActivity()).get(ChannelViewModel.class);

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
        List<ChannelInfo> myChannels = channelViewModel.getMyChannels();
        List<ChannelInfo> followedChannels = channelViewModel.getListenedChannels();
        channelListAdapter.setCreatedChannels(myChannels);
        channelListAdapter.setFollowedChannels(followedChannels);
        channelListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onChannelClick(ChannelInfo channelInfo) {
        if (pick) {
            Intent intent = new Intent();
            intent.putExtra("channelInfo", channelInfo);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        } else {
            Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Channel, channelInfo.channelId, 0);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}
