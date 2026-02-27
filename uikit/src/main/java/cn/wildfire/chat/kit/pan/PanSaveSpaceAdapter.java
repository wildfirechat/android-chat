/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
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
 * 保存到网盘 - 空间选择适配器
 */
public class PanSaveSpaceAdapter extends RecyclerView.Adapter<PanSaveSpaceAdapter.ViewHolder> {
    
    private List<PanSpace> spaces = new ArrayList<>();
    private OnSpaceClickListener onSpaceClickListener;
    
    public interface OnSpaceClickListener {
        void onSpaceClick(PanSpace space);
    }
    
    public void setOnSpaceClickListener(OnSpaceClickListener listener) {
        this.onSpaceClickListener = listener;
    }
    
    public void setSpaces(List<PanSpace> spaces) {
        this.spaces = spaces;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pan_save_space, parent, false);
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
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            descTextView = itemView.findViewById(R.id.descTextView);
            
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onSpaceClickListener != null) {
                    onSpaceClickListener.onSpaceClick(spaces.get(position));
                }
            });
        }
        
        void bind(PanSpace space) {
            nameTextView.setText(space.getDisplayName());
            
            // 根据空间类型设置图标和描述
            int iconRes = R.mipmap.ic_file_type_unknown;
            String desc = "";
            if (space.getSpaceType() != null) {
                switch (space.getSpaceType()) {
                    case 1: // GLOBAL_PUBLIC
                        iconRes = R.mipmap.ic_file_type_unknown;
                        desc = itemView.getContext().getString(R.string.pan_space_global_desc);
                        break;
                    case 2: // USER_PUBLIC
                        iconRes = R.mipmap.ic_file_type_unknown;
                        desc = itemView.getContext().getString(R.string.pan_space_public_desc);
                        break;
                    case 3: // USER_PRIVATE
                        iconRes = R.mipmap.ic_file_type_unknown;
                        desc = itemView.getContext().getString(R.string.pan_space_private_desc);
                        break;
                }
            }
            iconImageView.setImageResource(iconRes);
            descTextView.setText(desc);
        }
    }
}
