/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

/**
 * 消息备份与恢复主界面
 */
public class BackupAndRestoreActivity extends WfcBaseActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, BackupAndRestoreActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_backup_restore;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 添加 Fragment
        if (savedInstanceState == null) {
            Fragment fragment = new BackupAndRestoreFragmentImpl();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }

    /**
     * 内部实现类，可以直接访问 chat 模块的类
     */
    public static class BackupAndRestoreFragmentImpl extends BackupAndRestoreFragment {
        @Override
        protected void onCreateBackupClick() {
            ConversationSelectActivity.start(requireContext());
        }

        @Override
        protected void onRestoreBackupClick() {
            RestoreSourceActivity.start(requireContext());
        }
    }
}
