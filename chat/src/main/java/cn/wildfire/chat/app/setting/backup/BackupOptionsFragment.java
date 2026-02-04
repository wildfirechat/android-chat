package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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

import java.util.ArrayList;
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

/**
 * 备份选项配置界面 Fragment
 */
public class BackupOptionsFragment extends Fragment {

    private CheckBox includeMediaCheckbox;
    private RecyclerView conversationRecyclerView;
    private List<ConversationInfo> selectedConversations;

    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;
    private ConversationAdapter adapter;
    private PickBackupConversationViewModel pickBackupConversationViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.activity_backup_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pickBackupConversationViewModel = new ViewModelProvider(requireActivity()).get(PickBackupConversationViewModel.class);
        selectedConversations = pickBackupConversationViewModel.getSelectedConversations();

        if (selectedConversations == null || selectedConversations.isEmpty()) {
            Toast.makeText(requireContext(), R.string.no_conversations_selected, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }

        initView(view);
        initData();
    }

    private void initView(View view) {
        includeMediaCheckbox = view.findViewById(R.id.includeMediaCheckbox);
        conversationRecyclerView = view.findViewById(R.id.conversationRecyclerView);

        // 初始化 RecyclerView
        conversationRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter();
        conversationRecyclerView.setAdapter(adapter);

        // 初始化 ViewModels
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        // 设置 Toolbar 菜单 (Fragment 中可能需要 Activity 协助处理 Toolbar)
        // 这里假设 Activity 会处理 onCreateOptionsMenu 或者使用 Toolbar
        // 如果是标准的 Fragment 使用 menu，需要重写 onCreateOptionsMenu

        // 由于原 Activity 使用了 WfcBaseActivity 的 menu() 方法，这里我们可能需要手动设置 menu
        // 或者依赖 Activity 的 Toolbar。为了简单起见，我们可以在这里设置 Toolbar 的 menu item click listener
        // 但这取决于 Activity 的实现。
        // 假设 Host Activity 是 BackupAndRestoreActivity (继承自 WfcBaseActivity)
        // 但 WfcBaseActivity 可能不直接支持 Fragment 的 menu。
        // 我们尝试使用 setHasOptionsMenu(true) 和 onCreateOptionsMenu
    }

    // 如果 WfcBaseActivity 支持 Fragment 的 options menu
    @Override
    public void onCreateOptionsMenu(@NonNull android.view.Menu menu, @NonNull android.view.MenuInflater inflater) {
        inflater.inflate(R.menu.menu_backup_next, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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
        pickBackupConversationViewModel.setIncludeMedia(includeMedia);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new BackupDestinationFragment())
                .addToBackStack(null)
                .commit();
    }

    private class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
        private List<ConversationInfo> conversations = new ArrayList<>();

        void setData(List<ConversationInfo> data) {
            this.conversations = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_backup_conversation, parent, false);
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
                    userInfoLiveData.observe(getViewLifecycleOwner(), new Observer<UserInfo>() {
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
                    groupLiveData.observe(getViewLifecycleOwner(), new Observer<Pair<GroupInfo, Integer>>() {
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
