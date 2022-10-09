/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;
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

    @BindView(R2.id.speakerImageView)
    ImageView speakerImageView;

    @BindView(R2.id.micLinearLayout)
    LinearLayout micLinearLayout;
    @BindView(R2.id.micImageView)
    MicImageView micImageView;

    private AVEngineKit.CallSession callSession;
    private AVEngineKit.ParticipantProfile myProfile;
    private AVEngineKit.ParticipantProfile focusProfile;

    private ConferenceParticipantItemView focusParticipantItemView;
    private ConferenceParticipantItemView myParticipantItemView;

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
        speakerImageView.setSelected(true);
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
        if (userId.equals(ChatManager.Instance().getUserId())) {
            myParticipantItemView.updateVolume(volume);
        } else {
            if (focusProfile != null && focusProfile.getUserId().equals(userId)) {
                focusParticipantItemView.updateVolume(volume);
            }
        }
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
            if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                myParticipantItemView = conferenceItem;
            } else {
                focusParticipantItemView = conferenceItem;
            }

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
            muteVideoImageView.setSelected(myProfile.isAudience() || myProfile.isVideoMuted());
            muteAudioImageView.setSelected(myProfile.isAudience() || myProfile.isAudioMuted());
            micImageView.setMuted(myProfile.isAudience() || myProfile.isAudioMuted());
        });
    }

    @OnClick(R2.id.speakerImageView)
    void switchSpeaker() {
        AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
        AVAudioManager.AudioDevice selectedAudioDevice = audioManager.getSelectedAudioDevice();
        if (selectedAudioDevice == AVAudioManager.AudioDevice.BLUETOOTH) {
            return;
        }
        speakerImageView.setSelected(selectedAudioDevice == AVAudioManager.AudioDevice.EARPIECE);
        audioManager.setDefaultAudioDevice(selectedAudioDevice == AVAudioManager.AudioDevice.EARPIECE ? AVAudioManager.AudioDevice.SPEAKER_PHONE : AVAudioManager.AudioDevice.EARPIECE);
    }

//    @OnClick(R2.id.minimizeImageView)
//    void minimize() {
////        ((ConferenceActivity) getActivity()).showFloatingView(focusVideoUserId);
//        // VoipBaseActivity#onStop会处理，这儿仅仅finish
//        ((Activity) getContext()).finish();
//    }

    @OnClick(R2.id.manageParticipantView)
    void addParticipant() {
        ((ConferenceActivity) getContext()).showParticipantList();
    }

    @OnClick({R2.id.muteView, R2.id.micLinearLayout})
    void muteAudio() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        if (!session.isAudience() && !session.isAudioMuted()) {
            muteAudioImageView.setSelected(true);
            micImageView.setMuted(true);
            ConferenceManager.getManager().muteAudio(true);
            startHideBarTimer();
        } else {
            ConferenceInfo conferenceInfo = ConferenceManager.getManager().getCurrentConferenceInfo();
            if (conferenceInfo.isAllowTurnOnMic() || conferenceInfo.getOwner().equals(ChatManager.Instance().getUserId())) {
                boolean toMute = !session.isAudioMuted();
                muteAudioImageView.setSelected(toMute);
                micImageView.setMuted(toMute);
                ConferenceManager.getManager().muteAudio(toMute);
                startHideBarTimer();
            } else {
                if (ConferenceManager.getManager().isApplyingUnmute()) {
                    new MaterialDialog.Builder(getContext())
                        .content("主持人不允许解除静音，您已经申请解除静音，正在等待主持人操作")
                        .negativeText("取消申请")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Toast.makeText(getContext(), "已取消申请", Toast.LENGTH_SHORT).show();
                                ConferenceManager.getManager().applyUnmute(true);
                            }

                        })
                        .positiveText("继续申请")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Toast.makeText(getContext(), "已重新发送申请，请耐心等待主持人操作", Toast.LENGTH_SHORT).show();
                                ConferenceManager.getManager().applyUnmute(false);
                            }

                        })
                        .cancelable(false)
                        .build()
                        .show();
                } else {
                    new MaterialDialog.Builder(getContext())
                        .content("主持人不允许解除静音，您可以向主持人申请解除静音")
                        .negativeText("取消")
                        .onNegative((dialog, which) -> {

                        })
                        .positiveText("申请解除静音")
                        .onPositive((dialog, which) -> {
                            Toast.makeText(getContext(), "已重新发送申请，请耐心等待主持人操作", Toast.LENGTH_SHORT).show();
                            ConferenceManager.getManager().applyUnmute(false);
                        })
                        .cancelable(false)
                        .build()
                        .show();

                }
            }
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
        // TODO 参考 muteAudio 处理
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            boolean toMute = !session.videoMuted;
            muteVideoImageView.setSelected(toMute);
            ConferenceManager.getManager().muteVideo(toMute);
            startHideBarTimer();
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            if (ChatManager.Instance().getUserId().equals(session.getHost())) {
                new AlertDialog.Builder(getContext())
                    .setMessage("请选择是否结束会议")
                    .setIcon(R.mipmap.ic_launcher)
                    .setNeutralButton("退出会议", (dialogInterface, i) -> {
                        ConferenceManager.getManager().setCurrentConferenceInfo(null);
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(false);
                    })
                    .setPositiveButton("结束会议", (dialogInterface, i) -> {
                        ConferenceManager.getManager().setCurrentConferenceInfo(null);
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

    @OnClick(R2.id.moreActionLinearLayout)
    void showMoreActionDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_action_more, null);
        ImageView handUpImageView = view.findViewById(R.id.handUpImageView);
        TextView handUpTextView = view.findViewById(R.id.handUpTextView);
        handUpImageView.setSelected(ConferenceManager.getManager().isHandUp());
        handUpTextView.setText(ConferenceManager.getManager().isHandUp() ? "放下" : "举手");

        view.findViewById(R.id.inviteLinearLayout).setOnClickListener(v -> {
            ConferenceActivity activity = (ConferenceActivity) getContext();
            activity.inviteNewParticipant();
            dialog.dismiss();
        });
        view.findViewById(R.id.chatLinearLayout).setOnClickListener(v -> {
            Conversation conversation = new Conversation(Conversation.ConversationType.ChatRoom, callSession.getCallId(), 0);
            Intent intent = new Intent(getContext(), ConversationActivity.class);
            intent.putExtra("conversation", conversation);
            getContext().startActivity(intent);
            dialog.dismiss();
        });
        view.findViewById(R.id.handupLinearLayout).setOnClickListener(v -> {
            ConferenceManager.getManager().handUp(!handUpImageView.isSelected());
            handUpImageView.setSelected(!handUpImageView.isSelected());
            dialog.dismiss();
        });
        view.findViewById(R.id.minimizeLinearLayout).setOnClickListener(v -> {
            Activity activity = (Activity) getContext();
            activity.finish();
            dialog.dismiss();
        });
        view.findViewById(R.id.recordLinearLayout).setOnClickListener(v -> {
            // TODO
            dialog.dismiss();
        });
        view.findViewById(R.id.settingLinearLayout).setOnClickListener(v -> {
            // TODO
            dialog.dismiss();
        });
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    public void onPageUnselected() {
        if (focusProfile != null) {
            callSession.setParticipantVideoType(focusProfile.getUserId(), focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
        }
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
            micLinearLayout.setVisibility(GONE);
        } else {
//            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
            micLinearLayout.setVisibility(VISIBLE);

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
