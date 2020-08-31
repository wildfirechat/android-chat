package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
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
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.lifecycle.ViewModelProviders;

import org.webrtc.RendererCommon;
import org.webrtc.StatsReport;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.conference.ConferenceActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceItem;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.PeerConnectionClient;
import cn.wildfirechat.model.UserInfo;

public class ConferenceVideoFragment extends Fragment implements AVEngineKit.CallSessionCallback {
    @BindView(R2.id.rootView)
    LinearLayout rootLinearLayout;
    @BindView(R2.id.durationTextView)
    TextView durationTextView;
    @BindView(R2.id.videoContainerGridLayout)
    GridLayout participantGridView;
    @BindView(R2.id.focusVideoContainerFrameLayout)
    FrameLayout focusVideoContainerFrameLayout;
    @BindView(R2.id.muteImageView)
    ImageView muteImageView;
    @BindView(R2.id.videoImageView)
    ImageView videoImageView;

    private List<String> participants;
    private UserInfo me;
    private UserViewModel userViewModel;

    private String focusVideoUserId;
    private ConferenceItem focusConferenceItem;

    private RendererCommon.ScalingType scalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED;
    private boolean micEnabled = true;
    private boolean videoEnabled = true;

    public static final String TAG = "ConferenceVideoFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_video_outgoing_connected, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private AVEngineKit getEngineKit() {
        return AVEngineKit.Instance();
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            getActivity().finish();
            return;
        }

        initParticipantsView(session);

        if (session.getState() == AVEngineKit.CallState.Connected) {
            session.startVideoSource();
            List<AVEngineKit.ParticipantProfile> profiles = session.getParticipantProfiles();
            for (AVEngineKit.ParticipantProfile profile : profiles) {
                if (profile.getState() == AVEngineKit.CallState.Connected) {
                    didReceiveRemoteVideoTrack(profile.getUserId());
                }
            }
            didCreateLocalVideoTrack();
        } else {
            if (session.isLocalVideoCreated()) {
                didCreateLocalVideoTrack();
            } else {
                session.startPreview();
            }
        }

        updateCallDuration();
        updateParticipantStatus(session);
        bringParticipantVideoFront();

        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(true);
    }

    private void initParticipantsView(AVEngineKit.CallSession session) {
        me = userViewModel.getUserInfo(userViewModel.getUserId(), false);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.removeAllViews();

        participants = session.getParticipantIds();
        if (participants != null && participants.size() > 0) {
            List<UserInfo> participantUserInfos = userViewModel.getUserInfos(participants);
            for (UserInfo userInfo : participantUserInfos) {
                ConferenceItem multiCallItem = new ConferenceItem(getActivity());
                multiCallItem.setTag(userInfo.uid);

                multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
                multiCallItem.getStatusTextView().setText(R.string.connecting);
                multiCallItem.setOnClickListener(clickListener);
                GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
                participantGridView.addView(multiCallItem);
            }
        }

        ConferenceItem multiCallItem = new ConferenceItem(getActivity());
        multiCallItem.setTag(me.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with, with));
        multiCallItem.getStatusTextView().setText(me.displayName);
        GlideApp.with(multiCallItem).load(me.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());

        focusVideoContainerFrameLayout.setLayoutParams(new FrameLayout.LayoutParams(with, with));
        focusVideoContainerFrameLayout.addView(multiCallItem);
        focusConferenceItem = multiCallItem;
        focusVideoUserId = me.uid;
    }

    private void updateParticipantStatus(AVEngineKit.CallSession session) {
        int count = participantGridView.getChildCount();
        String meUid = userViewModel.getUserId();
        for (int i = 0; i < count; i++) {
            View view = participantGridView.getChildAt(i);
            String userId = (String) view.getTag();
            if (meUid.equals(userId)) {
                ((ConferenceItem) view).getStatusTextView().setVisibility(View.GONE);
            } else {
                PeerConnectionClient client = session.getClient(userId);
                if (client.state == AVEngineKit.CallState.Connected) {
                    ((ConferenceItem) view).getStatusTextView().setVisibility(View.GONE);
                } else if (client.videoMuted) {
                    ((ConferenceItem) view).getStatusTextView().setText("关闭摄像头");
                    ((ConferenceItem) view).getStatusTextView().setVisibility(View.VISIBLE);
                }
            }
        }
    }


    @OnClick(R2.id.minimizeImageView)
    void minimize() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        session.stopVideoSource();
        List<String> participants = session.getParticipantIds();
        for (String participant : participants) {
            session.setupRemoteVideo(participant, null, scalingType);
        }
        ((ConferenceActivity) getActivity()).showFloatingView(null);
    }

    @OnClick(R2.id.addParticipantImageView)
    void addParticipant() {
        ((ConferenceActivity) getActivity()).addParticipant(Config.MAX_VIDEO_PARTICIPANT_COUNT - participants.size() - 1);
    }

    @OnClick(R2.id.muteImageView)
    void mute() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.muteAudio(!micEnabled);
            micEnabled = !micEnabled;
            muteImageView.setSelected(micEnabled);
        }
    }

    @OnClick(R2.id.switchCameraImageView)
    void switchCamera() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.switchCamera();
        }
    }

    @OnClick(R2.id.videoImageView)
    void video() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            session.muteVideo(!videoEnabled);
            videoEnabled = !videoEnabled;
            videoImageView.setSelected(videoEnabled);
        }
    }

    @OnClick(R2.id.hangupImageView)
    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null) {
            session.endCall();
        }
    }

    // hangup 触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        // do nothing
    }

    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callState == AVEngineKit.CallState.Connected) {
            updateParticipantStatus(callSession);
        } else if (callState == AVEngineKit.CallState.Idle) {
            getActivity().finish();
        }
        Toast.makeText(getActivity(), "" + callState.name(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didParticipantJoined(String userId) {
        if (participants.contains(userId)) {
            return;
        }
        int count = participantGridView.getChildCount();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int with = dm.widthPixels;

        participantGridView.getLayoutParams().height = with;

        UserInfo userInfo = userViewModel.getUserInfo(userId, false);
        ConferenceItem multiCallItem = new ConferenceItem(getActivity());
        multiCallItem.setTag(userInfo.uid);
        multiCallItem.setLayoutParams(new ViewGroup.LayoutParams(with / 3, with / 3));
        multiCallItem.getStatusTextView().setText(userInfo.displayName);
        multiCallItem.setOnClickListener(clickListener);
        GlideApp.with(multiCallItem).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(multiCallItem.getPortraitImageView());
        participantGridView.addView(multiCallItem);
        participants.add(userId);
    }

    @Override
    public void didParticipantConnected(String userId) {

    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        View view = participantGridView.findViewWithTag(userId);
        if (view != null) {
            participantGridView.removeView(view);
        }
        participants.remove(userId);

        if (userId.equals(focusVideoUserId)) {
            focusVideoUserId = null;
            focusVideoContainerFrameLayout.removeView(focusConferenceItem);
            focusConferenceItem = null;
        }

        Toast.makeText(getActivity(), "用户" + userId + "离开了通话", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didChangeMode(boolean audioOnly) {

    }

    @Override
    public void didCreateLocalVideoTrack() {
        ConferenceItem item = rootLinearLayout.findViewWithTag(me.uid);
        if (item.findViewWithTag("v_" + me.uid) != null) {
            return;
        }

        SurfaceView surfaceView = getEngineKit().getCurrentSession().createRendererView();
        if (surfaceView != null) {
            surfaceView.setZOrderMediaOverlay(false);
            surfaceView.setTag("v_" + me.uid);
            item.addView(surfaceView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            getEngineKit().getCurrentSession().setupLocalVideo(surfaceView, scalingType);
        }
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        ConferenceItem item = rootLinearLayout.findViewWithTag(userId);
        if (item == null) {
            return;
        }

        SurfaceView surfaceView = getEngineKit().getCurrentSession().createRendererView();
        if (surfaceView != null) {
            surfaceView.setZOrderMediaOverlay(false);
            item.addView(surfaceView);
            surfaceView.setTag("v_" + userId);
            getEngineKit().getCurrentSession().setupRemoteVideo(userId, surfaceView, scalingType);
        }
        bringParticipantVideoFront();
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        ConferenceItem item = rootLinearLayout.findViewWithTag(userId);
        if (item != null) {
            View view = item.findViewWithTag("v_" + userId);
            if (view != null) {
                item.removeView(view);
            }

            item.getStatusTextView().setText("关闭摄像头");
            item.getStatusTextView().setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void didError(String reason) {
        Toast.makeText(getActivity(), "发生错误" + reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {

    }

    @Override
    public void didVideoMuted(String userId, boolean videoMuted) {
        if (videoMuted) {
            didRemoveRemoteVideoTrack(userId);
        } else {
            if (userId.equals(me.uid)) {
                didCreateLocalVideoTrack();
            } else {
                didReceiveRemoteVideoTrack(userId);
            }
        }
    }

    private ConferenceItem getUserConferenceItem(String userId) {
        return participantGridView.findViewWithTag(userId);
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        Log.d(TAG, userId + " volume " + volume);
        ConferenceItem multiCallItem = getUserConferenceItem(userId);
        if (multiCallItem != null) {
            if (volume > 1000) {
                multiCallItem.getStatusTextView().setVisibility(View.VISIBLE);
                multiCallItem.getStatusTextView().setText("正在说话");
            } else {
                multiCallItem.getStatusTextView().setVisibility(View.GONE);
                multiCallItem.getStatusTextView().setText("");
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String userId = (String) v.getTag();
            if (!userId.equals(focusVideoUserId)) {
                ConferenceItem clickedConferenceItem = (ConferenceItem) v;
                int clickedIndex = participantGridView.indexOfChild(v);
                participantGridView.removeView(clickedConferenceItem);
                participantGridView.endViewTransition(clickedConferenceItem);

                if (focusConferenceItem != null) {
                    focusVideoContainerFrameLayout.removeView(focusConferenceItem);
                    focusVideoContainerFrameLayout.endViewTransition(focusConferenceItem);
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    int with = dm.widthPixels;
                    participantGridView.addView(focusConferenceItem, clickedIndex, new FrameLayout.LayoutParams(with / 3, with / 3));
                    focusConferenceItem.setOnClickListener(clickListener);
                }
                focusVideoContainerFrameLayout.addView(clickedConferenceItem, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                clickedConferenceItem.setOnClickListener(null);
                focusConferenceItem = clickedConferenceItem;
                focusVideoUserId = userId;

                bringParticipantVideoFront();
            } else {
                // do nothing

            }
        }
    };

    private void bringParticipantVideoFront() {
        SurfaceView focusSurfaceView = focusConferenceItem.findViewWithTag("v_" + focusVideoUserId);
        if (focusSurfaceView != null) {
            focusSurfaceView.setZOrderOnTop(false);
            focusSurfaceView.setZOrderMediaOverlay(false);
        }
        int count = participantGridView.getChildCount();
        for (int i = 0; i < count; i++) {
            ConferenceItem callItem = (ConferenceItem) participantGridView.getChildAt(i);
            SurfaceView surfaceView = callItem.findViewWithTag("v_" + callItem.getTag());
            if (surfaceView != null) {
                surfaceView.setZOrderMediaOverlay(true);
                surfaceView.setZOrderOnTop(true);
            }
        }
    }

    private Handler handler = new Handler();

    private void updateCallDuration() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() == AVEngineKit.CallState.Connected) {
            long s = System.currentTimeMillis() - session.getConnectedTime();
            s = s / 1000;
            String text;
            if (s > 3600) {
                text = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
            } else {
                text = String.format("%02d:%02d", s / 60, (s % 60));
            }
            durationTextView.setText(text);
        }
        handler.postDelayed(this::updateCallDuration, 1000);
    }
}
