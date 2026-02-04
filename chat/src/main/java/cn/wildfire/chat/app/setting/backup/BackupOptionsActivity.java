/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 备份选项配置界面
 */
public class BackupOptionsActivity extends WfcBaseActivity {

    private CheckBox includeMediaCheckbox;
    private RecyclerView conversationRecyclerView;
    private ArrayList<ConversationInfo> selectedConversations;

    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private ConversationAdapter adapter;

    public static void start(Context context, ArrayList<ConversationInfo> conversations) {
        Intent intent = new Intent(context, BackupOptionsActivity.class);
        intent.putParcelableArrayListExtra("conversations", conversations);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_options;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedConversations = getIntent().getParcelableArrayListExtra("conversations");
        if (selectedConversations == null || selectedConversations.isEmpty()) {
            Toast.makeText(this, R.string.no_conversations_selected, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        initData();
    }

    private void initView() {
        includeMediaCheckbox = findViewById(R.id.includeMediaCheckbox);
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);

        // 初始化 RecyclerView
        conversationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter();
        conversationRecyclerView.setAdapter(adapter);

        // 初始化 ViewModels
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
    }

    @Override
    protected int menu() {
        return R.menu.menu_backup_next;
    }

    @Override
    protected void afterMenus(android.view.Menu menu) {
        super.afterMenus(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_next) {
            goToNextStep();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initData() {
        adapter.setData(selectedConversations);
    }

    private void goToNextStep() {
        boolean includeMedia = includeMediaCheckbox.isChecked();
        BackupDestinationActivity.start(this, selectedConversations, includeMedia);
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private List<ConversationInfo> conversations = new ArrayList<>();

        void setData(List<ConversationInfo> data) {
            this.conversations = data;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_backup_conversation, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ConversationInfo info = conversations.get(position);
            holder.bind(info);
        }

        @Override
        public int getItemCount() {
            return conversations.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView titleTextView;
            private TextView messageCountTextView;

            ViewHolder(View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.conversationTitleTextView);
                messageCountTextView = itemView.findViewById(R.id.messageCountTextView);
            }

            void bind(ConversationInfo info) {
                Conversation conversation = info.conversation;

                // 显示默认消息数
                messageCountTextView.setText(R.string.loading);

                messageCountTextView.setText(itemView.getContext().getString(R.string.messages_format, ChatManager.Instance().getMessageCount(conversation)));

                // 根据会话类型显示不同的名称
                if (conversation.type == Conversation.ConversationType.Single) {
                    // 单聊 - 显示用户信息
                    LiveData<UserInfo> userInfoLiveData = userViewModel.getUserInfoAsync(conversation.target, false);
                    userInfoLiveData.observe(BackupOptionsActivity.this, new Observer<UserInfo>() {
                        @Override
                        public void onChanged(UserInfo userInfo) {
                            if (userInfo != null) {
                                CharSequence name = userViewModel.getUserDisplayNameEx(userInfo);
                                titleTextView.setText(name);
                            }
                        }
                    });
                } else if (conversation.type == Conversation.ConversationType.Group) {
                    // 群聊 - 显示群组信息
                    LiveData<Pair<GroupInfo, Integer>> groupLiveData = groupViewModel.getConversationGroupInfoAsync(conversation.target, false);
                    groupLiveData.observe(BackupOptionsActivity.this, new Observer<Pair<GroupInfo, Integer>>() {
                        @Override
                        public void onChanged(Pair<GroupInfo, Integer> pair) {
                            if (pair != null && pair.first != null) {
                                GroupInfo groupInfo = pair.first;
                                String name = ChatManagerHolder.gChatManager.getGroupDisplayName(groupInfo);
                                titleTextView.setText(name);
                            }
                        }
                    });
                } else if (conversation.type == Conversation.ConversationType.Channel) {
                    // 频道 - 使用目标名称
                    titleTextView.setText(conversation.target);
                } else {
                    // 其他类型
                    titleTextView.setText(conversation.target);
                }
            }
        }
    }
}
