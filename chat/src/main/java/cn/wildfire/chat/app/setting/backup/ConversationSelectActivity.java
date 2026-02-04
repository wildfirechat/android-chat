/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 会话选择界面 - 用于选择要备份的会话
 */
public class ConversationSelectActivity extends WfcBaseActivity {

    private RecyclerView recyclerView;
    private TextView selectAllButton;
    private TextView confirmButton;
    private ConversationAdapter adapter;
    private List<ConversationInfo> allConversations = new ArrayList<>();
    private List<ConversationInfo> selectedConversations = new ArrayList<>();

    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;

    public static void start(Context context) {
        Intent intent = new Intent(context, ConversationSelectActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_conversation_select;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initData();
    }

    private void initView() {
        recyclerView = findViewById(R.id.recyclerView);
        selectAllButton = findViewById(R.id.selectAllButton);
        confirmButton = findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter();
        recyclerView.setAdapter(adapter);

        // 初始化 ViewModels
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        selectAllButton.setOnClickListener(v -> selectAll());
        confirmButton.setOnClickListener(v -> confirmSelection());
    }

    private void initData() {
        // 加载所有会话
        List<Conversation.ConversationType> types = Arrays.asList(
                Conversation.ConversationType.Single,
                Conversation.ConversationType.Group,
                Conversation.ConversationType.Channel
        );
        allConversations = ChatManagerHolder.gChatManager.getConversationList(types, Arrays.asList(0));
        adapter.setData(allConversations);
    }

    private void selectAll() {
        if (selectedConversations.size() == allConversations.size()) {
            // 全部取消
            selectedConversations.clear();
        } else {
            // 全选
            selectedConversations = new ArrayList<>(allConversations);
        }
        adapter.notifyDataSetChanged();
        updateButtonText();
    }

    private void updateButtonText() {
        if (selectedConversations.size() == allConversations.size()) {
            selectAllButton.setText(R.string.deselect_all);
        } else {
            selectAllButton.setText(R.string.select_all);
        }
    }

    private void confirmSelection() {
        if (selectedConversations.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one_conversation, Toast.LENGTH_SHORT).show();
            return;
        }

        // 直接跳转到备份选项界面
        BackupOptionsActivity.start(this, new ArrayList<>(selectedConversations));
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private Fragment fragment = new Fragment() {};
        private List<ConversationInfo> conversations = new ArrayList<>();
        private final CenterCrop centerCrop = new CenterCrop();
        private final RoundedCorners roundedCorners = new RoundedCorners(6); // 6 px圆角

        void setData(List<ConversationInfo> data) {
            this.conversations = data;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_conversation_select, parent, false);
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

        void toggleSelection(int position) {
            ConversationInfo info = conversations.get(position);
            if (selectedConversations.contains(info)) {
                selectedConversations.remove(info);
            } else {
                selectedConversations.add(info);
            }
            notifyItemChanged(position);
            updateButtonText();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView portraitImageView;
            private TextView titleTextView;
            private TextView messageCountTextView;
            private CheckBox checkBox;

            ViewHolder(View itemView) {
                super(itemView);
                portraitImageView = itemView.findViewById(R.id.portraitImageView);
                titleTextView = itemView.findViewById(R.id.titleTextView);
                messageCountTextView = itemView.findViewById(R.id.messageCountTextView);
                checkBox = itemView.findViewById(R.id.checkBox);
                itemView.setOnClickListener(v -> toggleSelection(getAdapterPosition()));
                checkBox.setOnClickListener(v -> toggleSelection(getAdapterPosition()));
            }

            void bind(ConversationInfo info) {
                Conversation conversation = info.conversation;
                checkBox.setChecked(selectedConversations.contains(info));

                messageCountTextView.setText(itemView.getContext().getString(R.string.messages_count_suffix, ChatManager.Instance().getMessageCount(conversation)));

                // 根据会话类型显示不同的头像和名称
                if (conversation.type == Conversation.ConversationType.Single) {
                    // 单聊 - 显示用户信息
                    LiveData<UserInfo> userInfoLiveData = userViewModel.getUserInfoAsync(conversation.target, false);
                    userInfoLiveData.observe(ConversationSelectActivity.this, new Observer<UserInfo>() {
                        @Override
                        public void onChanged(UserInfo userInfo) {
                            if (userInfo != null) {
                                CharSequence name = userViewModel.getUserDisplayNameEx(userInfo);
                                titleTextView.setText(name);

                                Glide.with(ConversationSelectActivity.this)
                                        .load(userInfo.portrait)
                                        .placeholder(R.mipmap.avatar_def)
                                        .transform(centerCrop, roundedCorners)
                                        .into(portraitImageView);
                            }
                        }
                    });
                } else if (conversation.type == Conversation.ConversationType.Group) {
                    // 群聊 - 显示群组信息
                    LiveData<Pair<GroupInfo, Integer>> groupLiveData = groupViewModel.getConversationGroupInfoAsync(conversation.target, false);
                    groupLiveData.observe(ConversationSelectActivity.this, new Observer<Pair<GroupInfo, Integer>>() {
                        @Override
                        public void onChanged(Pair<GroupInfo, Integer> pair) {
                            if (pair != null && pair.first != null) {
                                GroupInfo groupInfo = pair.first;
                                String name = ChatManagerHolder.gChatManager.getGroupDisplayName(groupInfo);
                                titleTextView.setText(name);

                                Glide.with(ConversationSelectActivity.this)
                                        .load(groupInfo.portrait)
                                        .placeholder(R.mipmap.avatar_def)
                                        .transform(centerCrop, roundedCorners)
                                        .into(portraitImageView);
                            }
                        }
                    });
                } else if (conversation.type == Conversation.ConversationType.Channel) {
                    // 频道 - 使用目标名称
                    titleTextView.setText(conversation.target);
                    portraitImageView.setImageResource(R.mipmap.avatar_def);
                } else {
                    // 其他类型
                    titleTextView.setText(conversation.target);
                    portraitImageView.setImageResource(R.mipmap.avatar_def);
                }
            }
        }
    }
}
