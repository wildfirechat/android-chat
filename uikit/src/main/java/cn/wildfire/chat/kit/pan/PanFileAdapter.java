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
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 网盘文件列表适配器（目录浏览 / 目标选择页共用）。
 */
public class PanFileAdapter extends RecyclerView.Adapter<PanFileAdapter.ViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(PanFile file);
    }

    public interface OnFileLongClickListener {
        void onFileLongClick(PanFile file);
    }

    private final List<PanFile> files = new ArrayList<>();
    private final OnFileClickListener clickListener;
    private final OnFileLongClickListener longClickListener;

    public PanFileAdapter(OnFileClickListener clickListener, OnFileLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setFiles(List<PanFile> list) {
        files.clear();
        if (list != null) {
            files.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pan_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanFile file = files.get(position);
        holder.nameTextView.setText(file.name);
        if (file.isFolder()) {
            holder.descTextView.setText(file.childCount > 0 ? file.childCount + " 项" : "");
            holder.arrowImageView.setVisibility(View.VISIBLE);
            holder.iconImageView.setImageResource(R.mipmap.ic_settings_file);
        } else {
            holder.descTextView.setText(PanService.formatPanSize(file.size));
            holder.arrowImageView.setVisibility(View.GONE);
            holder.iconImageView.setImageResource(FileUtils.getFileTypeImageResId(file.name));
        }
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onFileClick(file);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onFileLongClick(file);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        ImageView arrowImageView;
        TextView nameTextView;
        TextView descTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            arrowImageView = itemView.findViewById(R.id.arrowImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
        }
    }
}
