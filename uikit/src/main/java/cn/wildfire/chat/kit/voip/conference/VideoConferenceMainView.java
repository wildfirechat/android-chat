/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

class VideoConferenceMainView extends RelativeLayout {

    @BindView(R2.id.previewContainerFrameLayout)
    FrameLayout previewContainerFrameLayout;
    @BindView(R2.id.focusContainerFrameLayout)
    FrameLayout focusContainerFrameLayout;

    private AVEngineKit.CallSession callSession;
    private AVEngineKit.ParticipantProfile myProfile;
    private AVEngineKit.ParticipantProfile focusProfile;

    private ConferenceParticipantItemView focusParticipantItemView;
    private ConferenceParticipantItemView myParticipantItemView;

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
        ButterKnife.bind(this, view);
    }

    public void setup(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile myProfile, AVEngineKit.ParticipantProfile focusProfile) {
        this.callSession = session;
        this.myProfile = myProfile;
        this.focusProfile = focusProfile;
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


    private void setupConferenceMainView() {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.min(dm.widthPixels, dm.heightPixels);

        previewContainerFrameLayout.removeAllViews();

        focusVideoUserId = myProfile.getUserId();

        List<AVEngineKit.ParticipantProfile> mainProfiles = new ArrayList<>();
//        if (!myProfile.isAudience()) {
            mainProfiles.add(myProfile);
//        }
        if (focusProfile != null && !focusProfile.getUserId().equals(myProfile.getUserId())) {
            mainProfiles.add(focusProfile);
            focusVideoUserId = focusProfile.getUserId();
        }

        for (AVEngineKit.ParticipantProfile profile : mainProfiles) {
            ConferenceParticipantItemView conferenceItem = new ConferenceParticipantItemView(getContext());
//            conferenceItem.setOnClickListener(clickListener);
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
    }

    public void onPageUnselected() {
        if (focusProfile != null) {
            callSession.setParticipantVideoType(focusProfile.getUserId(), focusProfile.isScreenSharing(), AVEngineKit.VideoType.VIDEO_TYPE_NONE);
        }
    }


//    @Override
//    public void onStop() {
//        super.onStop();
//        handler.removeCallbacks(hideBarCallback);
//        handler.removeCallbacks(updateCallDurationRunnable);
//    }

}
