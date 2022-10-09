/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.webrtc.StatsReport;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.ClickableViewPager;
import cn.wildfirechat.avenginekit.AVAudioManager;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;
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

    private SparseArray<View> views;
    private ViewPager viewPager;
    private List<AVEngineKit.ParticipantProfile> profiles;
    private ConferenceViewAdapter adapter;
    private AVEngineKit.CallSession callSession;
    private int currentPosition = -1;
    private static final String TAG = "conferenceFragment";

    /**
     * 这个值需要和{@link ConferenceParticipantGridView} 的布局里面的  row * col 对应上
     */
    private static final int COUNT_PER_PAGE = 4;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference, container, false);
        ButterKnife.bind(this, view);

        callSession = AVEngineKit.Instance().getCurrentSession();
        if (callSession == null || callSession.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return null;
        }
        profiles = getAllProfiles();

        views = new SparseArray<>(3);
        viewPager = view.findViewById(R.id.viewPager);
        adapter = new ConferenceViewAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);
        viewPager.addOnPageChangeListener(pageChangeListener);
        viewPager.setOnClickListener(clickListener);
        ((ClickableViewPager) viewPager).setOnViewPagerClickListener(new ClickableViewPager.OnClickListener() {
            @Override
            public void onViewPagerClick(ViewPager viewPager) {
                clickListener.onClick(viewPager);
            }
        });

        // 禁用自动设置 surfaceView 层级关系
        AVEngineKit.DISABLE_SURFACE_VIEW_AUTO_OVERLAY = true;
        callSession.autoSwitchVideoType = false;

        speakerImageView.setSelected(true);
        titleTextView.setText(this.callSession.getTitle());


        handler.post(() -> {
            AVEngineKit.ParticipantProfile myProfile = callSession.getMyProfile();
            muteVideoImageView.setSelected(myProfile.isAudience() || myProfile.isVideoMuted());
            muteAudioImageView.setSelected(myProfile.isAudience() || myProfile.isAudioMuted());
            micImageView.setMuted(myProfile.isAudience() || myProfile.isAudioMuted());
        });
        handler.post(updateCallDurationRunnable);

        return view;
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
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
                setPanelVisibility(GONE);
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
            AVEngineKit.CallSession session = callSession;
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

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private class ConferenceViewAdapter extends PagerAdapter {

        public ConferenceViewAdapter() {
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = null;
            if (position == 0) {
                view = new ConferenceMainView(container.getContext());
            } else {
                view = new ConferenceParticipantGridView(container.getContext());
            }
            container.addView(view);
            views.put(position % 3, view);
            Log.d(TAG, "instantiateItem " + position);
            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, @NonNull Object object) {
            // 取消视频流订阅
            container.removeView((View) object);
            Log.d(TAG, "destroyItem " + position);
            View view = (View) object;
            if (view instanceof ConferenceMainView) {

            } else if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).onDestroyView();
            }
        }

        @Override
        public int getCount() {
            Log.d(TAG, "getCount " + profiles.size());
            if (profiles.size() <= 2) {
                ((ClickableViewPager) viewPager).setPagingEnabled(false);
                return 1;
            }
            ((ClickableViewPager) viewPager).setPagingEnabled(true);
            return 1 + (int) Math.ceil(profiles.size() / (double) COUNT_PER_PAGE);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @Override
        public void finishUpdate(@NonNull ViewGroup container) {
            super.finishUpdate(container);
        }
    }

    final ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
        private Boolean first = true;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (first && positionOffset == 0 && positionOffsetPixels == 0) {
                onPageSelected(0);
                first = false;
            }
        }

        @Override
        public void onPageSelected(int position) {
            Log.e(TAG, " onPageSelected " + position);
            View view = views.get(position % 3);
            if (view == null) {
                // pending layout
                return;
            }

            // TODO 优化从第 0-> 1 或者 1-> 0时，在两页都会显示的视频流，不用取消订阅
            // 比如第 0 页显示的大流，刚好也需要显示在第 1 页时，就不用取消订阅
            if (currentPosition != -1) {
                view = views.get(currentPosition % 3);
                if (view instanceof ConferenceParticipantGridView) {
                    ((ConferenceParticipantGridView) view).onPageUnselected();
                } else if (view instanceof ConferenceMainView) {
                    ((ConferenceMainView) view).onPageUnselected();
                }
            }

            view = views.get(position % 3);
            if (view instanceof ConferenceParticipantGridView) {
                ((ConferenceParticipantGridView) view).setParticipantProfiles(callSession, getGridPageParticipantProfiles(position));
            } else if (view instanceof ConferenceMainView) {
                AVEngineKit.ParticipantProfile myProfile = callSession.getMyProfile();
                ((ConferenceMainView) view).setup(callSession, myProfile, findFocusProfile());
                ((ConferenceMainView) view).setup(callSession, myProfile, findFocusProfile());
            }

            currentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    private void onParticipantProfileUpdate(List<String> participants) {
        // 比如有人离开后
        if (adapter.getCount() <= currentPosition) {
            viewPager.setCurrentItem(currentPosition - 1);
        } else {
            View view = views.get(currentPosition % 3);
            if (view instanceof ConferenceMainView) {
                AVEngineKit.ParticipantProfile focusProfile = findFocusProfile();
                if (participants.contains(ChatManager.Instance().getUserId()) || focusProfile == null || participants.contains(focusProfile.getUserId())) {
                    ((ConferenceMainView) view).setup(this.callSession, callSession.getMyProfile(), focusProfile);
                }
            } else if (view instanceof ConferenceParticipantGridView) {
                List<AVEngineKit.ParticipantProfile> currentPageParticipantProfiles = getGridPageParticipantProfiles(currentPosition);
                boolean updateCurrentPage = false;
                for (String userId : participants) {
                    for (AVEngineKit.ParticipantProfile p : currentPageParticipantProfiles) {
                        if (userId.equals(p.getUserId())) {
                            updateCurrentPage = true;
                            break;
                        }
                    }
                }
                if (updateCurrentPage) {
                    ((ConferenceParticipantGridView) view).setParticipantProfiles(this.callSession, currentPageParticipantProfiles);
                }
            }
        }
    }

    private List<AVEngineKit.ParticipantProfile> getGridPageParticipantProfiles(int position) {
        int fromIndex = (position - 1) * COUNT_PER_PAGE;
        int endIndex = Math.min(fromIndex + COUNT_PER_PAGE, profiles.size());
        return profiles.subList(fromIndex, endIndex);
    }

    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason reason) {

    }

    @Override
    public void didChangeState(AVEngineKit.CallState state) {

    }

    @Override
    public void didParticipantJoined(String userId, boolean screenSharing) {
        this.profiles = getAllProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didParticipantJoined " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didParticipantConnected(String userId, boolean screenSharing) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason reason, boolean screenSharing) {
        this.profiles = getAllProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didParticipantLeft " + userId);
        LiveDataBus.setValue("kConferenceMemberChanged", new Object());
    }

    @Override
    public void didChangeType(String userId, boolean audience, boolean screenSharing) {
        this.profiles = getAllProfiles();
        this.adapter.notifyDataSetChanged();
        onParticipantProfileUpdate(Collections.singletonList(userId));
        Log.d(TAG, "didChangeType " + userId + " " + audience);
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {

    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId, boolean screenSharing) {

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
    public void didMuteStateChanged(List<String> participants) {
        this.profiles = getAllProfiles();
        onParticipantProfileUpdate(participants);
        LiveDataBus.setValue("kConferenceMutedStateChanged", new Object());
    }

    @Override
    public void didMediaLostPacket(String media, int lostPacket, boolean screenSharing) {
    }

    @Override
    public void didMediaLostPacket(String userId, String media, int lostPacket, boolean uplink, boolean screenSharing) {
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
//        Log.d(TAG, "didReportAudioVolume " + userId + " " + volume);
        View view = views.get(currentPosition % 3);
        if (view instanceof ConferenceMainView) {
            ((ConferenceMainView) view).updateParticipantVolume(userId, volume);
        } else if (view instanceof ConferenceParticipantGridView) {
            ((ConferenceParticipantGridView) view).updateParticipantVolume(userId, volume);
        }
    }

    @Override
    public void didAudioDeviceChanged(AVAudioManager.AudioDevice device) {

    }

    private List<AVEngineKit.ParticipantProfile> getAllProfiles() {
        List<AVEngineKit.ParticipantProfile> profiles = callSession.getParticipantProfiles();
        profiles.add(0, callSession.getMyProfile());
        return profiles;
    }

    private AVEngineKit.ParticipantProfile findFocusProfile() {
        AVEngineKit.ParticipantProfile focusProfile = null;
        for (AVEngineKit.ParticipantProfile profile : profiles) {
            if (!profile.isAudience() && !profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                if (profile.isScreenSharing()) {
                    focusProfile = profile;
                    break;
                } else if (!profile.isVideoMuted() && (focusProfile == null || focusProfile.isVideoMuted())) {
                    focusProfile = profile;
                } else if (!profile.isAudioMuted() && focusProfile == null) {
                    focusProfile = profile;
                }
            }
        }
        return focusProfile;
    }
}
