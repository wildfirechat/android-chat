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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 主播连麦管理 BottomSheet
 * <p>
 * 三个区域：连麦中 / 申请连麦 / 可邀请
 * 申请全部处理完后自动关闭。
 * </p>
 */
public class LiveCoStreamManagerFragment extends BottomSheetDialogFragment {

    private static final String ARG_CALL_ID = "callId";

    private String callId;

    // Active section
    private View activeSectionLayout;
    private View activeDivider;
    private RecyclerView activeRecycler;
    private List<String> activeUserIds;

    // Requesting section
    private View requestingSectionLayout;
    private View requestingDivider;
    private RecyclerView requestingRecycler;
    private List<String> requestingUserIds;

    // Invitable section
    private View invitableSectionLayout;
    private RecyclerView invitableRecycler;
    private TextView invitableEmptyView;

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

        activeSectionLayout = view.findViewById(R.id.activeSectionLayout);
        activeDivider = view.findViewById(R.id.activeDivider);
        activeRecycler = view.findViewById(R.id.activeCoStreamersRecycler);

        requestingSectionLayout = view.findViewById(R.id.requestingSectionLayout);
        requestingDivider = view.findViewById(R.id.requestingDivider);
        requestingRecycler = view.findViewById(R.id.requestingRecycler);

        invitableSectionLayout = view.findViewById(R.id.invitableSectionLayout);
        invitableRecycler = view.findViewById(R.id.invitableRecycler);
        invitableEmptyView = view.findViewById(R.id.invitableEmptyView);

        activeRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        requestingRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        invitableRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.refreshHintView).setOnClickListener(v -> loadData());

        loadData();
    }

    private void loadData() {
        LiveStreamingKit kit = LiveStreamingKit.getInstance();

        // Active
        activeUserIds = new ArrayList<>(kit.getActiveCoStreamers());
        activeRecycler.setAdapter(new ActiveAdapter(activeUserIds));
        setSectionVisible(activeSectionLayout, activeDivider, !activeUserIds.isEmpty());

        // Requesting
        requestingUserIds = new ArrayList<>(kit.getCoStreamRequests());
        requestingRecycler.setAdapter(new RequestingAdapter(requestingUserIds));
        // Divider between requesting and invitable shown when either requesting or active is visible
        boolean requestingVisible = !requestingUserIds.isEmpty();
        setSectionVisible(requestingSectionLayout, null, requestingVisible);

        // Invitable (async)
        kit.getViewers(callId, new LiveStreamingKit.GetViewersCallback() {
            @Override
            public void onSuccess(List<String> viewers) {
                if (!isAdded()) return;
                List<String> invitable = new ArrayList<>();
                for (String uid : viewers) {
                    if (!activeUserIds.contains(uid) && !requestingUserIds.contains(uid)) {
                        invitable.add(uid);
                    }
                }
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    invitableRecycler.setAdapter(new InvitableAdapter(invitable));
                    invitableEmptyView.setVisibility(invitable.isEmpty() ? View.VISIBLE : View.GONE);
                    invitableSectionLayout.setVisibility(View.VISIBLE);
                    // Show divider above invitable only when there's something above it
                    requestingDivider.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onFailure() {
                // leave invitable section hidden
            }
        });
    }

    /** Shows/hides a section and optionally its trailing divider. */
    private void setSectionVisible(View section, View divider, boolean visible) {
        section.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (divider != null) divider.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /** Called after each accept/reject. Dismisses sheet once all requests are handled. */
    private void onRequestHandled() {
        if (requestingUserIds.isEmpty()) {
            setSectionVisible(requestingSectionLayout, null, false);
            if (isAdded()) dismissAllowingStateLoss();
        }
    }

    // ── Active adapter ────────────────────────────────────────────────────────

    private class ActiveAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        ActiveAdapter(List<String> userIds) { this.userIds = userIds; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_live_co_stream_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(getString(R.string.live_co_stream_end_for_user));
            holder.secondaryAction.setVisibility(View.GONE);
            holder.primaryAction.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                holder.primaryAction.setEnabled(false);
                LiveStreamingKit.getInstance().endCoStreamForUser(uid);
                userIds.remove(pos);
                notifyItemRemoved(pos);
                if (userIds.isEmpty()) setSectionVisible(activeSectionLayout, activeDivider, false);
            });
        }

        @Override
        public int getItemCount() { return userIds.size(); }
    }

    // ── Requesting adapter ────────────────────────────────────────────────────

    private class RequestingAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        RequestingAdapter(List<String> userIds) { this.userIds = userIds; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_live_co_stream_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(R.string.live_co_stream_accept);
            holder.primaryAction.setEnabled(true);
            holder.secondaryAction.setVisibility(View.VISIBLE);
            holder.secondaryAction.setText(R.string.live_co_stream_reject);
            holder.secondaryAction.setEnabled(true);

            holder.primaryAction.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                holder.primaryAction.setEnabled(false);
                holder.secondaryAction.setEnabled(false);
                LiveStreamingKit.getInstance().acceptCoStreamRequest(uid);
                userIds.remove(pos);
                notifyItemRemoved(pos);
                onRequestHandled();
            });

            holder.secondaryAction.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                holder.primaryAction.setEnabled(false);
                holder.secondaryAction.setEnabled(false);
                LiveStreamingKit.getInstance().rejectCoStreamRequest(uid);
                userIds.remove(pos);
                notifyItemRemoved(pos);
                onRequestHandled();
            });
        }

        @Override
        public int getItemCount() { return userIds.size(); }
    }

    // ── Invitable adapter ─────────────────────────────────────────────────────

    private class InvitableAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final List<String> userIds;

        InvitableAdapter(List<String> userIds) { this.userIds = userIds; }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_live_co_stream_user, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            String uid = userIds.get(position);
            bindUser(holder, uid);
            holder.primaryAction.setText(R.string.live_co_stream_invite);
            holder.secondaryAction.setVisibility(View.GONE);
            holder.primaryAction.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                holder.primaryAction.setEnabled(false);
                holder.primaryAction.setText(R.string.live_co_stream_invite_sent);
                LiveStreamingKit.getInstance().inviteCoStream(uid);
                Toast.makeText(requireContext(), R.string.live_co_stream_invite_sent, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() { return userIds.size(); }
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
}
