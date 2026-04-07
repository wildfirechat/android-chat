/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
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

    // Drag related fields
    private static final float CLICK_DRAG_TOLERANCE = 10f;
    private boolean isPreviewDragging = false;
    private float dragStartX, dragStartY;
    private float previewStartX, previewStartY;
    private int statusBarHeight = 0;
    private int topBarHeight = 0;
    private int bottomBarHeight = 0;

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

    private void initScreenDimensions() {
        if (statusBarHeight == 0) {
            // Get status bar height
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
        }
    }
    
    // Get parent dimensions for drag boundaries
    private int getParentWidth() {
        View parent = (View) getParent();
        if (parent != null) {
            return parent.getWidth();
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.max(dm.widthPixels, dm.heightPixels);
    }
    
    private int getParentHeight() {
        View parent = (View) getParent();
        if (parent != null) {
            return parent.getHeight();
        }
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Math.max(dm.widthPixels, dm.heightPixels);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (previewContainerFrameLayout == null || previewContainerFrameLayout.getChildCount() == 0) {
            return super.onTouchEvent(event);
        }
        
        // Check if touch is within preview container
        float x = event.getX();
        float y = event.getY();
        float previewX = previewContainerFrameLayout.getX();
        float previewY = previewContainerFrameLayout.getY();
        float previewW = previewContainerFrameLayout.getWidth();
        float previewH = previewContainerFrameLayout.getHeight();
        
        boolean inPreview = x >= previewX && x <= previewX + previewW &&
                           y >= previewY && y <= previewY + previewH;
        
        if (!inPreview) {
            return super.onTouchEvent(event);
        }
        
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartX = event.getRawX();
                dragStartY = event.getRawY();
                previewStartX = previewContainerFrameLayout.getX();
                previewStartY = previewContainerFrameLayout.getY();
                isPreviewDragging = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float rawX = event.getRawX();
                float rawY = event.getRawY();
                float dx = rawX - dragStartX;
                float dy = rawY - dragStartY;
                
                if (!isPreviewDragging && (Math.abs(dx) > CLICK_DRAG_TOLERANCE || Math.abs(dy) > CLICK_DRAG_TOLERANCE)) {
                    isPreviewDragging = true;
                }
                
                if (isPreviewDragging) {
                    float newX = previewStartX + dx;
                    float newY = previewStartY + dy;
                    
                    // Constrain to parent bounds
                    initScreenDimensions();
                    int parentWidth = getParentWidth();
                    int parentHeight = getParentHeight();
                    newX = Math.max(0, Math.min(newX, parentWidth - previewW));
                    newY = Math.max(statusBarHeight, Math.min(newY, parentHeight - previewH));
                    
                    previewContainerFrameLayout.setX(newX);
                    previewContainerFrameLayout.setY(newY);
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (!isPreviewDragging) {
                    // Click - toggle bars
                    if (clickListener != null) {
                        clickListener.onClick(this);
                    }
                }
                isPreviewDragging = false;
                return true;
                
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                isPreviewDragging = false;
                return true;
        }
        
        return super.onTouchEvent(event);
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (previewContainerFrameLayout == null || previewContainerFrameLayout.getChildCount() == 0) {
            return super.onInterceptTouchEvent(event);
        }
        
        float x = event.getX();
        float y = event.getY();
        float previewX = previewContainerFrameLayout.getX();
        float previewY = previewContainerFrameLayout.getY();
        float previewW = previewContainerFrameLayout.getWidth();
        float previewH = previewContainerFrameLayout.getHeight();
        
        boolean inPreview = x >= previewX && x <= previewX + previewW &&
                           y >= previewY && y <= previewY + previewH;
        
        if (inPreview) {
            // Intercept all touches in preview area for dragging
            return true;
        }
        
        return super.onInterceptTouchEvent(event);
    }

    public void setupProfiles(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile myProfile, AVEngineKit.ParticipantProfile focusProfile) {
        this.callSession = session;

        // 不 post 一下，可能视频流界面黑屏，原因未知
        ChatManager.Instance().getMainHandler().post(() -> {
            setupConferenceMainView(myProfile, focusProfile);
        });
    }

    public void updateMyProfile(AVEngineKit.ParticipantProfile myProfile) {
        if (this.myProfile == null) {
            // myProfile 和 focusProfile 换位置
        }
        setupConferenceMainView(myProfile, this.focusProfile);
    }

    public void updateFocusProfile(AVEngineKit.ParticipantProfile focusProfile) {
        if (this.focusProfile == null) {
            // myProfile 和 focusProfile 换位置
        }
        setupConferenceMainView(this.myProfile, focusProfile);
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

    // Set bar heights for collision detection
    public void setBarHeights(int topBarHeight, int bottomBarHeight) {
        this.topBarHeight = topBarHeight;
        this.bottomBarHeight = bottomBarHeight;
    }

    // Adjust preview position when bars are shown/hidden (no animation)
    public void adjustPositionForBars(boolean barsVisible) {
        if (previewContainerFrameLayout == null) return;
        
        float currentY = previewContainerFrameLayout.getY();
        float currentX = previewContainerFrameLayout.getX();
        float previewH = previewContainerFrameLayout.getHeight();
        float previewW = previewContainerFrameLayout.getWidth();
        
        if (barsVisible && (topBarHeight > 0 || bottomBarHeight > 0)) {
            int parentHeight = getParentHeight();
            boolean needAdjust = false;
            
            // Check if overlapping with top bar
            if (currentY < topBarHeight) {
                currentY = topBarHeight + 10;
                needAdjust = true;
            }
            
            // Check if overlapping with bottom bar
            float bottomBarTop = parentHeight - bottomBarHeight;
            if (currentY + previewH > bottomBarTop) {
                currentY = bottomBarTop - previewH - 10;
                needAdjust = true;
            }
            
            if (needAdjust) {
                // Ensure within horizontal bounds
                int parentWidth = getParentWidth();
                currentX = Math.max(0, Math.min(currentX, parentWidth - previewW));
                
                previewContainerFrameLayout.setX(currentX);
                previewContainerFrameLayout.setY(currentY);
            }
        }
        // When bars hide, keep current position (respect user's drag)
    }

    public static final String TAG = "ConferenceVideoFragment";


    private void setupConferenceMainView(AVEngineKit.ParticipantProfile myProfile, AVEngineKit.ParticipantProfile focusProfile) {

        if ((this.myProfile != null && this.myProfile.equals(myProfile) && (this.focusProfile != null && this.focusProfile.equals(focusProfile)))) {
            return;
        }

        this.myProfile = myProfile;
        this.focusProfile = focusProfile;

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
            boolean isMyProfile = profile.getUserId().equals(ChatManager.Instance().getUserId());
            if (profile.isAudience() || profile.isVideoMuted()) {
                conferenceItem = new ConferenceParticipantItemView(getContext());
//                conferenceItem.setBackgroundResource(R.color.gray0);
            } else {
                conferenceItem = new ConferenceParticipantItemVideoView(getContext());
                // Disable zoom for preview window to allow dragging
                ((ConferenceParticipantItemVideoView) conferenceItem).setEnableVideoZoom(false);
            }
            // Only set click listener for focus view, not for preview window
            if (!isMyProfile) {
                conferenceItem.setOnClickListener(clickListener);
            }
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
        this.focusProfile = null;
    }

}
