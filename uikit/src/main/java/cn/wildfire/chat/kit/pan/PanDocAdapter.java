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
import cn.wildfire.chat.kit.pan.model.PanDocEntry;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 在线文档首页适配器：最近打开 / 共享给我。
 */
public class PanDocAdapter extends RecyclerView.Adapter<PanDocAdapter.ViewHolder> {

    public interface OnDocClickListener {
        void onDocClick(PanDocEntry entry);
    }

    public interface OnDocLongClickListener {
        void onDocLongClick(PanDocEntry entry);
    }

    private final List<PanDocEntry> entries = new ArrayList<>();
    private final OnDocClickListener clickListener;
    private final OnDocLongClickListener longClickListener;

    public PanDocAdapter(OnDocClickListener clickListener, OnDocLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setEntries(List<PanDocEntry> list) {
        entries.clear();
        if (list != null) {
            entries.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pan_doc, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanDocEntry entry = entries.get(position);
        holder.nameTextView.setText(entry.file.name);
        String desc = entry.time;
        if (!entry.canEdit) {
            desc = (desc == null || desc.isEmpty() ? "" : desc + " · ") + "只读";
        }
        holder.descTextView.setText(desc);
        holder.iconImageView.setImageResource(FileUtils.getFileTypeImageResId(entry.file.name));
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDocClick(entry);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onDocLongClick(entry);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return entries.size();
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
