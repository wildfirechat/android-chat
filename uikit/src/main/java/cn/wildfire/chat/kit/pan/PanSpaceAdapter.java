/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 网盘空间列表适配器（首页 / 目标选择页共用）。
 */
public class PanSpaceAdapter extends RecyclerView.Adapter<PanSpaceAdapter.ViewHolder> {

    public interface OnSpaceClickListener {
        void onSpaceClick(PanSpace space);
    }

    private final List<PanSpace> spaces = new ArrayList<>();
    private final OnSpaceClickListener listener;

    public PanSpaceAdapter(OnSpaceClickListener listener) {
        this.listener = listener;
    }

    public void setSpaces(List<PanSpace> list) {
        spaces.clear();
        if (list != null) {
            spaces.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pan_space, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanSpace space = spaces.get(position);
        holder.nameTextView.setText(space.name);
        StringBuilder desc = new StringBuilder();
        if (space.isGlobalPublic()) {
            desc.append("公共空间");
        } else if (space.isUserPublic()) {
            desc.append("我的公共空间");
        } else {
            desc.append("我的个人空间");
        }
        if (space.totalQuota > 0) {
            desc.append(" · ").append(PanService.formatPanSize(space.usedQuota))
                .append("/").append(PanService.formatPanSize(space.totalQuota));
        } else {
            desc.append(" · ").append(space.fileCount).append("个文件");
        }
        holder.descTextView.setText(desc.toString());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSpaceClick(space);
            }
        });
    }

    @Override
    public int getItemCount() {
        return spaces.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        TextView descTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
        }
    }
}
