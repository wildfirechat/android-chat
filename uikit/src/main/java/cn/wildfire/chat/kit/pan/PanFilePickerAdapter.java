/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 网盘文件选择器适配器
 * 支持显示空间列表和文件列表
 */
public class PanFilePickerAdapter extends RecyclerView.Adapter<PanFilePickerAdapter.ViewHolder> {
    
    public static final int TYPE_SPACE = 1;
    public static final int TYPE_FILE = 2;
    
    private List<PanSpace> spaces = new ArrayList<>();
    private List<PanFile> files = new ArrayList<>();
    private List<PanFile> selectedFiles = new ArrayList<>();
    private int currentViewType = TYPE_SPACE;  // 当前显示类型
    
    private OnItemClickListener onItemClickListener;
    
    public interface OnItemClickListener {
        void onSpaceClick(PanSpace space);
        void onFileClick(PanFile file);
        void onFileCheckChanged(PanFile file, boolean isChecked);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }
    
    /**
     * 显示空间列表
     */
    public void setSpaces(List<PanSpace> spaces) {
        this.spaces = spaces != null ? spaces : new ArrayList<>();
        this.files = new ArrayList<>();
        this.currentViewType = TYPE_SPACE;
        notifyDataSetChanged();
    }
    
    /**
     * 显示文件列表
     */
    public void setFiles(List<PanFile> files, List<PanFile> selectedFiles) {
        this.files = files != null ? files : new ArrayList<>();
        this.spaces = new ArrayList<>();
        this.selectedFiles = selectedFiles != null ? selectedFiles : new ArrayList<>();
        this.currentViewType = TYPE_FILE;
        notifyDataSetChanged();
    }
    
    /**
     * 更新选中状态
     */
    public void updateSelection(List<PanFile> selectedFiles) {
        this.selectedFiles = selectedFiles != null ? selectedFiles : new ArrayList<>();
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
            .inflate(R.layout.item_pan_file_picker, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (currentViewType == TYPE_SPACE && position < spaces.size()) {
            holder.bindSpace(spaces.get(position));
        } else if (currentViewType == TYPE_FILE && position < files.size()) {
            holder.bindFile(files.get(position));
        }
    }
    
    @Override
    public int getItemCount() {
        if (currentViewType == TYPE_SPACE) {
            return spaces.size();
        } else {
            return files.size();
        }
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        TextView infoTextView;
        CheckBox checkBox;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            infoTextView = itemView.findViewById(R.id.infoTextView);
            checkBox = itemView.findViewById(R.id.checkBox);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                
                if (currentViewType == TYPE_SPACE && position < spaces.size()) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onSpaceClick(spaces.get(position));
                    }
                } else if (currentViewType == TYPE_FILE && position < files.size()) {
                    PanFile file = files.get(position);
                    if (file.isFolder()) {
                        if (onItemClickListener != null) {
                            onItemClickListener.onFileClick(file);
                        }
                    } else {
                        // 文件点击时切换选择状态
                        toggleFileSelection(file);
                    }
                }
            });
            
            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;
                
                if (currentViewType == TYPE_FILE && position < files.size()) {
                    PanFile file = files.get(position);
                    toggleFileSelection(file);
                }
            });
        }
        
        private void toggleFileSelection(PanFile file) {
            if (onItemClickListener != null) {
                boolean isSelected = isFileSelected(file);
                onItemClickListener.onFileCheckChanged(file, !isSelected);
            }
        }
        
        void bindSpace(PanSpace space) {
            nameTextView.setText(space.getDisplayName());
            
            // 所有空间使用文件夹图标（如果没有专门的图标）
            iconImageView.setImageResource(R.mipmap.ic_file);
            
            // 显示空间配额信息
            long usedQuota = space.getUsedQuota() != null ? space.getUsedQuota() : 0;
            long totalQuota = space.getTotalQuota() != null ? space.getTotalQuota() : 0;
            if (totalQuota > 0) {
                String usedStr = FileUtils.getReadableFileSize((int) usedQuota);
                String totalStr = FileUtils.getReadableFileSize((int) totalQuota);
                infoTextView.setText(itemView.getContext().getString(R.string.pan_quota_format, usedStr, totalStr));
            } else {
                infoTextView.setText(R.string.pan_quota_unlimited);
            }
            
            checkBox.setVisibility(View.GONE);
        }
        
        void bindFile(PanFile file) {
            nameTextView.setText(file.getName());
            
            if (file.isFolder()) {
                // 文件夹
                iconImageView.setImageResource(R.mipmap.ic_file);
                int childCount = file.getChildCount() != null ? file.getChildCount() : 0;
                infoTextView.setText(itemView.getContext().getString(R.string.pan_folder_item_count, childCount));
                checkBox.setVisibility(View.GONE);
            } else {
                // 文件
                iconImageView.setImageResource(FileUtils.getFileTypeImageResId(file.getName()));
                String size = FileUtils.getReadableFileSize(file.getSize() != null ? file.getSize().intValue() : 0);
                infoTextView.setText(size);
                
                // 显示选择框
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(isFileSelected(file));
            }
        }
        
        private boolean isFileSelected(PanFile file) {
            for (PanFile selected : selectedFiles) {
                if (selected.getId().equals(file.getId())) {
                    return true;
                }
            }
            return false;
        }
    }
}
