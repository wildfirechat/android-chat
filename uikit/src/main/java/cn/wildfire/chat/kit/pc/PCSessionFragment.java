/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pc;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.OnSettingUpdateListener;

/**
 * 「已登录的设备」页（微信风格），与 HarmonyOS 版一致。
 * <p>
 * 每台设备一张白色圆角卡片，第一张默认展开；点击卡片展开、其余收起（手风琴），
 * 再点已展开的卡片收起。卡片头部：小平台图标 + 设备名（加粗）+ 客户端名（灰字）+ 展开箭头；
 * 展开区：大平台图标 + 设备名 + 客户端名居中，下方依次为「手机通知」Switch（全局）、
 * 桌面电脑类设备的「锁定」Switch、「传文件」入口、「退出&lt;设备名&gt;登录」红色按钮。
 * <p>
 * 手机端装在 {@link PCSessionActivity} 这个空壳里，平板上同一份实现进右栏。
 * 入口是会话列表顶部那条多端登录横幅，参数为全部在线设备。
 */
public class PCSessionFragment extends Fragment implements WfcPage, OnSettingUpdateListener {

    private LinearLayout deviceCardContainer;

    private final List<PCOnlineInfo> pcOnlineInfos = new ArrayList<>();
    // 当前展开的卡片下标，-1 表示全部收起；第一张默认展开
    private int expandedPosition = 0;
    private final List<DeviceCardHolder> cardHolders = new ArrayList<>();
    // 程序内同步各卡片「手机通知」开关时置位，避免 setChecked 触发监听器再次请求服务端
    private boolean syncingMuteSwitches = false;

    /**
     * 没有 pcOnlineInfos 就没有可管理的会话，返回 null 让调用方放弃。
     * 兼容旧参数：只有单设备的「pcOnlineInfo」时也接得住。
     */
    @Nullable
    public static PCSessionFragment fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        ArrayList<PCOnlineInfo> infos = intent.getParcelableArrayListExtra("pcOnlineInfos");
        if ((infos == null || infos.isEmpty())) {
            PCOnlineInfo info = intent.getParcelableExtra("pcOnlineInfo");
            if (info != null) {
                infos = new ArrayList<>();
                infos.add(info);
            }
        }
        if (infos == null || infos.isEmpty()) {
            return null;
        }
        PCSessionFragment fragment = new PCSessionFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList("pcOnlineInfos", infos);
        fragment.setArguments(args);
        return fragment;
    }

    private void readInitialInfos() {
        pcOnlineInfos.clear();
        if (getArguments() != null) {
            ArrayList<PCOnlineInfo> infos = getArguments().getParcelableArrayList("pcOnlineInfos");
            if (infos != null) {
                pcOnlineInfos.addAll(infos);
            }
        }
    }

    /**
     * 标题固定为「已登录的设备」，与 HarmonyOS 版一致。
     */
    @Nullable
    @Override
    public CharSequence pageTitle() {
        return getString(R.string.pc_session_title);
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
        deviceCardContainer = view.findViewById(R.id.deviceCardContainer);

        readInitialInfos();
        if (pcOnlineInfos.isEmpty()) {
            WfcPageCompat.finishPage(this);
            return;
        }

        refreshDeviceCards();
    }

    @Override
    public void onResume() {
        super.onResume();
        ChatManager.Instance().addSettingUpdateListener(this);
        refreshFromServer();
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatManager.Instance().removeSettingUpdateListener(this);
    }

    @Override
    public void onSettingUpdate() {
        refreshFromServer();
    }

    /**
     * 从服务端刷新设备列表：全掉线就退掉本页；展开索引越界时重置回第一张。
     */
    private void refreshFromServer() {
        if (!isAdded()) {
            return;
        }
        List<PCOnlineInfo> infos = ChatManager.Instance().getPCOnlineInfos();
        if (infos == null || infos.isEmpty()) {
            WfcPageCompat.finishPage(this);
            return;
        }
        pcOnlineInfos.clear();
        pcOnlineInfos.addAll(infos);
        if (expandedPosition >= pcOnlineInfos.size()) {
            expandedPosition = 0;
        }
        refreshDeviceCards();
    }

    private void refreshDeviceCards() {
        if (deviceCardContainer == null) {
            return;
        }
        deviceCardContainer.removeAllViews();
        cardHolders.clear();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < pcOnlineInfos.size(); i++) {
            PCOnlineInfo info = pcOnlineInfos.get(i);
            View cardView = inflater.inflate(R.layout.pc_session_device_card, deviceCardContainer, false);
            DeviceCardHolder holder = new DeviceCardHolder(cardView, info, i);
            holder.bind();
            deviceCardContainer.addView(cardView);
            cardHolders.add(holder);
        }
    }

    /**
     * 点击卡片：展开当前、收起其它；点击已展开的卡片则收起。
     */
    private void toggleExpand(int position) {
        expandedPosition = expandedPosition == position ? -1 : position;
        for (DeviceCardHolder holder : cardHolders) {
            holder.updateExpandState(holder.position == expandedPosition, true);
        }
    }

    private void kickOffPC(PCOnlineInfo info) {
        ChatManager.Instance().kickoffPCClient(info.getClientId(), new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.pc_logout_success),
                    Toast.LENGTH_SHORT).show();
                // 成功后刷新列表，全部掉线时 refreshFromServer 会关掉本页
                refreshFromServer();
            }

            @Override
            public void onFail(int errorCode) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_failed) + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 手机通知（全局开关，改变手机静音状态）。每台设备的展开区都有一个同样的开关，
     * 成功后同步所有卡片的开关状态，失败则全部还原。
     */
    private void mutePhone(boolean isMute) {
        ChatManager.Instance().muteNotificationWhenPcOnline(isMute, new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_success), Toast.LENGTH_SHORT).show();
                syncMuteSwitches(isMute);
            }

            @Override
            public void onFail(int errorCode) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.operation_failed) + " " + errorCode,
                    Toast.LENGTH_SHORT).show();
                syncMuteSwitches(!isMute);
            }
        });
    }

    /**
     * 同步所有卡片展开区里的「手机通知」开关（全局同一状态），期间抑制监听器回调。
     */
    private void syncMuteSwitches(boolean checked) {
        syncingMuteSwitches = true;
        for (DeviceCardHolder holder : cardHolders) {
            if (holder.muteSwitch != null) {
                holder.muteSwitch.setChecked(checked);
            }
        }
        syncingMuteSwitches = false;
    }

    private void lockPC(PCOnlineInfo info, boolean isLock) {
        ChatManager.Instance().lockPCClient(info.getClientId(), isLock, new GeneralCallback() {
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
                // 还原这台设备的锁定开关
                for (DeviceCardHolder holder : cardHolders) {
                    if (holder.info.getClientId().equals(info.getClientId()) && holder.lockSwitch != null) {
                        holder.lockSwitch.setChecked(!isLock);
                        break;
                    }
                }
            }
        });
    }

    private void fileHelper() {
        Intent intent = ConversationActivity.buildConversationIntent(requireContext(),
            Conversation.ConversationType.Single, Config.FILE_TRANSFER_ID, 0);
        // 本页留在栈里：从文件传输助手返回时应该回到这里，与手机端不 finish 的行为一致
        ConversationRouter.open(this, intent);
    }

    /**
     * 单台设备卡片（微信风格）：
     * 头部 = 小平台图标 + 设备名（加粗）/客户端名（灰字）竖排 + 展开箭头；
     * 展开区 = 大平台图标 + 设备名 + 客户端名居中，功能列表依次为「手机通知」、
     * （桌面电脑类设备的）「锁定」、「传文件」、「退出&lt;设备名&gt;登录」。
     */
    private class DeviceCardHolder {
        final View itemView;
        final PCOnlineInfo info;
        final int position;

        View headerLayout;
        ImageView iconImageView;
        ImageView arrowImageView;
        TextView nameTextView;
        TextView clientNameTextView;
        View expandedLayout;
        ImageView bigIconImageView;
        TextView expandedNameTextView;
        TextView expandedClientNameTextView;
        Switch muteSwitch;
        LinearLayout lockLayout;
        Switch lockSwitch;
        LinearLayout fileLayout;
        TextView logoutTextView;

        DeviceCardHolder(View itemView, PCOnlineInfo info, int position) {
            this.itemView = itemView;
            this.info = info;
            this.position = position;
            headerLayout = itemView.findViewById(R.id.cardHeaderLayout);
            iconImageView = itemView.findViewById(R.id.cardIconImageView);
            arrowImageView = itemView.findViewById(R.id.cardArrowImageView);
            nameTextView = itemView.findViewById(R.id.cardNameTextView);
            clientNameTextView = itemView.findViewById(R.id.cardClientNameTextView);
            expandedLayout = itemView.findViewById(R.id.cardExpandedLayout);
            bigIconImageView = itemView.findViewById(R.id.cardBigIconImageView);
            expandedNameTextView = itemView.findViewById(R.id.cardExpandedNameTextView);
            expandedClientNameTextView = itemView.findViewById(R.id.cardExpandedClientNameTextView);
            muteSwitch = itemView.findViewById(R.id.cardMuteSwitch);
            lockLayout = itemView.findViewById(R.id.cardLockLayout);
            lockSwitch = itemView.findViewById(R.id.cardLockSwitch);
            fileLayout = itemView.findViewById(R.id.cardFileLayout);
            logoutTextView = itemView.findViewById(R.id.cardLogoutTextView);
        }

        void bind() {
            String deviceName = PCOnlineInfoUtil.deviceName(requireContext(), info);
            int iconRes = PCOnlineInfoUtil.platformIconRes(info);
            String clientName = info.getClientName();

            iconImageView.setImageResource(iconRes);
            nameTextView.setText(deviceName);
            bigIconImageView.setImageResource(iconRes);
            expandedNameTextView.setText(deviceName);

            // 客户端名（clientName）为空或为 "unknown" 时不显示小字
            if (TextUtils.isEmpty(clientName) || "unknown".equalsIgnoreCase(clientName)) {
                clientNameTextView.setVisibility(View.GONE);
                expandedClientNameTextView.setVisibility(View.GONE);
            } else {
                clientNameTextView.setText(clientName);
                expandedClientNameTextView.setText(clientName);
            }

            logoutTextView.setText(getString(R.string.pc_session_logout_button, deviceName));

            headerLayout.setOnClickListener(v -> toggleExpand(position));

            // 手机通知（全局开关）：先读初始状态再挂监听，避免 setChecked 被当成用户操作发到服务端
            muteSwitch.setChecked(ChatManager.Instance().isMuteNotificationWhenPcOnline());
            muteSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!syncingMuteSwitches) {
                    mutePhone(isChecked);
                }
            });

            if (PCOnlineInfoUtil.isDesktopDevice(info)) {
                lockLayout.setVisibility(View.VISIBLE);
                // 先读初始状态再挂监听，避免 setChecked 被当成用户操作发到服务端
                lockSwitch.setChecked(ChatManager.Instance().isLockPCClient(info.getClientId()));
                lockSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> lockPC(info, isChecked));
            } else {
                lockLayout.setVisibility(View.GONE);
            }

            fileLayout.setOnClickListener(v -> fileHelper());
            itemView.findViewById(R.id.cardLogoutLayout).setOnClickListener(v -> kickOffPC(info));

            updateExpandState(position == expandedPosition, false);
        }

        void updateExpandState(boolean expanded, boolean animate) {
            expandedLayout.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (animate) {
                arrowImageView.animate().rotation(expanded ? 180 : 0).setDuration(200).start();
            } else {
                arrowImageView.setRotation(expanded ? 180 : 0);
            }
        }
    }
}
