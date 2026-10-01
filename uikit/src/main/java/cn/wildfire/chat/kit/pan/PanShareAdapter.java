/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.pan.model.PanShare;

/**
 * 分享列表适配器。
 */
public class PanShareAdapter extends RecyclerView.Adapter<PanShareAdapter.ViewHolder> {

    public interface OnShareClickListener {
        void onShareClick(PanShare share);
    }

    private final List<PanShare> shares = new ArrayList<>();
    private final OnShareClickListener clickListener;

    public PanShareAdapter(OnShareClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setShares(List<PanShare> list) {
        shares.clear();
        if (list != null) {
            shares.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pan_share, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanShare share = shares.get(position);
        String name = share.targetName;
        if (name == null || name.isEmpty()) {
            name = share.targetId;
        }
        holder.nameTextView.setText(name);
        String desc = (share.isGroup() ? "群聊" : "联系人");
        if (share.createdByName != null && !share.createdByName.isEmpty()) {
            desc = desc + " · " + share.createdByName + " 分享";
        }
        holder.descTextView.setText(desc);
        holder.permissionTextView.setText(share.canEdit() ? R.string.pan_permission_edit : R.string.pan_permission_view);
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onShareClick(share);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView descTextView;
        TextView permissionTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            permissionTextView = itemView.findViewById(R.id.permissionTextView);
        }
    }
}
