/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.viewmodel.SettingViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 恢复源选择界面
 */
public class RestoreSourceActivity extends WfcBaseActivity {

    private CardView restoreFromPCCard;
    private TextView restoreFromPCTitleTextView;
    private TextView restoreFromPCDescTextView;

    private SettingViewModel settingViewModel;

    public static void start(Context context) {
        Intent intent = new Intent(context, RestoreSourceActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_restore_source;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        observeSettingChanges();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePCOnlineStatus();
    }

    private void initView() {
        findViewById(R.id.restoreFromLocalCard).setOnClickListener(v -> restoreFromLocal());

        restoreFromPCCard = findViewById(R.id.restoreFromPCCard);
        restoreFromPCTitleTextView = restoreFromPCCard.findViewById(R.id.restoreFromPCTitleTextView);
        restoreFromPCDescTextView = restoreFromPCCard.findViewById(R.id.restoreFromPCDescTextView);

        restoreFromPCCard.setOnClickListener(v -> {
            if (isPCOnline()) {
                restoreFromPC();
            } else {
                Toast.makeText(this, R.string.pc_is_not_online, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeSettingChanges() {
        settingViewModel = new ViewModelProvider(this).get(SettingViewModel.class);

        settingViewModel.settingUpdatedLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                updatePCOnlineStatus();
            }
        });
    }

    private void updatePCOnlineStatus() {
        boolean isOnline = isPCOnline();

        if (isOnline) {
            restoreFromPCCard.setEnabled(true);
            restoreFromPCCard.setAlpha(1.0f);
            restoreFromPCCard.setClickable(true);
            restoreFromPCTitleTextView.setText(R.string.restore_from_pc);
            restoreFromPCTitleTextView.setTextColor(getResources().getColor(R.color.black0));
            restoreFromPCDescTextView.setText(R.string.restore_from_pc_desc);
            restoreFromPCDescTextView.setTextColor(getResources().getColor(R.color.gray1));
        } else {
            restoreFromPCCard.setEnabled(false);
            restoreFromPCCard.setAlpha(0.5f);
            restoreFromPCCard.setClickable(false);
            restoreFromPCTitleTextView.setText(R.string.restore_from_pc_offline);
            restoreFromPCTitleTextView.setTextColor(getResources().getColor(R.color.gray11));
            restoreFromPCDescTextView.setText(R.string.pc_client_not_online);
            restoreFromPCDescTextView.setTextColor(getResources().getColor(R.color.gray11));
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

    private void restoreFromLocal() {
        BackupListActivity.start(this);
    }

    private void restoreFromPC() {
        PCRestoreListActivity.start(this);
    }
}
