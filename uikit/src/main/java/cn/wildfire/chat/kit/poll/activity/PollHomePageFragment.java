/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.Conversation;

/**
 * 「投票」整页：发起投票 / 我的投票两个入口。
 * <p>
 * 逐行搬自 {@link PollHomeActivity}，那个类现在只是手机端的壳。入口是会话加号面板里的
 * 「投票」，会话本身就在右栏，不迁的话点开要整屏跳出去再跳回来。
 */
public class PollHomePageFragment extends Fragment implements WfcPage {

    private String groupId;
    private RecyclerView recyclerView;
    private PollHomeAdapter adapter;

    public static PollHomePageFragment fromIntent(Intent intent) {
        PollHomePageFragment fragment = new PollHomePageFragment();
        Bundle args = new Bundle();
        args.putString(PollHomeActivity.EXTRA_GROUP_ID,
            intent == null ? null : intent.getStringExtra(PollHomeActivity.EXTRA_GROUP_ID));
        fragment.setArguments(args);
        return fragment;
    }

    private String groupId() {
        if (groupId == null && getArguments() != null) {
            groupId = getArguments().getString(PollHomeActivity.EXTRA_GROUP_ID);
        }
        return groupId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_poll_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<PollHomeItem> items = new ArrayList<>();
        items.add(new PollHomeItem(getString(R.string.create_poll), R.drawable.ic_poll_create, () -> {
            // 发起投票
            Intent intent = new Intent(getContext(), CreatePollActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId());
            intent.putExtra(CreatePollActivity.EXTRA_CONVERSATION, conversation);
            WfcPageCompat.startPage(this, intent);
        }));
        items.add(new PollHomeItem(getString(R.string.my_polls), R.drawable.ic_poll_list, () -> {
            // 我的投票
            Intent intent = new Intent(getContext(), PollListActivity.class);
            intent.putExtra(PollListActivity.EXTRA_GROUP_ID, groupId());
            WfcPageCompat.startPage(this, intent);
        }));

        adapter = new PollHomeAdapter(items);
        recyclerView.setAdapter(adapter);
    }

    /**
     * 投票首页菜单项
     */
    private static class PollHomeItem {
        String title;
        int iconResId;
        Runnable action;

        PollHomeItem(String title, int iconResId, Runnable action) {
            this.title = title;
            this.iconResId = iconResId;
            this.action = action;
        }
    }

    /**
     * 适配器
     */
    private class PollHomeAdapter extends RecyclerView.Adapter<PollHomeAdapter.ViewHolder> {
        private List<PollHomeItem> items;

        PollHomeAdapter(List<PollHomeItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll_home, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            PollHomeItem item = items.get(position);
            holder.iconImageView.setImageResource(item.iconResId);
            holder.titleTextView.setText(item.title);
            holder.itemView.setOnClickListener(v -> {
                if (item.action != null) {
                    item.action.run();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconImageView;
            TextView titleTextView;

            ViewHolder(View itemView) {
                super(itemView);
                iconImageView = itemView.findViewById(R.id.iconImageView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
            }
        }
    }
}
