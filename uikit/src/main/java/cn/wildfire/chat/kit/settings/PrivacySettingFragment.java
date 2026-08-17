/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Intent;
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
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.settings.blacklist.BlacklistListActivity;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

/**
 * 隐私设置页。手机端装在 {@link PrivacySettingActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class PrivacySettingFragment extends Fragment {

    private SwitchMaterial switchAddFriendNeedVerify;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.privacy_setting_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchAddFriendNeedVerify = view.findViewById(R.id.switchAddFriendNeedVerify);
        switchAddFriendNeedVerify.setChecked(ChatManager.Instance().isAddFriendNeedVerify());
        switchAddFriendNeedVerify.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setAddFriendNeedVerify(isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    // do nothing
                }

                @Override
                public void onFail(int errorCode) {
                    if (isAdded()) {
                        Toast.makeText(getActivity(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                    }
                }
            }));

        view.findViewById(R.id.blacklistOptionItemView).setOnClickListener(v -> blacklistSettings());
        // 朋友圈隐私暂未实现，保留入口占位（改造前也是空实现）
        view.findViewById(R.id.momentsPrivacyOptionItemView).setOnClickListener(v -> momentsSettings());
        view.findViewById(R.id.findMeOptionItemView).setOnClickListener(v -> findMeSettings());
    }

    private void blacklistSettings() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), BlacklistListActivity.class));
    }

    private void momentsSettings() {
    }

    private void findMeSettings() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), PrivacyFindMeSettingActivity.class));
    }
}
