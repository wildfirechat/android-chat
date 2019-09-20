package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.wildfire.chat.kit.channel.ChannelInfoActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.viewHolder.ChannelViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.SearchChannelCallback;

public class ChannelSearchModule extends SearchableModule<ChannelInfo, ChannelViewHolder> {
    @Override
    public ChannelViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int type) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.channel_item, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBind(Fragment fragment, ChannelViewHolder holder, ChannelInfo channelInfo) {
        holder.bind(channelInfo);
    }

    @Override
    public void onClick(Fragment fragment, ChannelViewHolder holder, View view, ChannelInfo channelInfo) {
        // TODO show channel info
        Toast.makeText(fragment.getActivity(), "show channel info", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(fragment.getActivity(), ChannelInfoActivity.class);
        intent.putExtra("channelInfo", channelInfo);
        fragment.startActivity(intent);
    }

    @Override
    public int getViewType(ChannelInfo channelInfo) {
        return R.layout.channel_item;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public String category() {
        return "频道";
    }

    @Override
    public List<ChannelInfo> search(String keyword) {
        CountDownLatch latch = new CountDownLatch(1);
        List<ChannelInfo> result = new ArrayList<>();
        ChatManager.Instance().searchChannel(keyword, new SearchChannelCallback() {
            @Override
            public void onSuccess(List<ChannelInfo> channelInfos) {
                if (channelInfos != null) {
                    result.addAll(channelInfos);
                }
                latch.countDown();
            }

            @Override
            public void onFail(int errorCode) {
                Log.e(ChannelSearchModule.class.getSimpleName(), "search failure: " + errorCode);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }
}
