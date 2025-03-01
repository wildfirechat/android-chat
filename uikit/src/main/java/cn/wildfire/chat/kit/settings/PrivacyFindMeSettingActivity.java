/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.ChatManager.DisableSearchUserMask;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.UserSettingScope;

public class PrivacyFindMeSettingActivity extends WfcBaseActivity {
    SwitchMaterial displayNameSwitch;
    SwitchMaterial nameSwitch;
    SwitchMaterial mobileSwitch;
    FrameLayout displayNameOptionLayout;


    protected void bindViews() {
        super.bindViews();
        displayNameSwitch = findViewById(R.id.displayNameSwitch);
        nameSwitch = findViewById(R.id.nameSwitch);
        mobileSwitch = findViewById(R.id.mobileSwitch);
    }

    @Override
    protected int contentLayout() {
        return R.layout.privacy_find_me_activity;
    }

    @Override
    protected void afterViews() {
        int searchableFlag = getUserPrivacySearchableFlag();

        //如果搜索用户支持按照昵称搜索，请打开这里；
//        displayNameOptionLayout.setVisibility(View.VISIBLE);
        displayNameSwitch.setChecked((searchableFlag & DisableSearchUserMask.DisplayName) == 0);
        displayNameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Mobile | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Name;
            }
            ChatManager.Instance().setUserSetting(UserSettingScope.Privacy_Searchable, null, flag + "", new GeneralCallback() {
                @Override
                public void onSuccess() {
                    displayNameSwitch.setChecked(isChecked);
                }

                @Override
                public void onFail(int errorCode) {
                    displayNameSwitch.setChecked(isChecked);
                    Toast.makeText(PrivacyFindMeSettingActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            });
        });

        nameSwitch.setChecked((searchableFlag & DisableSearchUserMask.Name) == 0);
        nameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Mobile | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Name;
            }
            ChatManager.Instance().setUserSetting(UserSettingScope.Privacy_Searchable, null, flag + "", new GeneralCallback() {
                @Override
                public void onSuccess() {
                    nameSwitch.setChecked(isChecked);
                }

                @Override
                public void onFail(int errorCode) {
                    nameSwitch.setChecked(isChecked);
                    Toast.makeText(PrivacyFindMeSettingActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            });
        });

        mobileSwitch.setChecked((searchableFlag & ChatManager.DisableSearchUserMask.Mobile) == 0);
        mobileSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Name | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Mobile;
            }
            ChatManager.Instance().setUserSetting(UserSettingScope.Privacy_Searchable, null, flag + "", new GeneralCallback() {
                @Override
                public void onSuccess() {
                    mobileSwitch.setChecked(isChecked);
                }

                @Override
                public void onFail(int errorCode) {
                    mobileSwitch.setChecked(isChecked);
                    Toast.makeText(PrivacyFindMeSettingActivity.this, getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private int getUserPrivacySearchableFlag() {
        int flag = 0;
        try {
            String settingValue = ChatManager.Instance().getUserSetting(UserSettingScope.Privacy_Searchable, "");
            flag = Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return flag;
    }
}
