/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Date;
import java.util.Objects;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.BooleanCallback;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.uikit.permission.PermissionKit;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

/**
 * 会议详情页。
 * <p>
 * 逐行搬自 {@link ConferenceInfoActivity}，那个类现在只是手机端的壳。入口有三条，都在右栏
 * 可达的路径上：会话里的会议邀请消息、会议入口页的收藏列表、扫码落地。不迁的话点开要整屏
 * 跳出去再跳回来。<strong>「加入会议」仍启动全屏的 {@link ConferenceActivity}</strong>——
 * 音视频界面本来就是全屏形态，不在右栏里。
 */
public class ConferenceInfoPageFragment extends Fragment implements WfcPage {

    private String conferenceId;
    private String password;
    private ConferenceInfo conferenceInfo;

    TextView titleTextView;
    TextView ownerTextView;
    TextView callIdTextView;
    TextView startDateTimeView;
    TextView endDateTimeView;
    SwitchMaterial audioSwitch;
    SwitchMaterial videoSwitch;
    Button joinConferenceButton;

    private MenuItem destroyItem;
    private MenuItem favItem;
    private MenuItem unFavItem;

    /**
     * 是否已收藏。收藏状态是异步查回来的，且用户随时可能切换，菜单显隐以它为唯一依据。
     * -1=还不知道，0=未收藏（显示「收藏」），1=已收藏（显示「取消收藏」）。
     */
    private int favState = -1;

    public static ConferenceInfoPageFragment fromIntent(Intent intent) {
        ConferenceInfoPageFragment fragment = new ConferenceInfoPageFragment();
        Bundle args = new Bundle();
        if (intent != null) {
            args.putString("conferenceId", intent.getStringExtra("conferenceId"));
            args.putString("password", intent.getStringExtra("password"));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.av_conference_info_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        conferenceId = getArguments() == null ? null : getArguments().getString("conferenceId");
        password = getArguments() == null ? null : getArguments().getString("password");

        titleTextView = view.findViewById(R.id.titleTextView);
        ownerTextView = view.findViewById(R.id.ownerTextView);
        callIdTextView = view.findViewById(R.id.callIdTextView);
        startDateTimeView = view.findViewById(R.id.startDateTimeTextView);
        endDateTimeView = view.findViewById(R.id.endDateTimeTextView);
        audioSwitch = view.findViewById(R.id.audioSwitch);
        videoSwitch = view.findViewById(R.id.videoSwitch);
        joinConferenceButton = view.findViewById(R.id.joinConferenceBtn);

        view.findViewById(R.id.conferenceQRCodeLinearLayout).setOnClickListener(v -> showConferenceQRCode());
        joinConferenceButton.setOnClickListener(v -> joinConference());

        WfcUIKit.getWfcUIKit().getAppServiceProvider().queryConferenceInfo(conferenceId, password, new AppServiceProvider.QueryConferenceInfoCallback() {
            @Override
            public void onSuccess(ConferenceInfo info) {
                if (getView() == null) {
                    return;
                }
                setupConferenceInfo(info);
                if (!info.getOwner().equals(ChatManager.Instance().getUserId())) {
                    WfcUIKit.getWfcUIKit().getAppServiceProvider().isFavConference(conferenceId, new BooleanCallback() {
                        @Override
                        public void onSuccess(boolean isFav) {
                            if (getView() == null) {
                                return;
                            }
                            favState = isFav ? 1 : 0;
                            WfcPageCompat.invalidatePageMenu(ConferenceInfoPageFragment.this);
                        }

                        @Override
                        public void onFail(int code, String msg) {
                            if (getView() == null) {
                                return;
                            }
                            favState = 0;
                            WfcPageCompat.invalidatePageMenu(ConferenceInfoPageFragment.this);
                        }
                    });
                }
            }

            @Override
            public void onFail(int code, String msg) {
                if (getView() == null) {
                    return;
                }
                Toast.makeText(getContext(), getString(R.string.conf_get_info_failed), Toast.LENGTH_SHORT).show();
                WfcPageCompat.finishPage(ConferenceInfoPageFragment.this);
            }
        });
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.conference_info;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        destroyItem = menu.findItem(R.id.destroy);
        favItem = menu.findItem(R.id.fav);
        unFavItem = menu.findItem(R.id.unfav);
        if (conferenceInfo != null) {
            if (Objects.equals(conferenceInfo.getOwner(), ChatManager.Instance().getUserId())) {
                destroyItem.setVisible(true);
            } else {
                destroyItem.setVisible(false);
            }
        }
        // 收藏按钮只有非创建者才显示，且只在查回收藏状态之后才出现（favState >= 0）
        boolean canFav = conferenceInfo != null
            && !Objects.equals(conferenceInfo.getOwner(), ChatManager.Instance().getUserId())
            && favState >= 0;
        favItem.setVisible(canFav && favState == 0);
        unFavItem.setVisible(canFav && favState == 1);
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.destroy) {
            ConferenceManager.getManager().destroyConference(conferenceId, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    if (getView() == null) {
                        return;
                    }
                    Toast.makeText(getContext(), getString(R.string.conf_destroy_success), Toast.LENGTH_SHORT).show();
                    WfcPageCompat.finishPage(ConferenceInfoPageFragment.this);
                }

                @Override
                public void onFail(int i) {
                    if (getView() == null) {
                        return;
                    }
                    Toast.makeText(getContext(), getString(R.string.conf_destroy_failed, i), Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        } else if (item.getItemId() == R.id.fav) {
            this.onFav(true);
            return true;
        } else if (item.getItemId() == R.id.unfav) {
            this.onFav(false);
            return true;
        }
        return false;
    }

    void showConferenceQRCode() {
        String qrcodeValue = WfcScheme.buildConferenceScheme(conferenceId, password);
        Intent intent = QRCodeActivity.buildQRCodeIntent(requireContext(), getString(R.string.conf_qrcode_title), null, qrcodeValue);
        WfcPageCompat.startPage(this, intent);
    }

    void joinConference() {
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(requireActivity(), permissions);
        PermissionKit.checkThenRequestPermission(requireActivity(), requireActivity().getSupportFragmentManager(), tuples, o -> {
            if (o) {
                ConferenceInfo info = conferenceInfo;
                boolean audience = !audioSwitch.isChecked() && !videoSwitch.isChecked();
                boolean muteVideo = audience || !videoSwitch.isChecked();
                boolean muteAudio = audience || !audioSwitch.isChecked();
                AVEngineKit.CallSession session = AVEngineKit.Instance().joinConference(info.getConferenceId(), false, info.getPin(), info.getOwner(), info.getConferenceTitle(), "", audience, info.isAdvance(), muteAudio, muteVideo, null);
                if (session != null) {
                    Intent intent = new Intent(getContext(), ConferenceActivity.class);
                    // 会议界面是全屏形态，不登记右栏，走原始 startActivity
                    startActivity(intent);
                    WfcPageCompat.finishAfterOpeningPage(ConferenceInfoPageFragment.this);
                } else {
                    Toast.makeText(getContext(), getString(R.string.join_conf_failed), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupConferenceInfo(ConferenceInfo info) {
        conferenceInfo = info;
        ConferenceManager.getManager().setCurrentConferenceInfo(info);
        titleTextView.setText(info.getConferenceTitle());
        String owner = info.getOwner();
        String ownerName = ChatManager.Instance().getUserDisplayName(owner);
        ownerTextView.setText(ownerName);
        callIdTextView.setText(info.getConferenceId());
        startDateTimeView.setText(info.getStartTime() == 0 ? getString(R.string.conf_start_time_now) : new Date(info.getStartTime() * 1000).toString());
        endDateTimeView.setText(new Date(info.getEndTime() * 1000).toString());

        if (info.isAudience() && !info.isAllowTurnOnMic() && !owner.equals(ChatManager.Instance().getUserId())) {
            audioSwitch.setChecked(false);
            videoSwitch.setChecked(false);
            audioSwitch.setEnabled(false);
            videoSwitch.setEnabled(false);
        }
        long now = System.currentTimeMillis() / 1000;
        if (now > info.getEndTime()) {
            joinConferenceButton.setEnabled(false);
            joinConferenceButton.setText(R.string.conf_ended);
        } else if (now < info.getStartTime()) {
            joinConferenceButton.setEnabled(false);
            joinConferenceButton.setText(R.string.conf_not_started);
        } else {
            joinConferenceButton.setEnabled(true);
            joinConferenceButton.setText(R.string.conf_join);
        }

        WfcPageCompat.invalidatePageMenu(this);
    }

    private void onFav(boolean fav) {
        GeneralCallback callback = new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (getView() == null) {
                    return;
                }
                favState = fav ? 1 : 0;
                WfcPageCompat.invalidatePageMenu(ConferenceInfoPageFragment.this);
            }

            @Override
            public void onFail(int errorCode) {

            }
        };
        if (fav) {
            WfcUIKit.getWfcUIKit().getAppServiceProvider().favConference(conferenceId, callback);
        } else {
            WfcUIKit.getWfcUIKit().getAppServiceProvider().unfavConference(conferenceId, callback);
        }
    }
}
