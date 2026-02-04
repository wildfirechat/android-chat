/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import cn.wildfirechat.chat.R;
import cn.wildfirechat.backup.BackupMetadata;
import cn.wildfirechat.backup.BackupManager;

/**
 * 备份与恢复主界面 Fragment
 */
public class BackupAndRestoreFragment extends Fragment {

    private RecyclerView backupListRecyclerView;
    private TextView createBackupButton;
    private TextView restoreBackupButton;
    private BackupListAdapter adapter;
    private List<BackupMetadata> backupList;

    public static BackupAndRestoreFragment newInstance() {
        return new BackupAndRestoreFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_backup_and_restore, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        bindEvents();
        loadBackupList();
    }

    private void initViews(View view) {
        backupListRecyclerView = view.findViewById(R.id.backupListRecyclerView);
        createBackupButton = view.findViewById(R.id.createBackupButton);
        restoreBackupButton = view.findViewById(R.id.restoreBackupButton);

        backupListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BackupListAdapter();
        backupListRecyclerView.setAdapter(adapter);
    }

    private void bindEvents() {
        createBackupButton.setOnClickListener(v -> onCreateBackupClick());

        restoreBackupButton.setOnClickListener(v -> onRestoreBackupClick());
    }

    /**
     * 子类可以重写此方法来处理创建备份的点击事件
     */
    protected void onCreateBackupClick() {
        Toast.makeText(requireContext(), R.string.backup_feature_not_implemented, Toast.LENGTH_SHORT).show();
    }

    /**
     * 子类可以重写此方法来处理恢复备份的点击事件
     */
    protected void onRestoreBackupClick() {
        // 默认实现：显示Toast提示
        Toast.makeText(requireContext(), R.string.restore_feature_not_implemented, Toast.LENGTH_SHORT).show();
    }

    private void loadBackupList() {
        // 获取备份目录
        File backupDir = getBackupDirectory();
        if (backupDir.exists()) {
            backupList = BackupManager.getInstance().getBackupList(backupDir.getAbsolutePath());
            adapter.setBackupList(backupList);
        }
    }

    private File getBackupDirectory() {
        // 使用外部存储目录
        File dir = new File(Environment.getExternalStorageDirectory(), "WildFireChatBackup");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBackupList();
    }

    // Adapter for backup list
    private static class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
        private List<BackupMetadata> backupList;

        public void setBackupList(List<BackupMetadata> backupList) {
            this.backupList = backupList;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_backup_list, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (backupList != null && position < backupList.size()) {
                BackupMetadata metadata = backupList.get(position);
                holder.bind(metadata);
            }
        }

        @Override
        public int getItemCount() {
            return backupList != null ? backupList.size() : 0;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            private TextView backupTimeTextView;
            private TextView backupInfoTextView;

            ViewHolder(View itemView) {
                super(itemView);
                backupTimeTextView = itemView.findViewById(R.id.backupTimeTextView);
                backupInfoTextView = itemView.findViewById(R.id.backupInfoTextView);
            }

            void bind(BackupMetadata metadata) {
                backupTimeTextView.setText(metadata.getBackupTime());
                if (metadata.getStatistics() != null) {
                    String info = itemView.getContext().getString(R.string.conversations_unit, metadata.getStatistics().totalConversations) + ", " +
                                  itemView.getContext().getString(R.string.messages_unit, metadata.getStatistics().totalMessages);
                    backupInfoTextView.setText(info);
                }
            }
        }
    }
}
