/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Intent;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListActivity;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class PrivacySettingActivity extends WfcBaseActivity {

    private SwitchMaterial switchAddFriendNeedVerify;

    protected void bindViews() {
        super.bindViews();
        this.switchAddFriendNeedVerify = findViewById(R.id.switchAddFriendNeedVerify);

        this.switchAddFriendNeedVerify.setChecked(ChatManager.Instance().isAddFriendNeedVerify());
    }

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.blacklistOptionItemView).setOnClickListener(v -> blacklistSettings());
        findViewById(R.id.momentsPrivacyOptionItemView).setOnClickListener(v -> mementsSettings());
        findViewById(R.id.findMeOptionItemView).setOnClickListener(v -> findMeSettings());

        this.switchAddFriendNeedVerify.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ChatManager.Instance().setAddFriendNeedVerify(isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    // do nothing
                }

                @Override
                public void onFail(int errorCode) {
                    if (!isFinishing()) {
                        Toast.makeText(PrivacySettingActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    @Override
    protected int contentLayout() {
        return R.layout.privacy_setting_activity;
    }

    void blacklistSettings() {
        Intent intent = new Intent(this, BlacklistListActivity.class);
        startActivity(intent);
    }

    void mementsSettings() {

    }

    void findMeSettings() {
        Intent intent = new Intent(this, PrivacyFindMeSettingActivity.class);
        startActivity(intent);
    }
}
