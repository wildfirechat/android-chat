/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.audio.AudioPlayManager;
import cn.wildfire.chat.kit.audio.AudioPlayModeUtils;

/**
 * 聊天设置页，目前只有「使用听筒播放语音消息」一项。
 * <p>
 * 这一项与语音消息长按菜单里的「听筒/扬声器播放」是同一个全局设置：两边写的都是
 * {@link AudioPlayModeUtils}，并互相监听对方的改动，所以在菜单里切换之后回到本页，
 * 开关已经是新的状态；反过来在本页拨动开关，会话页标题上的听筒图标也会立刻跟着变。
 * <p>
 * 手机端装在 {@link ChatSettingActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class ChatSettingFragment extends Fragment implements AudioPlayModeUtils.OnAudioPlayModeChangedListener {

    private SwitchMaterial switchAudioPlayInEarpiece;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_chat_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchAudioPlayInEarpiece = view.findViewById(R.id.switchAudioPlayInEarpiece);
        switchAudioPlayInEarpiece.setChecked(AudioPlayModeUtils.isEarpieceMode(requireContext()));
        switchAudioPlayInEarpiece.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = requireContext();
            if (AudioPlayModeUtils.isEarpieceMode(context) == isChecked) {
                // 回填开关状态触发的回调，不是用户拨动，什么都不用做
                return;
            }
            AudioPlayModeUtils.setEarpieceMode(context, isChecked);
            // 与长按菜单一致：切换播放方式后停掉当前播放，下一条按新方式播
            AudioPlayManager.getInstance().stopPlay();
        });
        AudioPlayModeUtils.addOnAudioPlayModeChangedListener(this);
    }

    @Override
    public void onDestroyView() {
        AudioPlayModeUtils.removeOnAudioPlayModeChangedListener(this);
        switchAudioPlayInEarpiece = null;
        super.onDestroyView();
    }

    /**
     * 别处（语音消息长按菜单）改了播放方式，把开关同步过来。
     */
    @Override
    public void onAudioPlayModeChanged(boolean earpiece) {
        if (switchAudioPlayInEarpiece != null && switchAudioPlayInEarpiece.isChecked() != earpiece) {
            switchAudioPlayInEarpiece.setChecked(earpiece);
        }
    }
}
