/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.lifecycle.ViewTreeViewModelStoreOwner;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantItemView extends RelativeLayout {
    public ImageView portraitImageView;
    public TextView statusTextView;
    protected MicImageView micImageView;
    protected ImageView videoStateImageView;
    protected TextView nameTextView;

    private UserViewModel userViewModel;

    public ConferenceParticipantItemView(@NonNull Context context) {
        super(context);
        initView(context, null);
    }

    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConferenceParticipantItemView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    protected void initView(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_conference_participant_grid_item, this);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        statusTextView = view.findViewById(R.id.statusTextView);
        micImageView = view.findViewById(R.id.micImageView);
        videoStateImageView = view.findViewById(R.id.videoStateImageView);
        nameTextView = view.findViewById(R.id.userNameTextView);
    }

    public ImageView getPortraitImageView() {
        return portraitImageView;
    }

    public TextView getStatusTextView() {
        return statusTextView;
    }

    public void setup(AVEngineKit.CallSession session, AVEngineKit.ParticipantProfile profile) {
        String participantKey = VoipBaseActivity.participantKey(profile.getUserId(), profile.isScreenSharing());
        UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
        this.setTag(participantKey);
        updateParticipantUserInfoViews(userInfo);

        LifecycleOwner lifecycleOwner = ViewTreeLifecycleOwner.get(nameTextView);
        ViewModelStoreOwner viewModelStoreOwner = ViewTreeViewModelStoreOwner.get(nameTextView);
        if (viewModelStoreOwner != null && lifecycleOwner != null) {
            UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
            userViewModel.userInfoLiveData().observe(lifecycleOwner, userInfos -> {
                for (UserInfo info : userInfos) {
                    if (info.uid.equals(profile.getUserId())) {
                        updateParticipantUserInfoViews(info);
                        break;
                    }
                }
            });

        }

        //statusTextView.setText(R.string.connecting);
//        } else {
//            videoContainer.setVisibility(GONE);
//        }
        videoStateImageView.setSelected(profile.isAudience() || profile.isVideoMuted());
        micImageView.setMuted(profile.isAudience() || profile.isAudioMuted());
    }

    private void updateParticipantUserInfoViews(UserInfo userInfo) {
        Glide.with(this).load(userInfo.portrait).apply(new RequestOptions().circleCrop()).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo));
    }

    public void updateParticipantProfile(AVEngineKit.ParticipantProfile profile) {

    }

    public void updateVolume(int volume) {
        int padding = 0;
        if (volume > 500) {
            padding = 2;
            this.portraitImageView.setBackgroundResource(R.drawable.av_conference_participant_item_highlight_boarder);
        } else {
            this.portraitImageView.setBackground(null);
        }
        this.portraitImageView.setPadding(padding, padding, padding, padding);
        micImageView.setVolume(volume);
    }
}
