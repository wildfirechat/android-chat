/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

class VideoConferenceMainView extends RelativeLayout {

    FrameLayout previewContainerFrameLayout;
    FrameLayout focusContainerFrameLayout;

    private AVEngineKit.CallSession callSession;
    private AVEngineKit.ParticipantProfile myProfile;
    private AVEngineKit.ParticipantProfile focusProfile;

    private ConferenceParticipantItemView focusParticipantItemView;
    private ConferenceParticipantItemView myParticipantItemView;

    private OnClickListener clickListener;

    public VideoConferenceMainView(Context context) {
        super(context);
        initView(context, null);
    }

    public VideoConferenceMainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public VideoConferenceMainView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VideoConferenceMainView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_conference_video_main, this);
        bindViews(view);
    }

    private void bindViews(View view) {
        previewContainerFrameLayout = view.findViewById(R.id.previewContainerFrameLayout);
        focusContainerFrameLayout = view.findViewById(R.id.focusContainerFrameLayout);
    }

    public void setupProfiles(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile myProfile, AVEngineKit.ParticipantProfile focusProfile) {
        this.callSession = session;
        this.myProfile = myProfile;

        if (this.focusProfile != null && focusProfile != null && !this.focusProfile.isVideoMuted() && !this.focusProfile.getUserId().equals(focusProfile.getUserId())) {
            session.setParticipantVideoType(this.focusProfile.getUserId(), this.focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
        }
        this.focusProfile = focusProfile;
        // 不 post 一下，可能视频流界面黑屏，原因未知
        ChatManager.Instance().getMainHandler().post(() -> {
            setupConferenceMainView();
        });
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
        // setup 的时候，post 了一下，故这儿可能为空，需要判空
        if (userId.equals(ChatManager.Instance().getUserId()) && myParticipantItemView != null) {
            myParticipantItemView.updateVolume(volume);
        } else {
            if (focusProfile != null && focusProfile.getUserId().equals(userId) && focusParticipantItemView != null) {
                focusParticipantItemView.updateVolume(volume);
            }
        }
    }

    public void onDestroyView() {
        // do nothing
        // 要在页面取消选择之后，才会走到这儿，取消选择的时候，已经做了相关处理
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        super.setOnClickListener(l);
        this.clickListener = l;
    }

    public static final String TAG = "ConferenceVideoFragment";


    private void setupConferenceMainView() {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int height = Math.max(dm.heightPixels, dm.widthPixels);
        int width = Math.min(dm.widthPixels, dm.heightPixels);

        previewContainerFrameLayout.removeAllViews();

        List<AVEngineKit.ParticipantProfile> mainProfiles = new ArrayList<>();
//        if (!myProfile.isAudience()) {
        mainProfiles.add(myProfile);
//        }
        if (focusProfile != null && !focusProfile.getUserId().equals(myProfile.getUserId())) {
            mainProfiles.add(focusProfile);
        }

        for (AVEngineKit.ParticipantProfile profile : mainProfiles) {
            ConferenceParticipantItemView conferenceItem;
            if (profile.isAudience() || profile.isVideoMuted()) {
                conferenceItem = new ConferenceParticipantItemView(getContext());
//                conferenceItem.setBackgroundResource(R.color.gray0);
            } else {
                conferenceItem = new ConferenceParticipantItemVideoView(getContext());
                ((ConferenceParticipantItemVideoView) conferenceItem).setEnableVideoZoom(true);
            }
            conferenceItem.setOnClickListener(clickListener);
            conferenceItem.setup(this.callSession, profile);
            if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                myParticipantItemView = conferenceItem;
            } else {
                focusParticipantItemView = conferenceItem;
            }

            if (focusProfile != null && !focusProfile.getUserId().equals(myProfile.getUserId())) {
                if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                    previewContainerFrameLayout.removeAllViews();
                    conferenceItem.setLayoutParams(new ViewGroup.LayoutParams(width / 3, height / 4));
                    previewContainerFrameLayout.addView(conferenceItem);
                    conferenceItem.setBackgroundResource(R.color.gray0_half_transparent);
                    SurfaceView focusSurfaceView = conferenceItem.findViewWithTag("sv_" + profile.getUserId());
                    if (focusSurfaceView != null) {
                        focusSurfaceView.setZOrderMediaOverlay(true);
                    }
                } else {
                    focusContainerFrameLayout.removeAllViews();
                    conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    focusContainerFrameLayout.addView(conferenceItem);
                    if (!profile.isAudience() && !profile.isVideoMuted()) {
                        this.callSession.setParticipantVideoType(focusProfile.getUserId(), focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_BIG_STREAM);
                    }
                }
            } else {
                previewContainerFrameLayout.removeAllViews();
                focusContainerFrameLayout.removeAllViews();
                conferenceItem.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                focusContainerFrameLayout.addView(conferenceItem);
            }
        }
    }

    public void onPageUnselected(boolean keepSubscribeFocusVideo) {
        if (focusProfile != null && !keepSubscribeFocusVideo) {
            callSession.setParticipantVideoType(focusProfile.getUserId(), focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
        }
    }

}
