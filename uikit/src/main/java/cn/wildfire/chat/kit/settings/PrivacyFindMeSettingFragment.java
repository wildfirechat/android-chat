/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.ChatManager.DisableSearchUserMask;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.UserSettingScope;

/**
 * 「如何找到我」设置页。手机端装在 {@link PrivacyFindMeSettingActivity} 这个空壳里，
 * 平板上同一份实现进右栏。
 */
public class PrivacyFindMeSettingFragment extends Fragment {

    private SwitchMaterial displayNameSwitch;
    private SwitchMaterial nameSwitch;
    private SwitchMaterial mobileSwitch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.privacy_find_me_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        displayNameSwitch = view.findViewById(R.id.displayNameSwitch);
        nameSwitch = view.findViewById(R.id.nameSwitch);
        mobileSwitch = view.findViewById(R.id.mobileSwitch);

        int searchableFlag = getUserPrivacySearchableFlag();

        //如果搜索用户支持按照昵称搜索，请打开这里；
//        view.findViewById(R.id.displayNameOptionLayout).setVisibility(View.VISIBLE);
        displayNameSwitch.setChecked((searchableFlag & DisableSearchUserMask.DisplayName) == 0);
        displayNameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Mobile | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Name;
            }
            applyFlag(flag, displayNameSwitch, isChecked);
        });

        nameSwitch.setChecked((searchableFlag & DisableSearchUserMask.Name) == 0);
        nameSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Mobile | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Name;
            }
            applyFlag(flag, nameSwitch, isChecked);
        });

        mobileSwitch.setChecked((searchableFlag & DisableSearchUserMask.Mobile) == 0);
        mobileSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int flag = getUserPrivacySearchableFlag();
            if (isChecked) {
                flag &= (DisableSearchUserMask.DisplayName | DisableSearchUserMask.Name | DisableSearchUserMask.UserId);
            } else {
                flag |= DisableSearchUserMask.Mobile;
            }
            applyFlag(flag, mobileSwitch, isChecked);
        });
    }

    /**
     * 三个开关的落库逻辑逐字相同，改造前是抄了三遍。
     */
    private void applyFlag(int flag, SwitchMaterial target, boolean isChecked) {
        ChatManager.Instance().setUserSetting(UserSettingScope.Privacy_Searchable, null, flag + "", new GeneralCallback() {
            @Override
            public void onSuccess() {
                target.setChecked(isChecked);
            }

            @Override
            public void onFail(int errorCode) {
                target.setChecked(isChecked);
                if (isAdded()) {
                    Toast.makeText(getActivity(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }
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
