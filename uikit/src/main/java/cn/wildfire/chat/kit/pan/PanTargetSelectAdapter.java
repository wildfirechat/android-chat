/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 目标选择适配器（移动/复制用）
 * 点击空间或文件夹是进入，不是选中
 */
public class PanTargetSelectAdapter extends RecyclerView.Adapter<PanTargetSelectAdapter.ViewHolder> {
    
    public static final int TYPE_SPACE = 1;
    public static final int TYPE_FOLDER = 2;
    
    private List<PanSpace> spaces = new ArrayList<>();
    private List<PanFile> folders = new ArrayList<>();
    private int currentViewType = TYPE_SPACE;
    
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onSpaceClick(PanSpace space);
        void onFolderClick(PanFile folder);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    /**
     * 设置空间列表
     */
    public void setSpaces(List<PanSpace> spaces) {
        this.spaces = spaces != null ? spaces : new ArrayList<>();
        this.folders = new ArrayList<>();
        this.currentViewType = TYPE_SPACE;
        notifyDataSetChanged();
    }
    
    /**
     * 设置文件夹列表
     */
    public void setFolders(List<PanFile> folders, Long parentId) {
        this.folders = folders != null ? folders : new ArrayList<>();
        this.spaces = new ArrayList<>();
        this.currentViewType = TYPE_FOLDER;
        notifyDataSetChanged();
    }
    
    /**
     * 设置文件列表（显示所有内容，包括文件夹和文件）
     */
    public void setFiles(List<PanFile> files, Long parentId) {
        this.folders = files != null ? files : new ArrayList<>();
        this.spaces = new ArrayList<>();
        this.currentViewType = TYPE_FOLDER;
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemViewType(int position) {
        return currentViewType;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pan_target_space, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (currentViewType == TYPE_SPACE && position < spaces.size()) {
            holder.bindSpace(spaces.get(position));
        } else if (currentViewType == TYPE_FOLDER && position < folders.size()) {
            holder.bindFolder(folders.get(position));
        }
    }
    
    @Override
    public int getItemCount() {
        if (currentViewType == TYPE_SPACE) {
            return spaces.size();
        } else {
            return folders.size();
        }
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        TextView infoTextView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                
                if (currentViewType == TYPE_SPACE && position < spaces.size()) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onSpaceClick(spaces.get(position));
                    }
                } else if (currentViewType == TYPE_FOLDER && position < folders.size()) {
                    PanFile file = folders.get(position);
                    if (file.isFolder()) {
                        // 点击文件夹进入
                        if (onItemClickListener != null) {
                            onItemClickListener.onFolderClick(file);
                        }
                    } else {
                        // 点击文件提示不能进入
                        Toast.makeText(itemView.getContext(), "请选择文件夹或点击粘贴保存到当前目录", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        void bindSpace(PanSpace space) {
            nameTextView.setText(space.getDisplayName());
            iconImageView.setImageResource(R.mipmap.ic_file);
            
            // 显示空间配额
            long usedQuota = space.getUsedQuota() != null ? space.getUsedQuota() : 0;
            long totalQuota = space.getTotalQuota() != null ? space.getTotalQuota() : 0;
            if (totalQuota > 0) {
                String usedStr = FileUtils.getReadableFileSize((int) usedQuota);
                String totalStr = FileUtils.getReadableFileSize((int) totalQuota);
                infoTextView.setText(itemView.getContext().getString(R.string.pan_quota_format, usedStr, totalStr));
            } else {
                infoTextView.setText(R.string.pan_quota_unlimited);
            }
        }
        
        void bindFolder(PanFile file) {
            nameTextView.setText(file.getName());
            
            if (file.isFolder()) {
                // 文件夹
                iconImageView.setImageResource(R.mipmap.ic_file);
                int childCount = file.getChildCount() != null ? file.getChildCount() : 0;
                infoTextView.setText(itemView.getContext().getString(R.string.pan_folder_item_count, childCount));
            } else {
                // 文件
                iconImageView.setImageResource(FileUtils.getFileTypeImageResId(file.getName()));
                String size = FileUtils.getReadableFileSize(file.getSize() != null ? file.getSize().intValue() : 0);
                infoTextView.setText(size);
            }
        }
    }
}
