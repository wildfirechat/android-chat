/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.group.JoinGroupFragment;
import cn.wildfire.chat.kit.domain.TGroupJoinRequests;
import cn.wildfirechat.model.UserInfo;

public class JoinGroupRequestListAdapter extends RecyclerView.Adapter<JoinGroupListRequestViewHolder> {
    private List<TGroupJoinRequests> groupJoinRequests;
    private JoinGroupFragment fragment;

    public JoinGroupRequestListAdapter(JoinGroupFragment fragment) {
        this.fragment = fragment;
    }

    public List<TGroupJoinRequests> getGroupJoinRequests() {
        return groupJoinRequests;
    }

    public void setGroupJoinRequests(List<TGroupJoinRequests> joinRequests) {
        this.groupJoinRequests = joinRequests;
    }

    @NonNull
    @Override
    public JoinGroupListRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.contact_item_join_group, parent, false);
        return new JoinGroupListRequestViewHolder(fragment, this, view);
    }

    @Override
    public void onBindViewHolder(@NonNull JoinGroupListRequestViewHolder holder, int position) {
        holder.onBind(groupJoinRequests.get(position));
    }

    public void onUserInfosUpdate(List<UserInfo> userInfos) {
        if (groupJoinRequests == null || groupJoinRequests.isEmpty()) {
            return;
        }
        for (UserInfo info : userInfos) {
            for (int i = 0; i < groupJoinRequests.size(); i++) {
                if (groupJoinRequests.get(i).getApplicantId().equals(info.uid)) {
                    notifyItemChanged(i);
                }
            }
        }
    }


    @Override
    public int getItemCount() {
        return groupJoinRequests == null ? 0 : groupJoinRequests.size();
    }
}
