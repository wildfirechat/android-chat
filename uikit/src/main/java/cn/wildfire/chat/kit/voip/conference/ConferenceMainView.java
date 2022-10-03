/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

class ConferenceMainView extends RelativeLayout {
    @BindView(R2.id.rootView)
    RelativeLayout rootLinearLayout;

    @BindView(R2.id.topBarView)
    LinearLayout topBarView;

    @BindView(R2.id.bottomPanel)
    FrameLayout bottomPanel;

    @BindView(R2.id.titleTextView)
    TextView titleTextView;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;

    @BindView(R2.id.manageParticipantTextView)
    TextView manageParticipantTextView;

    @BindView(R2.id.previewContainerFrameLayout)
    FrameLayout previewContainerFrameLayout;
    @BindView(R2.id.focusContainerFrameLayout)
    FrameLayout focusContainerFrameLayout;

    @BindView(R2.id.muteImageView)
    ImageView muteAudioImageView;
    @BindView(R2.id.videoImageView)
    ImageView muteVideoImageView;
    @BindView(R2.id.shareScreenImageView)
    ImageView shareScreenImageView;

    private AVEngineKit.CallSession callSession;
    private AVEngineKit.ParticipantProfile myProfile;
    private AVEngineKit.ParticipantProfile focusProfile;

    public ConferenceMainView(Context context) {
        super(context);
        initView(context, null);
    }

    public ConferenceMainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ConferenceMainView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConferenceMainView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_conference_main, this);
        ButterKnife.bind(this, view);
        handler.post(updateCallDurationRunnable);
    }

    public void setup(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile myProfile, AVEngineKit.ParticipantProfile focusProfile) {
        this.callSession = session;
        this.myProfile = myProfile;
        this.focusProfile = focusProfile;
        titleTextView.setText(this.callSession.getTitle());
        setupConferenceMainView();
    }

    public void updateMyProfile(AVEngineKit.ParticipantProfile myProfile) {
        if (this.myProfile == null) {
            // myProfile 和 focusProfile 换位置
        }
        this.myProfile = myProfile;
        setupConferenceMainView();
    }

    public void updateFocusProfile(AVEngineKit.ParticipantProfile focusProfile) {
        if (this.focusProfile == null) {
            // myProfile 和 focusProfile 换位置
        }
        this.focusProfile = focusProfile;
        setupConferenceMainView();
    }

    public void updateParticipantVolume(String userId, int volume) {

    }

    public void onDestroyView() {
        // TODO
        // do nothing
    }

    private UserInfo me;

    // TODO 移除，并将VoipBaseActivity.focusVideoUserId 修改为static
    private String focusVideoUserId;


    public static final String TAG = "ConferenceVideoFragment";

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private void setupConferenceMainView() {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);

        previewContainerFrameLayout.removeAllViews();

        focusVideoUserId = myProfile.getUserId();

        List<AVEngineKit.ParticipantProfile> mainProfiles = new ArrayList<>();
        if (!myProfile.isAudience()) {
            mainProfiles.add(myProfile);
        }
        if (focusProfile != null && !focusProfile.getUserId().equals(myProfile.getUserId())) {
            mainProfiles.add(focusProfile);
            focusVideoUserId = focusProfile.getUserId();
        }

        for (AVEngineKit.ParticipantProfile profile : mainProfiles) {
            ConferenceParticipantItemView conferenceItem = new ConferenceParticipantItemView(getContext());
            conferenceItem.setOnClickListener(clickListener);
            conferenceItem.setup(this.callSession, profile);

            if (focusProfile != null) {
                if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                    previewContainerFrameLayout.removeAllViews();
                    conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(size / 3, size / 3));
                    previewContainerFrameLayout.addView(conferenceItem);
                } else {
                    focusContainerFrameLayout.removeAllViews();
                    conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    focusContainerFrameLayout.addView(conferenceItem);
                    if (!profile.isVideoMuted()) {
                        this.callSession.setParticipantVideoType(focusProfile.getUserId(), focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_BIG_STREAM);
                    }
                }
            } else {
                previewContainerFrameLayout.removeAllViews();

                focusContainerFrameLayout.removeAllViews();
                conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                focusContainerFrameLayout.addView(conferenceItem);
            }
        }

        handler.post(() -> {
            muteVideoImageView.setSelected(myProfile.isVideoMuted());
            muteAudioImageView.setSelected(myProfile.isAudioMuted());
        });
    }

    @OnClick(R2.id.minimizeImageView)
    void minimize() {
//        ((ConferenceActivity) getActivity()).showFloatingView(focusVideoUserId);
        // VoipBaseActivity#onStop会处理，这儿仅仅finish
        ((Activity) getContext()).finish();
    }

    @OnClick(R2.id.manageParticipantView)
    void addParticipant() {
        ((ConferenceActivity) getContext()).showParticipantList();
    }

    @OnClick(R2.id.muteView)
    void muteAudio() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.isAudioMuted();
            muteAudioImageView.setSelected(toMute);

            if (toMute) {
                if (session.videoMuted) {
                    session.switchAudience(true);
                }
                session.muteAudio(true);
            } else {
                session.muteAudio(false);
                if (session.videoMuted) {
                    session.switchAudience(false);
                }
            }
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.videoView)
    void muteVideo() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.videoMuted;
            muteVideoImageView.setSelected(toMute);
            if (toMute) {
                if (session.audioMuted) {
                    session.switchAudience(true);
                }
                session.muteVideo(true);
            } else {
                session.muteVideo(false);
                if (session.audioMuted) {
                    session.switchAudience(false);
                }
            }
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.hangupView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            if (ChatManager.Instance().getUserId().equals(session.getHost())) {
                new AlertDialog.Builder(getContext())
                    .setMessage("请选择是否结束会议")
                    .setIcon(R.mipmap.ic_launcher)
                    .setNeutralButton("退出会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(false);
                    })
                    .setPositiveButton("结束会议", (dialogInterface, i) -> {
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(true);
                    })
                    .create()
                    .show();
            } else {
                session.leaveConference(false);
            }
        }
    }

    @OnClick(R2.id.shareScreenView)
    void shareScreen() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            if (session.isAudience()) {
                return;
            }

//            shareScreenImageView.setSelected(!session.isScreenSharing());
            if (!session.isScreenSharing()) {
                ((VoipBaseActivity) getContext()).startScreenShare();
            } else {
                ((VoipBaseActivity) getContext()).stopScreenShare();
            }
        }
    }

    @OnClick(R2.id.titleLinearLayout)
    void showConferenceInfoDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_info_dialog, null);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView conferenceIdTextView = view.findViewById(R.id.conferenceIdTextView);
        TextView conferenceHostTextView = view.findViewById(R.id.hostTextView);
        TextView conferenceLinkTextView = view.findViewById(R.id.conferenceLinkTextView);

        titleTextView.setText(callSession.getTitle());
        conferenceIdTextView.setText(callSession.getCallId());
        conferenceHostTextView.setText(callSession.getHost());
        String conferenceLink = WfcScheme.buildConferenceScheme(callSession.getCallId(), callSession.getPin());
        conferenceLinkTextView.setText(conferenceLink);

        view.findViewById(R.id.copyCallIdImageView).setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                return;
            }
            ClipData clipData = ClipData.newPlainText("conferenceId", callSession.getCallId());
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        view.findViewById(R.id.copyLinkImageView).setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                return;
            }
            ClipData clipData = ClipData.newPlainText("conferenceLink", conferenceLink);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = (String) v.getTag();
            if (userId != null && !userId.equals(focusVideoUserId)) {
                if (bottomPanel.getVisibility() == View.GONE) {
                    setPanelVisibility(View.VISIBLE);
                    startHideBarTimer();
                }
            } else {
                if (bottomPanel.getVisibility() == View.GONE) {
                    setPanelVisibility(View.VISIBLE);
                    startHideBarTimer();
                } else {
                    setPanelVisibility(View.GONE);
                }
            }
        }
    };

    private void startHideBarTimer() {
        if (bottomPanel.getVisibility() == View.GONE) {
            return;
        }
        handler.removeCallbacks(hideBarCallback);
        handler.postDelayed(hideBarCallback, 3000);
    }

    private final Runnable hideBarCallback = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                setPanelVisibility(View.GONE);
            }
        }
    };

    private void setPanelVisibility(int visibility) {
        bottomPanel.setVisibility(visibility);
        topBarView.setVisibility(visibility);
        // TODO status bar
        Activity activity = ((Activity) getContext());
        if (visibility == VISIBLE) {
//            activity.requestWindowFeature(Window.FEATURE_ACTION_BAR);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
//            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        }
    }

    private final Handler handler = new Handler();

    private final Runnable updateCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
            if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
                String text;
                if (session.getConnectedTime() == 0) {
                    text = "会议连接中";
                } else {
                    long s = System.currentTimeMillis() - session.getConnectedTime();
                    s = s / 1000;
                    if (s > 3600) {
                        text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
                    } else {
                        text = String.format("%02d:%02d", s / 60, (s % 60));
                    }
                }
                durationTextView.setText(text);
            }
            handler.postDelayed(updateCallDurationRunnable, 1000);
        }
    };


//    @Override
//    public void onStop() {
//        super.onStop();
//        handler.removeCallbacks(hideBarCallback);
//        handler.removeCallbacks(updateCallDurationRunnable);
//    }

}
