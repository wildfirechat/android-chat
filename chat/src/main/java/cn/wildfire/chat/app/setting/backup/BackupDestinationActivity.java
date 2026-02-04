/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting.backup;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 备份目标选择界面
 */
public class BackupDestinationActivity extends WfcBaseActivity {

    private ArrayList<ConversationInfo> conversations;
    private boolean includeMedia;

    private CardView backupToPCCard;
    private TextView backupToPCTitleTextView;
    private TextView backupToPCDescTextView;

    private SettingViewModel settingViewModel;

    public static void start(Context context, ArrayList<ConversationInfo> conversations, boolean includeMedia) {
        Intent intent = new Intent(context, BackupDestinationActivity.class);
        intent.putParcelableArrayListExtra("conversations", conversations);
        intent.putExtra("includeMedia", includeMedia);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_destination;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversations = getIntent().getParcelableArrayListExtra("conversations");
        includeMedia = getIntent().getBooleanExtra("includeMedia", true);

        initView();
        observeSettingChanges();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePCOnlineStatus();
    }

    private void initView() {
        findViewById(R.id.backupToLocalCard).setOnClickListener(v -> backupToLocal());

        backupToPCCard = findViewById(R.id.backupToPCCard);
        backupToPCTitleTextView = backupToPCCard.findViewById(R.id.titleTextView);
        backupToPCDescTextView = backupToPCCard.findViewById(R.id.descTextView);

        // 初始状态，稍后在 onResume 中更新
        backupToPCCard.setOnClickListener(v -> {
            if (isPCOnline()) {
                backupToPC();
            } else {
                Toast.makeText(this, R.string.pc_is_not_online, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeSettingChanges() {
        // 获取 SettingViewModel
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);

        // 监听设置变更
        settingViewModel.settingUpdatedLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                // 当设置变更时，重新检查PC在线状态
                updatePCOnlineStatus();
            }
        });
    }

    private void updatePCOnlineStatus() {
        boolean isOnline = isPCOnline();

        if (isOnline) {
            // PC 在线 - 启用按钮
            backupToPCCard.setEnabled(true);
            backupToPCCard.setAlpha(1.0f);
            backupToPCCard.setClickable(true);
            backupToPCTitleTextView.setText(R.string.backup_to_pc);
            backupToPCTitleTextView.setTextColor(getResources().getColor(R.color.black0));
            backupToPCDescTextView.setText(R.string.backup_to_pc_desc);
            backupToPCDescTextView.setTextColor(getResources().getColor(R.color.gray1));
        } else {
            // PC 离线 - 禁用按钮
            backupToPCCard.setEnabled(false);
            backupToPCCard.setAlpha(0.5f);
            backupToPCCard.setClickable(false);
            backupToPCTitleTextView.setText(R.string.backup_to_pc_offline);
            backupToPCTitleTextView.setTextColor(getResources().getColor(R.color.gray11));
            backupToPCDescTextView.setText(R.string.pc_client_not_online);
            backupToPCDescTextView.setTextColor(getResources().getColor(R.color.gray11));
        }
    }

    private boolean isPCOnline() {
        try {
            List<PCOnlineInfo> pcOnlineInfos = ChatManager.Instance().getPCOnlineInfos();
            return pcOnlineInfos != null && !pcOnlineInfos.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private void backupToLocal() {
        // 调用现有的本地备份流程
        // 使用当前用户ID作为备份密码
        String userId = ChatManagerHolder.gChatManager.getUserId();
        BackupProgressActivity.startForBackup(this, conversations, includeMedia, userId, null);
    }

    private void backupToPC() {
        // 跳转到备份请求进度界面
        BackupRequestProgressActivity.start(this, conversations, includeMedia);
    }
}
