/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.client.Platform;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnSettingUpdateListener;

/**
 * PC 端在线时的会话管理页：踢下线、手机端静音、锁定 PC、进文件传输助手。
 * <p>
 * 手机端装在 {@link PCSessionActivity} 这个空壳里，平板上同一份实现进右栏。
 * 入口是会话列表顶部那条「XX 已登录」横幅。
 */
public class PCSessionFragment extends Fragment implements WfcPage, OnSettingUpdateListener {

    private Button kickOffPCButton;
    private TextView descTextView;
    private Switch muteSwitch;
    private Switch lockSwitch;
    private LinearLayout fileHelperLayout;

    private PCOnlineInfo pcOnlineInfo;

    /**
     * 没有 pcOnlineInfo 就没有可管理的会话，返回 null 让调用方放弃。
     */
    @Nullable
    public static PCSessionFragment fromIntent(@Nullable Intent intent) {
        PCOnlineInfo info = intent == null ? null : intent.getParcelableExtra("pcOnlineInfo");
        if (info == null) {
            return null;
        }
        PCSessionFragment fragment = new PCSessionFragment();
        Bundle args = new Bundle();
        args.putParcelable("pcOnlineInfo", info);
        fragment.setArguments(args);
        return fragment;
    }

    private PCOnlineInfo pcOnlineInfo() {
        if (pcOnlineInfo == null && getArguments() != null) {
            pcOnlineInfo = getArguments().getParcelable("pcOnlineInfo");
        }
        return pcOnlineInfo;
    }

    /**
     * 标题是「Windows 已登录」这种按平台拼出来的，不是 manifest 里的固定 label。
     */
    @Nullable
    @Override
    public CharSequence pageTitle() {
        PCOnlineInfo info = pcOnlineInfo();
        if (info == null) {
            return null;
        }
        return info.getPlatform().getPlatFormName() + " " + getString(R.string.pc_online_status_logged_in);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pc_session_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        kickOffPCButton = view.findViewById(R.id.kickOffPCButton);
        descTextView = view.findViewById(R.id.descTextView);
        muteSwitch = view.findViewById(R.id.muteSwitch);
        lockSwitch = view.findViewById(R.id.lockSwitch);
        fileHelperLayout = view.findViewById(R.id.fileHelperLayout);

        PCOnlineInfo info = pcOnlineInfo();
        if (info == null) {
            WfcPageCompat.finishPage(this);
            return;
        }

        Platform platform = info.getPlatform();
        kickOffPCButton.setText(getString(R.string.pc_session_logout_button, platform.getPlatFormName()));
        descTextView.setText(platform.getPlatFormName() + " " + getString(R.string.pc_online_status_logged_in));

        muteSwitch.setChecked(ChatManager.Instance().isMuteNotificationWhenPcOnline());
        lockSwitch.setChecked(ChatManager.Instance().isLockPCClient(info.getClientId()));

        // 监听器在读完初始状态之后再挂，否则上面两次 setChecked 会被当成用户操作发到服务端
        kickOffPCButton.setOnClickListener(v -> kickOffPC());
        muteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> mutePhone(isChecked));
        lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> lockPC(isChecked));
        fileHelperLayout.setOnClickListener(v -> fileHelper());
    }

    @Override
    public void onResume() {
        super.onResume();
        ChatManager.Instance().addSettingUpdateListener(this);
        checkPCOnlineStatus();
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatManager.Instance().removeSettingUpdateListener(this);
    }

    @Override
    public void onSettingUpdate() {
        checkPCOnlineStatus();
    }

    /**
     * PC 那头下线了，这一页就没有意义了，自己退掉。
     */
    private void checkPCOnlineStatus() {
        PCOnlineInfo info = pcOnlineInfo();
        if (info == null || !isAdded()) {
            return;
        }
        List<PCOnlineInfo> infos = ChatManager.Instance().getPCOnlineInfos();
        for (PCOnlineInfo online : infos) {
            if (online.getClientId().equals(info.getClientId())) {
                return;
            }
        }
        WfcPageCompat.finishPage(this);
    }

    private void kickOffPC() {
        ChatManager.Instance().kickoffPCClient(pcOnlineInfo.getClientId(), new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(),
                    pcOnlineInfo.getPlatform() + " " + getString(R.string.pc_kicked_offline),
                    Toast.LENGTH_SHORT).show();
                WfcPageCompat.finishPage(PCSessionFragment.this);
            }

            @Override
            public void onFail(int errorCode) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), "" + errorCode, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mutePhone(boolean isMute) {
        ChatManager.Instance().muteNotificationWhenPcOnline(isMute, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(int errorCode) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_failed) + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
                muteSwitch.setChecked(!isMute);
            }
        });
    }

    private void lockPC(boolean isLock) {
        ChatManager.Instance().lockPCClient(pcOnlineInfo.getClientId(), isLock, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_success), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFail(int errorCode) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_failed) + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
                lockSwitch.setChecked(!isLock);
            }
        });
    }

    private void fileHelper() {
        Intent intent = ConversationActivity.buildConversationIntent(requireContext(),
            Conversation.ConversationType.Single, Config.FILE_TRANSFER_ID, 0);
        // 本页留在栈里：从文件传输助手返回时应该回到这里，与手机端不 finish 的行为一致
        ConversationRouter.open(this, intent);
    }
}
