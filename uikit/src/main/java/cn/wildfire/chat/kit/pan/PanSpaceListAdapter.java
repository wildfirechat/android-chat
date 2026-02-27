/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 网盘空间列表适配器
 */
public class PanSpaceListAdapter extends RecyclerView.Adapter<PanSpaceListAdapter.ViewHolder> {
    
    private List<PanSpace> spaces = new ArrayList<>();
    private OnSpaceClickListener onSpaceClickListener;
    private String userPublicSpaceName; // 自定义用户公共空间名称（如"XXX的公共空间"）
    
    public interface OnSpaceClickListener {
        void onSpaceClick(PanSpace space, String displayName);
    }
    
    public void setOnSpaceClickListener(OnSpaceClickListener listener) {
        this.onSpaceClickListener = listener;
    }
    
    public void setSpaces(List<PanSpace> spaces) {
        this.spaces = spaces;
        notifyDataSetChanged();
    }
    
    /**
     * 设置用户公共空间显示名称（用于查看他人空间时）
     * @param name 如"XXX的公共空间"
     */
    public void setUserPublicSpaceName(String name) {
        this.userPublicSpaceName = name;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pan_space, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanSpace space = spaces.get(position);
        holder.bind(space);
    }
    
    @Override
    public int getItemCount() {
        return spaces.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconImageView;
        TextView nameTextView;
        TextView descTextView;
        ProgressBar quotaProgressBar;
        TextView quotaTextView;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            quotaProgressBar = itemView.findViewById(R.id.quotaProgressBar);
            quotaTextView = itemView.findViewById(R.id.quotaTextView);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onSpaceClickListener != null) {
                    PanSpace space = spaces.get(position);
                    // 如果有自定义公共空间名称，传递它
                    String displayName = null;
                    if (space.getSpaceType() != null && space.getSpaceType() == 2 && userPublicSpaceName != null) {
                        displayName = userPublicSpaceName;
                    }
                    onSpaceClickListener.onSpaceClick(space, displayName);
                }
            });
        }
        
        void bind(PanSpace space) {
            // 如果是用户公共空间且设置了自定义名称，则使用自定义名称
            if (space.getSpaceType() != null && space.getSpaceType() == 2 && userPublicSpaceName != null) {
                nameTextView.setText(userPublicSpaceName);
            } else {
                nameTextView.setText(space.getDisplayName());
            }
            
            // 根据空间类型设置图标
            int iconRes = R.mipmap.ic_file_type_unknown;
            if (space.getSpaceType() != null) {
                switch (space.getSpaceType()) {
                    case 1: // GLOBAL_PUBLIC
                        iconRes = R.mipmap.ic_file_type_unknown;
                        descTextView.setText(R.string.pan_space_global_desc);
                        break;
                    case 2: // USER_PUBLIC
                        iconRes = R.mipmap.ic_file_type_unknown;
                        if (userPublicSpaceName != null) {
                            descTextView.setText(userPublicSpaceName);
                        } else {
                            descTextView.setText(R.string.pan_space_public_desc);
                        }
                        break;
                    case 3: // USER_PRIVATE
                        iconRes = R.mipmap.ic_file_type_unknown;
                        descTextView.setText(R.string.pan_space_private_desc);
                        break;
                }
            }
            iconImageView.setImageResource(iconRes);
            
            // 配额显示
            if (space.getTotalQuota() != null && space.getTotalQuota() > 0) {
                int percent = space.getUsagePercent();
                quotaProgressBar.setProgress(percent);
                quotaTextView.setText(formatQuota(space.getUsedQuota()) + " / " + formatQuota(space.getTotalQuota()));
            } else {
                quotaProgressBar.setProgress(0);
                quotaTextView.setText(R.string.pan_quota_unlimited);
            }
        }
        
        private String formatQuota(Long bytes) {
            if (bytes == null) return "0 B";
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
            if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)) + " MB";
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
