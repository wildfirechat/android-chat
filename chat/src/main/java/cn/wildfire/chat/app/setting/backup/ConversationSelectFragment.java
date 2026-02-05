package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetConversationListCallback;

/**
 * 会话选择界面 - 用于选择要备份的会话
 */
public class ConversationSelectFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView selectAllButton;
    private TextView confirmButton;
    private ConversationAdapter adapter;
    private List<ConversationInfo> allConversations = new ArrayList<>();
    private List<ConversationInfo> selectedConversations = new ArrayList<>();

    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private PickBackupConversationViewModel pickBackupConversationViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_conversation_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);
        initData();
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        selectAllButton = view.findViewById(R.id.selectAllButton);
        confirmButton = view.findViewById(R.id.confirmButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter();
        recyclerView.setAdapter(adapter);

        // 初始化 ViewModels
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        pickBackupConversationViewModel = new ViewModelProvider(requireActivity()).get(PickBackupConversationViewModel.class);

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
        ChatManagerHolder.gChatManager.getConversationListAsync(types, Arrays.asList(0), new GetConversationListCallback() {
            @Override
            public void onSuccess(List<ConversationInfo> conversationInfos) {
                allConversations = conversationInfos;
                adapter.setData(conversationInfos);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(int errorCode) {

            }
        });
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
            Toast.makeText(requireContext(), R.string.select_at_least_one_conversation, Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存选中数据到 ViewModel
        pickBackupConversationViewModel.setSelectedConversations(new ArrayList<>(selectedConversations));

        // 跳转到备份选项界面
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new BackupOptionsFragment())
                .addToBackStack(null)
                .commit();
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private List<ConversationInfo> conversations = new ArrayList<>();
        private final CenterCrop centerCrop = new CenterCrop();
        private final RoundedCorners roundedCorners = new RoundedCorners(6); // 6 px圆角

        void setData(List<ConversationInfo> data) {
            this.conversations = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation_select, parent, false);
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
                    userInfoLiveData.observe(getViewLifecycleOwner(), new Observer<UserInfo>() {
                        @Override
                        public void onChanged(UserInfo userInfo) {
                            if (userInfo != null) {
                                CharSequence name = userViewModel.getUserDisplayNameEx(userInfo);
                                titleTextView.setText(name);

                                Glide.with(ConversationSelectFragment.this)
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
                    groupLiveData.observe(getViewLifecycleOwner(), new Observer<Pair<GroupInfo, Integer>>() {
                        @Override
                        public void onChanged(Pair<GroupInfo, Integer> pair) {
                            if (pair != null && pair.first != null) {
                                GroupInfo groupInfo = pair.first;
                                String name = ChatManagerHolder.gChatManager.getGroupDisplayName(groupInfo);
                                titleTextView.setText(name);

                                Glide.with(ConversationSelectFragment.this)
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
