/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupManager;
import cn.wildfirechat.backup.BackupMetadata;
import cn.wildfirechat.chat.R;

/**
 * 本地备份列表界面
 */
public class BackupListActivity extends WfcBaseActivity {

    private RecyclerView backupListRecyclerView;
    private TextView emptyTextView;
    private BackupListAdapter adapter;
    private List<BackupMetadata> backupList;
    private File backupDir;

    public static void start(Context context) {
        Intent intent = new Intent(context, BackupListActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_list;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initViews();
        loadBackupList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadBackupList();
    }

    private void initViews() {
        backupListRecyclerView = findViewById(R.id.backupListRecyclerView);
        emptyTextView = findViewById(R.id.emptyTextView);

        backupListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackupListAdapter();
        backupListRecyclerView.setAdapter(adapter);
    }

    private void loadBackupList() {
        backupDir = getBackupDirectory();
        if (backupDir != null && backupDir.exists()) {
            backupList = BackupManager.getInstance().getBackupList(backupDir.getAbsolutePath());

            if (backupList == null || backupList.isEmpty()) {
                emptyTextView.setVisibility(View.VISIBLE);
                backupListRecyclerView.setVisibility(View.GONE);
            } else {
                emptyTextView.setVisibility(View.GONE);
                backupListRecyclerView.setVisibility(View.VISIBLE);
                adapter.setBackupList(backupList);
            }
        } else {
            emptyTextView.setVisibility(View.VISIBLE);
            backupListRecyclerView.setVisibility(View.GONE);
        }
    }

    private File getBackupDirectory() {
        // 使用公共Documents目录（用户可以访问，应用卸载后保留）
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File dir = new File(documentsDir, "WildFireChatBackup");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    private void deleteBackup(BackupMetadata metadata, int position) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_backup)
                .setMessage(R.string.delete_backup_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    File backupDir = new File(getBackupDirectory(), metadata.getBackupDir());
                    if (deleteDirectory(backupDir)) {
                        Toast.makeText(this, R.string.backup_deleted, Toast.LENGTH_SHORT).show();
                        loadBackupList();
                    } else {
                        Toast.makeText(this, R.string.delete_backup_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean deleteDirectory(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return directory.delete();
        }
        return false;
    }

    private class BackupListAdapter extends RecyclerView.Adapter<BackupListAdapter.ViewHolder> {
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
                holder.bind(metadata, position);
            }
        }

        @Override
        public int getItemCount() {
            return backupList != null ? backupList.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView backupTimeTextView;
            private TextView deviceNameTextView;
            private TextView backupInfoTextView;
            private TextView restoreButton;
            private TextView deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                backupTimeTextView = itemView.findViewById(R.id.backupTimeTextView);
                deviceNameTextView = itemView.findViewById(R.id.deviceNameTextView);
                backupInfoTextView = itemView.findViewById(R.id.backupInfoTextView);
                restoreButton = itemView.findViewById(R.id.restoreButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }

            void bind(BackupMetadata metadata, int position) {
                backupTimeTextView.setText(metadata.getBackupTime());

                // 显示设备名称
                String deviceName = metadata.getDeviceName();
                if (deviceName != null && !deviceName.isEmpty()) {
                    deviceNameTextView.setText(getString(R.string.device) + ": " + deviceName);
                    deviceNameTextView.setVisibility(View.VISIBLE);
                } else {
                    deviceNameTextView.setVisibility(View.GONE);
                }

                if (metadata.getStatistics() != null) {
                    String info = getString(R.string.conversations_unit, metadata.getStatistics().totalConversations) + ", " +
                                  getString(R.string.messages_unit, metadata.getStatistics().totalMessages);
                    backupInfoTextView.setText(info);
                }

                restoreButton.setOnClickListener(v -> {
                    RestoreOptionsActivity.start(BackupListActivity.this, metadata);
                });

                deleteButton.setOnClickListener(v -> {
                    deleteBackup(metadata, position);
                });
            }
        }
    }
}
