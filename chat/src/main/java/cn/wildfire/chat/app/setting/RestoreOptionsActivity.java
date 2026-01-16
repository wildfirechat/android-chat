/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.File;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.backup.BackupMetadata;
import cn.wildfirechat.chat.R;

/**
 * 恢复选项界面
 */
public class RestoreOptionsActivity extends WfcBaseActivity {

    private static final String EXTRA_BACKUP_DIR = "backup_dir";
    private static final String EXTRA_BACKUP_TIME = "backup_time";
    private static final String EXTRA_DEVICE_NAME = "device_name";
    private static final String EXTRA_CONVERSATION_COUNT = "conversation_count";
    private static final String EXTRA_MESSAGE_COUNT = "message_count";
    private static final String EXTRA_MEDIA_COUNT = "media_count";

    private String backupDir;
    private String backupTime;
    private String deviceName;
    private int conversationCount;
    private int messageCount;
    private int mediaCount;

    private TextView backupTimeTextView;
    private TextView deviceNameTextView;
    private TextView conversationCountTextView;
    private TextView messageCountTextView;
    private TextView mediaCountTextView;
    private CheckBox includeMediaCheckBox;

    public static void start(Context context, BackupMetadata metadata) {
        Intent intent = new Intent(context, RestoreOptionsActivity.class);
        intent.putExtra(EXTRA_BACKUP_DIR, metadata.getBackupDir());
        intent.putExtra(EXTRA_BACKUP_TIME, metadata.getBackupTime());
        intent.putExtra(EXTRA_DEVICE_NAME, metadata.getDeviceName());
        if (metadata.getStatistics() != null) {
            intent.putExtra(EXTRA_CONVERSATION_COUNT, metadata.getStatistics().totalConversations);
            intent.putExtra(EXTRA_MESSAGE_COUNT, metadata.getStatistics().totalMessages);
            intent.putExtra(EXTRA_MEDIA_COUNT, metadata.getStatistics().mediaFileCount);
        }
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_restore_options;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        extractExtras();
        initViews();
    }

    @Override
    protected int menu() {
        return R.menu.menu_restore_confirm;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onRestoreConfirm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void extractExtras() {
        backupDir = getIntent().getStringExtra(EXTRA_BACKUP_DIR);
        backupTime = getIntent().getStringExtra(EXTRA_BACKUP_TIME);
        deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        conversationCount = getIntent().getIntExtra(EXTRA_CONVERSATION_COUNT, 0);
        messageCount = getIntent().getIntExtra(EXTRA_MESSAGE_COUNT, 0);
        mediaCount = getIntent().getIntExtra(EXTRA_MEDIA_COUNT, 0);
    }

    private void initViews() {
        backupTimeTextView = findViewById(R.id.backupTimeTextView);
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        conversationCountTextView = findViewById(R.id.conversationCountTextView);
        messageCountTextView = findViewById(R.id.messageCountTextView);
        mediaCountTextView = findViewById(R.id.mediaCountTextView);
        includeMediaCheckBox = findViewById(R.id.includeMediaCheckBox);

        backupTimeTextView.setText(getString(R.string.backup_time_label, backupTime));
        String displayName = deviceName != null && !deviceName.isEmpty() ? deviceName : getString(R.string.unknown_device);
        deviceNameTextView.setText(getString(R.string.device_label, displayName));
        conversationCountTextView.setText(getString(R.string.conversations_label, conversationCount));
        messageCountTextView.setText(getString(R.string.messages_label, messageCount));
        mediaCountTextView.setText(getString(R.string.media_files_label, mediaCount));

        // Disable media checkbox if no media files
        if (mediaCount == 0) {
            includeMediaCheckBox.setEnabled(false);
            includeMediaCheckBox.setChecked(false);
        }
    }

    private void onRestoreConfirm() {
        boolean includeMedia = includeMediaCheckBox.isChecked();
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File backupDirectory = new File(documentsDir, "WildFireChatBackup/" + backupDir);

        // Use user ID as password (same as backup)
        String userId = cn.wildfire.chat.kit.ChatManagerHolder.gChatManager.getUserId();
        BackupProgressActivity.startForRestore(this, backupDirectory.getAbsolutePath(), includeMedia, userId);
    }
}
