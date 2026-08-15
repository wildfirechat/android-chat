/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

/**
 * 新消息通知设置页。手机端装在 {@link MessageNotifySettingActivity} 这个空壳里，
 * 平板上同一份实现进右栏。
 */
public class MessageNotifySettingFragment extends Fragment {

    private SwitchMaterial switchMsgNotification;
    private SwitchMaterial switchVoipNotification;
    private SwitchMaterial switchShowMsgDetail;
    private SwitchMaterial switchUserReceipt;
    private SwitchMaterial switchSyncDraft;
    private SwitchMaterial switchPtt;
    private SwitchMaterial switchAudioMessageAmplification;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_msg_notify_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchMsgNotification = view.findViewById(R.id.switchMsgNotification);
        switchVoipNotification = view.findViewById(R.id.switchVoipNotification);
        switchShowMsgDetail = view.findViewById(R.id.switchShowMsgDetail);
        switchUserReceipt = view.findViewById(R.id.switchUserReceipt);
        switchSyncDraft = view.findViewById(R.id.switchSyncDraft);
        switchPtt = view.findViewById(R.id.switchPtt);
        switchAudioMessageAmplification = view.findViewById(R.id.switchAudioMessageAmplification);

        switchMsgNotification.setChecked(!ChatManager.Instance().isGlobalSilent());
        switchMsgNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setGlobalSilent(!isChecked, toastOnFail()));

        switchVoipNotification.setChecked(!ChatManager.Instance().isVoipSilent());
        switchVoipNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setVoipSilent(!isChecked, toastOnFail()));

        switchShowMsgDetail.setChecked(!ChatManager.Instance().isHiddenNotificationDetail());
        switchShowMsgDetail.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setHiddenNotificationDetail(!isChecked, toastOnFail()));

        switchUserReceipt.setChecked(ChatManager.Instance().isUserEnableReceipt());
        switchUserReceipt.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setUserEnableReceipt(isChecked, toastOnFail()));

        switchSyncDraft.setChecked(!ChatManager.Instance().isDisableSyncDraft());
        switchSyncDraft.setOnCheckedChangeListener((buttonView, isChecked) ->
            ChatManager.Instance().setDisableSyncDraft(!isChecked, new GeneralCallback() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFail(int errorCode) {
                }
            }));

        SharedPreferences sp = requireContext().getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);

        // 对讲开关原来挂在 switchSyncDraft 上（复制粘贴时漏改），结果是：拨动「同步草稿」会去写
        // pttEnabled 并把同步草稿自己的监听覆盖掉，而「对讲」开关拨了没有任何反应。
        switchPtt.setChecked(sp.getBoolean("pttEnabled", true));
        switchPtt.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean("pttEnabled", isChecked).apply();
            Toast.makeText(getActivity(), R.string.ptt_toggle_restart_tip, Toast.LENGTH_SHORT).show();
        });

        switchAudioMessageAmplification.setChecked(sp.getBoolean("audioMessageAmplificationEnabled", false));
        switchAudioMessageAmplification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sp.edit().putBoolean("audioMessageAmplificationEnabled", isChecked).apply();
            Config.ENABLE_AUDIO_MESSAGE_AMPLIFICATION = isChecked;
        });
    }

    private GeneralCallback toastOnFail() {
        return new GeneralCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFail(int errorCode) {
                if (isAdded()) {
                    Toast.makeText(getActivity(), getString(R.string.network_error), Toast.LENGTH_SHORT).show();
                }
            }
        };
    }
}
