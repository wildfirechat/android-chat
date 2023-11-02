/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Intent;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListActivity;

public class PrivacySettingActivity extends WfcBaseActivity {

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.blacklistOptionItemView).setOnClickListener(v -> blacklistSettings());
        findViewById(R.id.momentsPrivacyOptionItemView).setOnClickListener(v -> mementsSettings());
        findViewById(R.id.findMeOptionItemView).setOnClickListener(v -> findMeSettings());
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
