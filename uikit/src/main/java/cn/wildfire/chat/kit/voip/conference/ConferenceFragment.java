/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import org.webrtc.StatsReport;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceCommandContent;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.ClickableViewPager;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

// main view
// participantPreviewView

// 收到事件之后，判断是否影响当前页，不影响的话，就不做任何处理
// 切换页面时，初始化新切换到的页面
// 收到事件之后，可能需要增减页面

// 当前页面包含哪些人，只针对这些人的事件做处理

// 收到离开事件时，1. 当前页面处理，补位；2. 判断是否要减页面
// 收到加入事件时，1. 当前页面是否还能加位置；2. 判断是否需要增加页面

// 离开页面之后，反初始化

//  收到事件时，3 个页面(当前页，前后各一页)需根据实际情况处理

// 离开时，先更新 view，在删除

public class ConferenceFragment extends BaseConferenceFragment implements AVEngineKit.CallSessionCallback {

    FrameLayout rootFrameLayout;

    LinearLayout topBarView;

    FrameLayout bottomPanel;

    TextView titleTextView;
    TextView durationTextView;

    TextView manageParticipantTextView;

    ImageView muteAudioImageView;
    ImageView muteVideoImageView;
    ImageView shareScreenImageView;

    ImageView speakerImageView;

    LinearLayout micLinearLayout;
    MicImageView micImageView;

    WormDotsIndicator dotsIndicator;

    private SparseArray<View> conferencePages;
    private ViewPager viewPager;
    // 包含自己
    private List<AVEngineKit.ParticipantProfile> profiles;
    private AVEngineKit.ParticipantProfile myProfile;
    private PagerAdapter pagerAdapter;
    private AVEngineKit.CallSession callSession;
    private int currentPosition = -1;
    private boolean fixPreviewSurfaceViewZOrder = false;
    private boolean selectFirstPage = true;
    private static final String TAG = "conferenceFragment";

    /**
     * 这个值需要和{@link ConferenceParticipantGridView} 的布局里面的  row * col 对应上
     */
    private static final int VIDEO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE = 4;
    private static final int AUDIO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE = 12;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference, container, false);
        bindViews(view);
        bindEvents(view);

        callSession = AVEngineKit.Instance().getCurrentSession();
        if (callSession == null || callSession.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return null;
        }
        profiles = loadAllProfiles();

        conferencePages = new SparseArray<>(3);
        viewPager = view.findViewById(R.id.viewPager);
        if (isVideoConference()) {
            pagerAdapter = new VideoConferencePageAdapter();
        } else {
            pagerAdapter = new AudioConferencePageAdapter();
        }
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(conferencePageChangeListener);
        viewPager.setOnClickListener(clickListener);
        dotsIndicator.setViewPager(viewPager);
        ((ClickableViewPager) viewPager).setOnViewPagerClickListener(new ClickableViewPager.OnClickListener() {
            @Override
            public void onViewPagerClick(ViewPager viewPager) {
                clickListener.onClick(viewPager);
            }
        });

        // 禁用自动设置 surfaceView 层级关系
        AVEngineKit.DISABLE_SURFACE_VIEW_AUTO_OVERLAY = true;
        AVEngineKit.ENABLE_PROXIMITY_SENSOR_ADJUST_AUDIO_OUTPUT_DEVICE = false;
        callSession.autoSwitchVideoType = false;
        callSession.defaultVideoType = AVEngineKit.VideoType.VIDEO_TYPE_NONE;

        speakerImageView.setSelected(true);
        titleTextView.setText(this.callSession.getTitle());

        LiveDataBus.subscribe("kConferenceCommandStateChanged", this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                if (o instanceof ConferenceCommandContent) {
                    ConferenceCommandContent commandContent = ((ConferenceCommandContent) o);
                    if ((commandContent.getCommandType() == ConferenceCommandContent.ConferenceCommandType.FOCUS || commandContent.getCommandType() == ConferenceCommandContent.ConferenceCommandType.CANCEL_FOCUS)
                        && !commandContent.getTargetUserId().equals(ChatManager.Instance().getUserId())) {
                        profiles = loadAllProfiles();
                        AVEngineKit.ParticipantProfile focusProfile = findFocusProfile();
                        onParticipantProfileUpdate(Collections.singletonList(focusProfile.getUserId()));
                    }
                }
            }
        });

        handler.post(() -> {
            AVEngineKit.ParticipantProfile myProfile = callSession.getMyProfile();
            muteVideoImageView.setSelected(myProfile.isAudience() || myProfile.isVideoMuted());
            muteAudioImageView.setSelected(myProfile.isAudience() || myProfile.isAudioMuted());
            micImageView.setMuted(myProfile.isAudience() || myProfile.isAudioMuted());
        });

        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.speakerImageView).setOnClickListener(_v -> switchSpeaker());
        view.findViewById(R.id.manageParticipantView).setOnClickListener(_v -> addParticipant());
        view.findViewById(R.id.muteView).setOnClickListener(_v -> muteAudio());
        view.findViewById(R.id.micLinearLayout).setOnClickListener(_v -> muteAudio());
        view.findViewById(R.id.videoView).setOnClickListener(_v -> muteVideo());
        view.findViewById(R.id.switchCameraImageView).setOnClickListener(_v -> switchCamera());
        view.findViewById(R.id.hangupImageView).setOnClickListener(_v -> hangup());
        view.findViewById(R.id.shareScreenView).setOnClickListener(_v -> shareScreen());
        view.findViewById(R.id.titleLinearLayout).setOnClickListener(_v -> showConferenceInfoDialog());
        view.findViewById(R.id.moreActionLinearLayout).setOnClickListener(_v -> showMoreActionDialog());
    }

    private void bindViews(View view) {
        rootFrameLayout = view.findViewById(R.id.rootFrameLayout);
        topBarView = view.findViewById(R.id.topBarView);
        bottomPanel = view.findViewById(R.id.bottomPanel);
        titleTextView = view.findViewById(R.id.titleTextView);
        durationTextView = view.findViewById(R.id.durationTextView);
        manageParticipantTextView = view.findViewById(R.id.manageParticipantTextView);
        muteAudioImageView = view.findViewById(R.id.muteImageView);
        muteVideoImageView = view.findViewById(R.id.videoImageView);
        shareScreenImageView = view.findViewById(R.id.shareScreenImageView);
        speakerImageView = view.findViewById(R.id.speakerImageView);
        micLinearLayout = view.findViewById(R.id.micLinearLayout);
        micImageView = view.findViewById(R.id.micImageView);
        dotsIndicator = view.findViewById(R.id.dotsIndicator);
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(updateCallDurationRunnable);
        startHideBarTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(updateCallDurationRunnable);
        handler.removeCallbacks(hideBarCallback);
    }

    void switchSpeaker() {
        AVAudioManager audioManager = AVEngineKit.Instance().getAVAudioManager();
        AVAudioManager.AudioDevice selectedAudioDevice = audioManager.getSelectedAudioDevice();
        if (selectedAudioDevice == AVAudioManager.AudioDevice.BLUETOOTH) {
            return;
        }
        speakerImageView.setSelected(selectedAudioDevice == AVAudioManager.AudioDevice.EARPIECE);
        audioManager.setDefaultAudioDevice(selectedAudioDevice == AVAudioManager.AudioDevice.EARPIECE ? AVAudioManager.AudioDevice.SPEAKER_PHONE : AVAudioManager.AudioDevice.EARPIECE);
    }

    void addParticipant() {
        ((ConferenceActivity) getContext()).showParticipantList();
    }

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
                requestUnmute(true);
            }
        }
    }

    void muteVideo() {
        AVEngineKit.CallSession session = callSession;
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            return;
        }
        if (session.isAudioOnly()) {
            Toast.makeText(getActivity(), getString(R.string.conf_audio_no_camera), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!session.isAudience() && !session.videoMuted) {
            muteVideoImageView.setSelected(true);
            ConferenceManager.getManager().muteVideo(true);
            startHideBarTimer();
        } else {
            ConferenceInfo conferenceInfo = ConferenceManager.getManager().getCurrentConferenceInfo();
            if (!session.isAudience() || conferenceInfo.isAllowTurnOnMic() || conferenceInfo.getOwner().equals(ChatManager.Instance().getUserId())) {
                muteVideoImageView.setSelected(false);
                ConferenceManager.getManager().muteVideo(false);
                startHideBarTimer();
            } else {
                requestUnmute(false);
            }
        }
    }

    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
            startHideBarTimer();
        }
    }

    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        ConferenceManager conferenceManager = ConferenceManager.getManager();
        ConferenceInfo conferenceInfo = conferenceManager.getCurrentConferenceInfo();
        if (session != null) {
            if (ChatManager.Instance().getUserId().equals(conferenceInfo.getOwner())) {
                new MaterialDialog.Builder(getContext())
                    .content(R.string.conf_leave_allow_continue_prompt)
                    .negativeText(R.string.conf_leave_conference)
                    .onNegative((dialogInterface, i) -> {

                        conferenceManager.addHistory(conferenceInfo, ChatManager.Instance().getServerTimestamp() - session.getStartTime());
                        conferenceManager.setCurrentConferenceInfo(null);
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(false);
                    })
                    .positiveText(R.string.conf_end_conference)
                    .onPositive((dialogInterface, i) -> {
                        conferenceManager.addHistory(conferenceInfo, ChatManager.Instance().getServerTimestamp() - session.getStartTime());
                        conferenceManager.destroyConference(session.getCallId(), null);
                        conferenceManager.setCurrentConferenceInfo(null);
                        if (session.getState() != AVEngineKit.CallState.Idle)
                            session.leaveConference(true);
                    })
                    .build()
                    .show();
            } else {
                session.leaveConference(false);
            }
        } else {
            Activity activity = getActivity();
            if(activity != null && !activity.isFinishing()){
                activity.finish();
            }
        }
    }

    void shareScreen() {
        VoipBaseActivity voipBaseActivity = ((VoipBaseActivity) getActivity());
        if (!voipBaseActivity.checkOverlayPermission()) {
            return;
        }

        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            if (session.isAudioOnly()) {
                Toast.makeText(voipBaseActivity, getString(R.string.conf_audio_no_screen_share), Toast.LENGTH_SHORT).show();
                return;
            }
            if (!session.isScreenSharing()) {
                Toast.makeText(getContext(), getString(R.string.conf_screen_share_hint), Toast.LENGTH_LONG).show();
                session.muteAudio(false);
                if(AVEngineKit.SCREEN_SHARING_REPLACE_MODE){
                    session.muteVideo(true);
                }

                ((VoipBaseActivity) getContext()).startScreenShare();
                if (session.isAudience()) {
                    session.switchAudience(false);
                }
            } else {
                ((VoipBaseActivity) getContext()).stopScreenShare();
            }
        }
    }

    void showConferenceInfoDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_info_dialog, null);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView conferenceIdTextView = view.findViewById(R.id.conferenceIdTextView);
        TextView conferenceHostTextView = view.findViewById(R.id.hostTextView);
        TextView conferenceLinkTextView = view.findViewById(R.id.conferenceLinkTextView);

        titleTextView.setText(callSession.getTitle());
        conferenceIdTextView.setText(callSession.getCallId());
        UserInfo userInfo = ChatManager.Instance().getUserInfo(ConferenceManager.getManager().getCurrentConferenceInfo().getOwner(), false);
        conferenceHostTextView.setText(userInfo.displayName);
        String conferenceLink = WfcScheme.buildConferenceScheme(callSession.getCallId(), callSession.getPin());
        conferenceLinkTextView.setText(conferenceLink);

        view.findViewById(R.id.copyCallIdImageView).setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                return;
            }
            ClipData clipData = ClipData.newPlainText("conferenceId", callSession.getCallId());
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        view.findViewById(R.id.copyLinkImageView).setOnClickListener(v -> {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager == null) {
                return;
            }
            ClipData clipData = ClipData.newPlainText("conferenceLink", conferenceLink);
            clipboardManager.setPrimaryClip(clipData);
            Toast.makeText(getContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    void showMoreActionDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_action_more, null);
        ConferenceManager conferenceManager = ConferenceManager.getManager();
        ConferenceInfo conferenceInfo = conferenceManager.getCurrentConferenceInfo();

        ImageView recordImageView = view.findViewById(R.id.recordImageView);
        recordImageView.setSelected(conferenceInfo.isRecording());

        ImageView handUpImageView = view.findViewById(R.id.handUpImageView);
        TextView handUpTextView = view.findViewById(R.id.handUpTextView);
        handUpImageView.setSelected(conferenceManager.isHandUp());
        handUpTextView.setText(conferenceManager.isHandUp() ? getString(R.string.conf_hand_down) : getString(R.string.conf_hand_up));

        view.findViewById(R.id.inviteLinearLayout).setOnClickListener(v -> {
            ConferenceActivity activity = (ConferenceActivity) getContext();
            activity.inviteNewParticipant();
            dialog.dismiss();
        });
        view.findViewById(R.id.chatLinearLayout).setOnClickListener(v -> {
            Intent intent = ConversationActivity.buildChatRoomConversationIntent(getContext(), callSession.getCallId(), 0, callSession.getTitle(), true);
            getContext().startActivity(intent);
            dialog.dismiss();
        });
        view.findViewById(R.id.handupLinearLayout).setOnClickListener(v -> {
            conferenceManager.handUp(!handUpImageView.isSelected());
            handUpImageView.setSelected(!handUpImageView.isSelected());
            dialog.dismiss();
        });
        view.findViewById(R.id.minimizeLinearLayout).setOnClickListener(v -> {
            if (((VoipBaseActivity) getActivity()).checkOverlayPermission()) {
                Activity activity = (Activity) getContext();
                activity.finish();
                dialog.dismiss();
            }
        });
        view.findViewById(R.id.recordLinearLayout).setOnClickListener(v -> {
            if (ChatManager.Instance().getUserId().equals(conferenceInfo.getOwner())) {
                conferenceManager.requestRecord(!conferenceInfo.isRecording());
            } else {
                Toast.makeText(getActivity(), conferenceInfo.isRecording() ? getString(R.string.conf_recording_stop_owner) : getString(R.string.conf_recording_start_owner), Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        view.findViewById(R.id.settingLinearLayout).setOnClickListener(v -> {
            // TODO
            dialog.dismiss();
        });
        view.findViewById(R.id.cancelButton).setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.getBehavior().setState(STATE_EXPANDED);
        dialog.show();
    }


    private final View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bottomPanel.getVisibility() == GONE) {
                setPanelVisibility(VISIBLE);
                startHideBarTimer();
            } else {
                setPanelVisibility(GONE);
            }
        }
    };

    private void startHideBarTimer() {
        if (bottomPanel.getVisibility() == GONE) {
            return;
        }
        handler.removeCallbacks(hideBarCallback);
        handler.postDelayed(hideBarCallback, 3000);
    }

    private final Runnable hideBarCallback = new Runnable() {
        @Override
        public void run() {
            Activity activity = getActivity();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                setPanelVisibility(GONE);
            }
        }
    };

    private void requestUnmute(boolean audio) {
        if (audio) {
            if (ConferenceManager.getManager().isApplyingUnmuteAudio()) {
                new MaterialDialog.Builder(getContext())
                    .content(R.string.conf_unmute_request_in_progress_audio)
                    .negativeText(R.string.conf_cancel_request)
                    .onNegative((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_cancelled, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(true, true);
                    })
                    .positiveText(R.string.conf_continue_request)
                    .onPositive((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_resent, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(true, false);
                    })
                    .cancelable(false)
                    .build()
                    .show();
            } else {
                new MaterialDialog.Builder(getContext())
                    .content(R.string.conf_unmute_not_allowed_audio)
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, which) -> {})
                    .positiveText(R.string.conf_request_unmute_audio)
                    .onPositive((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_resent, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(true, false);
                    })
                    .cancelable(false)
                    .build()
                    .show();
            }
        } else {
            if (ConferenceManager.getManager().isApplyingUnmuteAudio()) {
                new MaterialDialog.Builder(getContext())
                    .content(R.string.conf_unmute_request_in_progress_video)
                    .negativeText(R.string.conf_cancel_request)
                    .onNegative((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_cancelled, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(false, true);
                    })
                    .positiveText(R.string.conf_continue_request)
                    .onPositive((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_resent, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(false, false);
                    })
                    .cancelable(false)
                    .build()
                    .show();
            } else {
                new MaterialDialog.Builder(getContext())
                    .content(R.string.conf_unmute_not_allowed_video)
                    .negativeText(R.string.cancel)
                    .onNegative((dialog, which) -> {})
                    .positiveText(R.string.conf_request_unmute_video)
                    .onPositive((dialog, which) -> {
                        Toast.makeText(getContext(), R.string.conf_request_resent, Toast.LENGTH_SHORT).show();
                        ConferenceManager.getManager().applyUnmute(false, false);
                    })
                    .cancelable(false)
                    .build()
                    .show();
            }
        }
    }

    private void setPanelVisibility(int visibility) {
        TransitionSet transitionSet = new TransitionSet();
        Transition transitionToBottom = new Slide(Gravity.BOTTOM);
        transitionToBottom.setDuration(300);
        transitionSet.addTransition(transitionToBottom);

        Transition transitionToTop = new Slide(Gravity.TOP);
        transitionToTop.setDuration(500);
        transitionSet.addTransition(transitionToTop);

        transitionToBottom.addTarget(bottomPanel);
        transitionToBottom.addTarget(micLinearLayout);
        transitionToTop.addTarget(topBarView);

        transitionSet.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {
//                Activity activity = ((Activity) getContext());
//                if (visibility == VISIBLE) {
//                    activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//                } else {
//                    activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
//                }
            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {

            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {

            }
        });

        transitionSet.setOrdering(TransitionSet.ORDERING_TOGETHER);
//        transitionSet.setInterpolator(new AccelerateDecelerateInterpolator());
        TransitionManager.beginDelayedTransition(rootFrameLayout, transitionSet);

        bottomPanel.setVisibility(visibility);
        topBarView.setVisibility(visibility);
        micLinearLayout.setVisibility(visibility == VISIBLE ? GONE : VISIBLE);
    }

    private final Handler handler = new Handler();

    private final Runnable updateCallDurationRunnable = new Runnable() {
        @Override
        public void run() {
            AVEngineKit.CallSession session = callSession;
            if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
                String text;
                if (session.getConnectedTime() == 0) {
                    text = "会议连接中";
                } else {
                    long s = ChatManager.Instance().getServerTimestamp() - session.getConnectedTime();
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

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private class VideoConferencePageAdapter extends PagerAdapter {

        public VideoConferencePageAdapter() {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = null;
            if (position == 0) {
                view = new VideoConferenceMainView(container.getContext());
                view.setOnClickListener(clickListener);
            } else {
                view = new ConferenceParticipantGridView(container.getContext());
            }
            container.addView(view);
            conferencePages.put(position % 3, view);
            Log.d(TAG, "instantiateItem " + position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            // 取消视频流订阅
            container.removeView((View) object);
            Log.d(TAG, "destroyItem " + position);
            View view = (View) object;
            if (view instanceof VideoConferenceMainView) {
                ((VideoConferenceMainView) view).onDestroyView();
            } else if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).onDestroyView();
            }
        }

        @Override
        public int getCount() {
            Log.d(TAG, "getCount " + profiles.size());
            int count;
            if (profiles.size() <= 2) {
                ((ClickableViewPager) viewPager).setPagingEnabled(false);
                count = 1;
            } else {
                ((ClickableViewPager) viewPager).setPagingEnabled(true);
                count = 1 + (int) Math.ceil(profiles.size() / (double) VIDEO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE);
            }
            dotsIndicator.setVisibility(count > 1 ? VISIBLE : GONE);
            return count;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    final ViewPager.OnPageChangeListener conferencePageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (selectFirstPage && positionOffset == 0 && positionOffsetPixels == 0) {
                onPageSelected(0);
                selectFirstPage = false;
            }
        }

        @Override
        public void onPageSelected(int position) {
            Log.e(TAG, " onPageSelected " + position);
            View view = conferencePages.get(position % 3);
            if (view == null) {
                // pending layout
                return;
            }

            boolean keepSubscribeFocusVideo = false;
            String keepSubscribeUserId = null;
            if ((currentPosition == 0 && position == 1)
                || (currentPosition == 1 && position == 0)) {
                List<AVEngineKit.ParticipantProfile> pageParticipantProfiles = getGridPageParticipantProfiles(1);
                AVEngineKit.ParticipantProfile focusProfile = findFocusProfile();
                for (AVEngineKit.ParticipantProfile p : pageParticipantProfiles) {
                    if (p.getUserId().equals(focusProfile.getUserId())) {
                        if (position == 1) {
                            keepSubscribeFocusVideo = true;
                        } else {
                            keepSubscribeUserId = focusProfile.getUserId();
                        }
                        break;
                    }
                }
                // 1 -> 0
            }

            // 比如第 0 页显示的大流，刚好也需要显示在第 1 页时，就不用取消订阅
            if (isVideoConference() && currentPosition != -1) {
                view = conferencePages.get(currentPosition % 3);
                if (view instanceof ConferenceParticipantGridView) {
                    ((ConferenceParticipantGridView) view).onPageUnselected(keepSubscribeUserId);
                } else if (view instanceof VideoConferenceMainView) {
                    ((VideoConferenceMainView) view).onPageUnselected(keepSubscribeFocusVideo);
                }
            }

            view = conferencePages.get(position % 3);
            if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).setParticipantProfiles(callSession, getGridPageParticipantProfiles(position));
            } else if (view instanceof VideoConferenceMainView) {
                AVEngineKit.ParticipantProfile myProfile = callSession.getMyProfile();
                ((VideoConferenceMainView) view).setupProfiles(callSession, myProfile, findFocusProfile());
            }

            currentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private class AudioConferencePageAdapter extends PagerAdapter {

        public AudioConferencePageAdapter() {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = new ConferenceParticipantGridView(container.getContext(), true);
            container.addView(view);
            conferencePages.put(position % 3, view);
            Log.d(TAG, "instantiateItem " + position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
            Log.d(TAG, "destroyItem " + position);
            View view = (View) object;
            if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).onDestroyView();
            }
        }

        @Override
        public int getCount() {
            Log.d(TAG, "getCount " + profiles.size());
            int count;
            if (profiles.size() <= AUDIO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE) {
                ((ClickableViewPager) viewPager).setPagingEnabled(false);
                count = 1;
            } else {
                ((ClickableViewPager) viewPager).setPagingEnabled(true);
                count = 1 + (int) Math.ceil(profiles.size() / (double) AUDIO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE);
            }
            dotsIndicator.setVisibility(count > 1 ? VISIBLE : GONE);
            return count;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {

    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {

    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        if (!resetConferencePageAdapter()) {
            this.pagerAdapter.notifyDataSetChanged();
            onParticipantProfileUpdate(Collections.singletonList(userId));
        }
        Log.d(TAG, "didParticipantJoined " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason reason, boolean screenSharing) {
        if (!resetConferencePageAdapter()) {
            this.pagerAdapter.notifyDataSetChanged();
            onParticipantProfileUpdate(Collections.singletonList(userId));
        }
        Log.d(TAG, "didParticipantLeft " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        Log.d(TAG, "didChangeType " + userId + " " + audience);
        if (!resetConferencePageAdapter()) {
            this.pagerAdapter.notifyDataSetChanged();
            onParticipantProfileUpdate(Collections.singletonList(userId));
        }
        if (userId.equals(ChatManager.Instance().getUserId())) {
            updateMuteState();
        }
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didMuteStateChanged(List<String> participants) {
        if (!resetConferencePageAdapter()) {
            onParticipantProfileUpdate(participants);
        }

        if (participants.contains(ChatManager.Instance().getUserId())) {
            updateMuteState();
        }
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
//        Log.d(TAG, "didReportAudioVolume " + userId + " " + volume);
        if (currentPosition == -1) {
            return;
        }
        View view = conferencePages.get(currentPosition % 3);
        if (view instanceof VideoConferenceMainView) {
            ((VideoConferenceMainView) view).updateParticipantVolume(userId, volume);
        } else if (view instanceof ConferenceParticipantGridView) {
            ((ConferenceParticipantGridView) view).updateParticipantVolume(userId, volume);
        }
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {
        // 预览视频流可能会被焦点视频流覆盖，采用下面的方法修复
        if (currentPosition == 0 && !fixPreviewSurfaceViewZOrder) {
            Object obj = conferencePages.get(0);
            if (!(obj instanceof VideoConferenceMainView)) {
                return;
            }
            VideoConferenceMainView mainView = (VideoConferenceMainView) conferencePages.get(0);
            SurfaceView previewSurfaceView = mainView.findViewWithTag("sv_" + ChatManager.Instance().getUserId());
            if (previewSurfaceView != null) {
                previewSurfaceView.setZOrderMediaOverlay(true);
                // 触发一次界面重绘，不然不生效
                clickListener.onClick(mainView);
                fixPreviewSurfaceViewZOrder = true;
            }
        }
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {

    }

    @Override
    public void didError(String error) {

    }

    @Override
    public void didGetStats(StatsReport[] reports) {

    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {
    }

    @Override
    public void didMediaLostPacket(String media, int lostPacket, boolean screenSharing) {
    }

    @Override
    public void didMediaLostPacket(String userId, String media, int lostPacket, boolean uplink, boolean screenSharing) {
    }


    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {
        Log.d(TAG, "didAudioDeviceChanged " + device.name());
    }

    private void updateMuteState() {
        muteVideoImageView.setSelected(myProfile.isAudience() || myProfile.isVideoMuted());
        muteAudioImageView.setSelected(myProfile.isAudience() || myProfile.isAudioMuted());
        micImageView.setMuted(myProfile.isAudience() || myProfile.isAudioMuted());
    }

    private boolean resetConferencePageAdapter() {
        boolean lastIsVideoConference = this.isVideoConference();
        this.profiles = loadAllProfiles();
        boolean isVideoConference = this.isVideoConference();
        if (lastIsVideoConference != isVideoConference) {
            if (isVideoConference) {
                pagerAdapter = new VideoConferencePageAdapter();
            } else {
                pagerAdapter = new AudioConferencePageAdapter();
            }
            viewPager.setAdapter(pagerAdapter);
            dotsIndicator.setViewPager(viewPager);
            selectFirstPage = true;
            currentPosition = -1;
            return true;
        }
        return false;
    }

    private void onParticipantProfileUpdate(List<String> participants) {
        if (currentPosition == -1) {
            return;
        }
        if (pagerAdapter.getCount() <= currentPosition) {
            viewPager.setCurrentItem(currentPosition - 1);
        } else {
            View view = conferencePages.get(currentPosition % 3);
            if (view instanceof VideoConferenceMainView) {
                AVEngineKit.ParticipantProfile focusProfile = findFocusProfile();
                ((VideoConferenceMainView) view).setupProfiles(this.callSession, callSession.getMyProfile(), focusProfile);
            } else if (view instanceof ConferenceParticipantGridView) {
                List<AVEngineKit.ParticipantProfile> currentPageParticipantProfiles = getGridPageParticipantProfiles(currentPosition);
                boolean currentPageUpdated = false;
                for (String userId : participants) {
                    for (AVEngineKit.ParticipantProfile p : currentPageParticipantProfiles) {
                        if (userId.equals(p.getUserId())) {
                            currentPageUpdated = true;
                            break;
                        }
                    }
                }

                // 当前页的用户离开会议时
                if (!currentPageUpdated && currentPageParticipantProfiles.size() < AUDIO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE) {
                    currentPageUpdated = true;
                }

                if (currentPageUpdated) {
                    ((ConferenceParticipantGridView) view).setParticipantProfiles(this.callSession, currentPageParticipantProfiles);
                }
            }
        }
    }

    private List<AVEngineKit.ParticipantProfile> getGridPageParticipantProfiles(int position) {
        int countPerPage;
        int fromIndex;
        if (this.isVideoConference()) {
            countPerPage = VIDEO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE;
            fromIndex = (position - 1) * countPerPage;
        } else {
            countPerPage = AUDIO_CONFERENCE_PARTICIPANT_COUNT_PER_PAGE;
            fromIndex = position * countPerPage;
        }
        int endIndex = Math.min(fromIndex + countPerPage, profiles.size());
        return profiles.subList(fromIndex, endIndex);
    }


    private List<AVEngineKit.ParticipantProfile> loadAllProfiles() {
        List<AVEngineKit.ParticipantProfile> profiles = callSession.getParticipantProfiles();
        myProfile = callSession.getMyProfile();
        profiles.add(0, myProfile);
        String focusUserId = ConferenceManager.getManager().getCurrentConferenceInfo().getFocus();
        AVEngineKit.ParticipantProfile focusUserProfile = focusUserId == null ? null : callSession.getParticipantProfile(focusUserId, true);
        if (focusUserProfile == null) {
            focusUserProfile = focusUserId == null ? null : callSession.getParticipantProfile(focusUserId, false);
        }

        AVEngineKit.ParticipantProfile finalFocusUserProfile = focusUserProfile;
        Collections.sort(profiles, new Comparator<AVEngineKit.ParticipantProfile>() {
            @Override
            public int compare(AVEngineKit.ParticipantProfile o1, AVEngineKit.ParticipantProfile o2) {
                if (finalFocusUserProfile != null) {
                    if (o1.getUserId().equals(focusUserId) && o1.isScreenSharing() == finalFocusUserProfile.isScreenSharing()) {
                        return -1;
                    }
                    if (o2.getUserId().equals(focusUserId) && o2.isScreenSharing() == finalFocusUserProfile.isScreenSharing()) {
                        return 1;
                    }
                }

                if (o1.isAudience() && !o2.isAudience()) {
                    return 1;
                } else if (!o1.isAudience() && o2.isAudience()) {
                    return -1;
                } else if (o1.isAudience() && o2.isAudience()) {
                    return o1.getUserId().compareTo(o2.getUserId());
                } else {
                    if (o1.isScreenSharing() && !o2.isScreenSharing()) {
                        return -1;
                    }
                    if (!o1.isScreenSharing() && o2.isScreenSharing()) {
                        return 1;
                    }
                    if (o1.isVideoMuted() && !o2.isVideoMuted()) {
                        return 1;
                    }
                    if (!o1.isVideoMuted() && o2.isVideoMuted()) {
                        return -1;
                    }
                    return o1.getUserId().compareTo(o2.getUserId());
                }
            }
        });
        return profiles;
    }

    private boolean isVideoConference() {
        for (AVEngineKit.ParticipantProfile p : profiles) {
            if (!p.isAudience() && !p.isVideoMuted()) {
                return true;
            }
        }
        return false;
    }

    private AVEngineKit.ParticipantProfile findFocusProfile() {
        // 已经排序了
        AVEngineKit.ParticipantProfile focusProfile = profiles.get(0);
        if (focusProfile.getUserId().equals(ChatManager.Instance().getUserId()) && profiles.size() > 1) {
            focusProfile = profiles.get(1);
        }
        Log.d(TAG, "findFocusProfile " + (focusProfile != null ? focusProfile.getUserId() : "null"));
        return focusProfile;
    }
}
