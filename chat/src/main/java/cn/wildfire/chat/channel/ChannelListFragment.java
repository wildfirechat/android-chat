package cn.wildfire.chat.channel;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import cn.wildfirechat.chat.R;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.conversation.ConversationActivity;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;


public class ChannelListFragment extends Fragment implements ChannelListAdapter.OnChannelClickListener {
    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;
    private ChannelViewModel channelViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.channel_list_frament, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        channelViewModel = ViewModelProviders.of(getActivity()).get(ChannelViewModel.class);
        List<ChannelInfo> myChannels = channelViewModel.getMyChannels();
        List<ChannelInfo> followedChannels = channelViewModel.getListenedChannels();

        ChannelListAdapter adapter = new ChannelListAdapter();
        adapter.setOnChannelClickListener(this);
        adapter.setCreatedChannels(myChannels);
        adapter.setFollowedChannels(followedChannels);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onChannelClick(ChannelInfo channelInfo) {
        Intent intent = ConversationActivity.buildConversationIntent(getActivity(), Conversation.ConversationType.Channel, channelInfo.channelId, 0);
        startActivity(intent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }
}
