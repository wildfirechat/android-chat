/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 主播连麦管理 BottomSheet
 * <p>
 * 三个区域：
 * 1. 连麦中（activeCoStreamers）—— "结束连麦" 按钮
 * 2. 申请连麦（coStreamRequests）—— "同意" / "拒绝" 按钮
 * 3. 可邀请（chatroom viewers minus above）—— "邀请连麦" 按钮
 * </p>
 */
public class LiveCoStreamManagerFragment extends BottomSheetDialogFragment {

    private static final String ARG_CALL_ID = "callId";

    private String callId;

    private RecyclerView activeRecycler;
    private RecyclerView requestingRecycler;
    private RecyclerView invitableRecycler;

    public static LiveCoStreamManagerFragment newInstance(String callId) {
        LiveCoStreamManagerFragment f = new LiveCoStreamManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, callId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            callId = getArguments().getString(ARG_CALL_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_co_stream_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activeRecycler = view.findViewById(R.id.activeCoStreamersRecycler);
        requestingRecycler = view.findViewById(R.id.requestingRecycler);
        invitableRecycler = view.findViewById(R.id.invitableRecycler);

        activeRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        requestingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        invitableRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadData();
    }

    private void loadData() {
        LiveStreamingKit kit = LiveStreamingKit.getInstance();
        List<String> active = kit.getActiveCoStreamers();
        List<String> requesting = kit.getCoStreamRequests();

        activeRecycler.setAdapter(new ActiveAdapter(active));
        requestingRecycler.setAdapter(new RequestingAdapter(requesting));

        // Invitable = chatroom members minus active and requesting
        kit.getViewers(callId, new LiveStreamingKit.GetViewersCallback() {
            @Override
            public void onSuccess(List<String> viewers) {
                if (!isAdded()) return;
                List<String> invitable = new ArrayList<>();
                for (String uid : viewers) {
                    if (!active.contains(uid) && !requesting.contains(uid)) {
                        invitable.add(uid);
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        invitableRecycler.setAdapter(new InvitableAdapter(invitable));
                    }
                });
            }

            @Override
            public void onFailure() {
                // leave empty
            }
        });
    }

    // ── Active co-streamers adapter ──────────────────────────────────────────

    private class ActiveAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        ActiveAdapter(List<String> userIds) {
            this.userIds = userIds;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_co_stream_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(getString(R.string.live_co_stream_end_for_user));
            holder.primaryAction.setOnClickListener(v -> {
                LiveStreamingKit.getInstance().endCoStreamForUser(uid);
                userIds.remove(position);
                notifyItemRemoved(position);
            });
            holder.secondaryAction.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }
    }

    // ── Requesting adapter ────────────────────────────────────────────────────

    private class RequestingAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        RequestingAdapter(List<String> userIds) {
            this.userIds = userIds;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_co_stream_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(R.string.live_co_stream_accept);
            holder.primaryAction.setOnClickListener(v -> {
                LiveStreamingKit.getInstance().acceptCoStreamRequest(uid);
                userIds.remove(position);
                notifyItemRemoved(position);
            });
            holder.secondaryAction.setVisibility(View.VISIBLE);
            holder.secondaryAction.setText(R.string.live_co_stream_reject);
            holder.secondaryAction.setOnClickListener(v -> {
                LiveStreamingKit.getInstance().rejectCoStreamRequest(uid);
                userIds.remove(position);
                notifyItemRemoved(position);
            });
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }
    }

    // ── Invitable adapter ─────────────────────────────────────────────────────

    private class InvitableAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        InvitableAdapter(List<String> userIds) {
            this.userIds = userIds;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_co_stream_user, parent, false);
            return new UserViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(R.string.live_co_stream_invite);
            holder.primaryAction.setOnClickListener(v -> {
                LiveStreamingKit.getInstance().inviteCoStream(uid);
                Toast.makeText(requireContext(), R.string.live_co_stream_invite_sent, Toast.LENGTH_SHORT).show();
                userIds.remove(position);
                notifyItemRemoved(position);
            });
            holder.secondaryAction.setVisibility(View.GONE);
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarView;
        TextView nameView;
        TextView primaryAction;
        TextView secondaryAction;

        UserViewHolder(View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.userAvatarImageView);
            nameView = itemView.findViewById(R.id.userNameTextView);
            primaryAction = itemView.findViewById(R.id.primaryActionButton);
            secondaryAction = itemView.findViewById(R.id.secondaryActionButton);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void bindUser(UserViewHolder holder, String userId) {
        UserInfo info = ChatManager.Instance().getUserInfo(userId, false);
        holder.nameView.setText((info != null && !TextUtils.isEmpty(info.displayName))
            ? info.displayName : userId);
        if (info != null && !TextUtils.isEmpty(info.portrait) && isAdded()) {
            Glide.with(this)
                .load(info.portrait)
                .circleCrop()
                .placeholder(R.drawable.live_avatar_placeholder)
                .into(holder.avatarView);
        }
    }

    private String getDisplayName(String userId) {
        UserInfo info = ChatManager.Instance().getUserInfo(userId, false);
        return (info != null && !TextUtils.isEmpty(info.displayName)) ? info.displayName : userId;
    }
}
