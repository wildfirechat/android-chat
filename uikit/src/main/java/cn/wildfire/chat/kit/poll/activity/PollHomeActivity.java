/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.model.Conversation;

/**
 * 投票首页
 * <p>
 * 包含发起投票和我的投票两个入口。
 * </p>
 */
public class PollHomeActivity extends WfcBaseActivity {
    private static final String EXTRA_GROUP_ID = "groupId";
    
    private String groupId;
    private RecyclerView recyclerView;
    private PollHomeAdapter adapter;
    
    public static Intent buildIntent(Context context, String groupId) {
        Intent intent = new Intent(context, PollHomeActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_poll_home;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        groupId = getIntent().getStringExtra(EXTRA_GROUP_ID);
        
        initView();
    }
    
    private void initView() {
        setTitle(getString(R.string.poll));
        
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        List<PollHomeItem> items = new ArrayList<>();
        items.add(new PollHomeItem(getString(R.string.create_poll), R.drawable.ic_poll_create, () -> {
            // 发起投票
            Intent intent = new Intent(this, CreatePollActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId);
            intent.putExtra(CreatePollActivity.EXTRA_CONVERSATION, conversation);
            startActivity(intent);
        }));
        items.add(new PollHomeItem(getString(R.string.my_polls), R.drawable.ic_poll_list, () -> {
            // 我的投票
            Intent intent = new Intent(this, PollListActivity.class);
            intent.putExtra(PollListActivity.EXTRA_GROUP_ID, groupId);
            startActivity(intent);
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
