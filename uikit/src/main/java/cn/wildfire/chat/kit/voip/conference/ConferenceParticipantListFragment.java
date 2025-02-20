/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceParticipantListFragment extends Fragment {
    RecyclerView recyclerView;
    TextView handupTextView;
    TextView applyingUnmuteAudioTextView;
    TextView applyingUnmuteVideoTextView;

    Button muteAllButton;
    Button unmuteAllButton;
    private AVEngineKit.CallSession callSession;

    private List<AVEngineKit.ParticipantProfile> participantProfiles;
    private ParticipantListAdapter adapter;

    private ConferenceManager conferenceManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_conference_participant_list, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.applyingUnmuteAudioTextView).setOnClickListener(_v -> showApplyingUnmuteDialog(true));
        view.findViewById(R.id.applyingUnmuteVideoTextView).setOnClickListener(_v -> showApplyingUnmuteDialog(false));
        view.findViewById(R.id.handupTextView).setOnClickListener(_v -> showHandupDialog());
        view.findViewById(R.id.muteAllButton).setOnClickListener(_v -> muteAll(true));
        view.findViewById(R.id.unmuteAllButton).setOnClickListener(_v -> unmuteAll(true));
    }

    private void bindViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        handupTextView = view.findViewById(R.id.handupTextView);
        applyingUnmuteAudioTextView = view.findViewById(R.id.applyingUnmuteAudioTextView);
        applyingUnmuteVideoTextView = view.findViewById(R.id.applyingUnmuteVideoTextView);
        muteAllButton = view.findViewById(R.id.muteAllButton);
        unmuteAllButton = view.findViewById(R.id.unmuteAllButton);
    }

    private void init() {
        callSession = AVEngineKit.Instance().getCurrentSession();
        if (adapter == null) {
            adapter = new ParticipantListAdapter();
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        }
        conferenceManager = ConferenceManager.getManager();

        LiveDataBus.subscribe("kConferenceMemberChanged", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceEnded", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceMutedStateChanged", this, o -> loadAndShowConferenceParticipants());
        LiveDataBus.subscribe("kConferenceCommandStateChanged", this, o -> loadAndShowConferenceParticipants());

        this.loadAndShowConferenceParticipants();
    }

    void showApplyingUnmuteDialog(boolean audio) {
        if (audio) {
            ConferenceApplyUnmuteAudioListFragment fragment = new ConferenceApplyUnmuteAudioListFragment();
            fragment.show(getChildFragmentManager(), "applyUnmuteAudioFragment");
        } else {
            ConferenceApplyUnmuteVideoListFragment fragment = new ConferenceApplyUnmuteVideoListFragment();
            fragment.show(getChildFragmentManager(), "applyUnmuteVideoFragment");
        }
    }

    void showHandupDialog() {
        ConferenceHandUpListFragment fragment = new ConferenceHandUpListFragment();
        fragment.show(getChildFragmentManager(), "handUpFragment");
    }

    void muteAll(boolean audio) {
        new MaterialDialog.Builder(getActivity())
            .title(audio ? getString(R.string.conf_mute_all_audio_title) : getString(R.string.conf_mute_all_video_title))
            .checkBoxPrompt(audio ? getString(R.string.conf_allow_unmute_audio) : getString(R.string.conf_allow_unmute_video), false, null)
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .positiveColor(getResources().getColor(R.color.red0))
            .onPositive((dialog, which) -> conferenceManager.requestMuteAll(audio, dialog.isPromptCheckBoxChecked()))
            .show();
    }

    void unmuteAll(boolean audio) {
        new MaterialDialog.Builder(getActivity())
            .title(audio ? getString(R.string.conf_unmute_all_audio_title) : getString(R.string.conf_unmute_all_video_title))
            .checkBoxPrompt(audio ? getString(R.string.conf_require_unmute_audio) : getString(R.string.conf_require_unmute_video), false, null)
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .positiveColor(getResources().getColor(R.color.red0))
            .onPositive((dialog, which) -> conferenceManager.requestUnmuteAll(audio, dialog.isPromptCheckBoxChecked()))
            .show();
    }

    private void loadAndShowConferenceParticipants() {
        if (conferenceManager.getCurrentConferenceInfo() != null && ChatManager.Instance().getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
            List<String> applyingUnmuteAudioMembers = conferenceManager.getApplyingUnmuteAudioMembers();
            if (applyingUnmuteAudioMembers.isEmpty()) {
                applyingUnmuteAudioTextView.setVisibility(View.GONE);
            } else {
                applyingUnmuteAudioTextView.setVisibility(View.VISIBLE);
                String text = ChatManager.Instance().getUserDisplayName(applyingUnmuteAudioMembers.get(0));
                if (applyingUnmuteAudioMembers.size() > 1) {
                    text += getString(R.string.conf_etc);
                }
                text += getString(R.string.conf_requesting_unmute_audio);
                applyingUnmuteAudioTextView.setText(text);
            }
            List<String> applyingUnmuteVideoMembers = conferenceManager.getApplyingUnmuteVideoMembers();
            if (applyingUnmuteVideoMembers.isEmpty()) {
                applyingUnmuteVideoTextView.setVisibility(View.GONE);
            } else {
                applyingUnmuteVideoTextView.setVisibility(View.VISIBLE);
                String text = ChatManager.Instance().getUserDisplayName(applyingUnmuteVideoMembers.get(0));
                if (applyingUnmuteVideoMembers.size() > 1) {
                    text += getString(R.string.conf_etc);
                }
                text += getString(R.string.conf_requesting_unmute_video);
                applyingUnmuteVideoTextView.setText(text);
            }
            List<String> handupMembers = conferenceManager.getHandUpMembers();
            if (handupMembers.isEmpty()) {
                handupTextView.setVisibility(View.GONE);
            } else {
                handupTextView.setVisibility(View.VISIBLE);
                String text = ChatManager.Instance().getUserDisplayName(handupMembers.get(0));
                if (handupMembers.size() > 1) {
                    text += getString(R.string.conf_etc);
                }
                text += getString(R.string.conf_hand_raising);
                handupTextView.setText(text);
            }

            if (conferenceManager.isMuteAllAudio()) {
                muteAllButton.setEnabled(false);
                unmuteAllButton.setEnabled(true);
            } else {
                muteAllButton.setEnabled(true);
                unmuteAllButton.setEnabled(false);
            }
        } else {
            handupTextView.setVisibility(View.GONE);
            applyingUnmuteAudioTextView.setVisibility(View.GONE);
            applyingUnmuteVideoTextView.setVisibility(View.GONE);
            muteAllButton.setVisibility(View.GONE);
            unmuteAllButton.setVisibility(View.GONE);
        }

        this.participantProfiles = callSession.getParticipantProfiles();
        this.participantProfiles.add(callSession.getMyProfile());
        adapter.notifyDataSetChanged();
    }

    private void onClickParticipant(AVEngineKit.ParticipantProfile profile) {
        Callable<Void> unmuteVideoCall = () -> {
            conferenceManager.muteVideo(false);
            return null;
        };
        Callable<Void> muteVideoCall = () -> {
            conferenceManager.muteVideo(true);
            return null;
        };
        Callable<Void> unmuteAudioCall = () -> {
            conferenceManager.muteAudio(false);
            return null;
        };
        Callable<Void> muteAudioCall = () -> {
            conferenceManager.muteAudio(true);
            return null;
        };
        Callable<Void> requestFocus = () -> {
            conferenceManager.requestFocus(profile.getUserId(), null);
            return null;
        };
        Callable<Void> cancelFocus = () -> {
            conferenceManager.requestCancelFocus(null);
            return null;
        };
        Callable<Void> userInfoCall = () -> {
            Intent intent = new Intent(getActivity(), UserInfoActivity.class);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
            intent.putExtra("userInfo", userInfo);
            startActivity(intent);
            return null;
        };

        Map<String, Callable<Void>> items = new HashMap<>();
        String selfUid = ChatManager.Instance().getUserId();
        items.put(getString(R.string.conf_view_user_info), userInfoCall);
        ConferenceInfo conferenceInfo = conferenceManager.getCurrentConferenceInfo();
        List<String> managers = conferenceInfo.getManagers();
        managers = managers != null ? managers : new ArrayList<>();
        if (selfUid.equals(conferenceInfo.getOwner()) || managers.contains(selfUid)) {
            if (selfUid.equals(profile.getUserId())) {
                // 主持人自己
                if (profile.isAudioMuted()) {
                    items.put(getString(R.string.conf_enable_audio), unmuteAudioCall);
                } else {
                    items.put(getString(R.string.conf_disable_audio), muteAudioCall);
                }
                if (profile.isVideoMuted()) {
                    items.put(getString(R.string.conf_enable_video), unmuteVideoCall);
                } else {
                    items.put(getString(R.string.conf_disable_video), muteVideoCall);
                }
                if (profile.isAudioMuted() && profile.isVideoMuted()) {
                    items.put(getString(R.string.conf_enable_audio_video), unmuteAudioCall);
                }
            } else {
                // 他人
                if (profile.isAudience() || profile.isAudioMuted()) {
                    items.put(getString(R.string.conf_invite_to_speak), () -> {
                        conferenceManager.requestMemberMute(true, profile.getUserId(), false);
                        return null;
                    });
                } else if (!profile.isAudience() && !profile.isAudioMuted()) {
                    items.put(getString(R.string.conf_cancel_speak), () -> {
                        conferenceManager.requestMemberMute(true, profile.getUserId(), true);
                        return null;
                    });
                }
                if (profile.isAudience() || profile.isVideoMuted()) {
                    items.put(getString(R.string.conf_invite_turn_on_camera), () -> {
                        conferenceManager.requestMemberMute(false, profile.getUserId(), false);
                        return null;
                    });
                } else if (!profile.isAudience() && !profile.isVideoMuted()) {
                    items.put(getString(R.string.conf_turn_off_camera), () -> {
                        conferenceManager.requestMemberMute(false, profile.getUserId(), true);
                        return null;
                    });
                }
                items.put(getString(R.string.conf_remove_member), () -> {
                    callSession.kickoffParticipant(profile.getUserId(), null);
                    return null;
                });
            }
            if (profile.getUserId().equals(conferenceInfo.getFocus())) {
                items.put(getString(R.string.conf_cancel_focus_user), cancelFocus);
            } else {
                items.put(getString(R.string.conf_set_as_focus_user), requestFocus);
            }
        } else {
            if (selfUid.equals(profile.getUserId())) {
                // 自己
                if (profile.isAudience()) {
                    items.put(getString(R.string.conf_hand_up), () -> {
                        conferenceManager.handUp(true);
                        return null;
                    });
                } else {
                    if (profile.isAudioMuted()) {
                        items.put(getString(R.string.conf_enable_audio), unmuteAudioCall);
                    } else {
                        items.put(getString(R.string.conf_disable_audio), muteAudioCall);
                    }
                    if (profile.isVideoMuted()) {
                        items.put(getString(R.string.conf_enable_video), unmuteVideoCall);
                    } else {
                        items.put(getString(R.string.conf_disable_video), muteVideoCall);
                    }
                }
            } else {
                // do nothing
            }

        }

        new MaterialDialog.Builder(getActivity())
            .cancelable(true)
            .items(items.keySet())
            .itemsCallback((dialog, itemView, position, text) -> {
                try {
                    items.get(text).call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            })
            .show();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private View itemView;

        ImageView portraitImageView;
        TextView nameTextView;
        TextView descTextView;
        ImageView audioImageView;
        ImageView videoImageView;

        public ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            bindViews(itemView);
        }

        private void bindViews(View itemView) {
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            audioImageView = itemView.findViewById(R.id.audioImageView);
            videoImageView = itemView.findViewById(R.id.videoImageView);
        }

        public void onBind(AVEngineKit.ParticipantProfile profile) {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(profile.getUserId(), false);
            String displayName = ChatManager.Instance().getUserDisplayName(userInfo);
            nameTextView.setText(displayName);
            Glide.with(this.itemView).load(userInfo.portrait).placeholder(R.mipmap.avatar_def)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                .into(portraitImageView);

            String desc = "";
            if (profile.getUserId().equals(ChatManager.Instance().getUserId())) {
                if (profile.getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
                    desc = getString(R.string.conf_host_me);
                } else {
                    desc = getString(R.string.conf_me);
                }
            } else {
                if (profile.getUserId().equals(conferenceManager.getCurrentConferenceInfo().getOwner())) {
                    desc = getString(R.string.conf_host);
                    if (profile.isScreenSharing()) {
                        desc += getString(R.string.conf_screen_sharing_suffix);
                    }
                } else {
                    if (profile.isScreenSharing()) {
                        desc += getString(R.string.conf_screen_sharing);
                    }
                }
            }
            if (TextUtils.isEmpty(desc)) {
                descTextView.setVisibility(View.GONE);
            } else {
                descTextView.setVisibility(View.VISIBLE);
                descTextView.setText(desc);
            }

            audioImageView.setSelected(profile.isAudience() || profile.isAudioMuted());
            videoImageView.setSelected(profile.isAudience() || profile.isVideoMuted());
            this.itemView.setOnClickListener(v -> {
                onClickParticipant(profile);
            });
        }
    }

    class ParticipantListAdapter extends RecyclerView.Adapter<ParticipantViewHolder> {

        @NonNull
        @Override
        public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.av_conference_participant_list_item, parent, false);
            return new ParticipantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
            holder.onBind(participantProfiles.get(position));
        }

        @Override
        public int getItemCount() {
            return participantProfiles == null ? 0 : participantProfiles.size();
        }
    }

    class Action {
        String name;

    }

}
