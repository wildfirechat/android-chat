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
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 网盘文件列表适配器
 */
public class PanFileListAdapter extends RecyclerView.Adapter<PanFileListAdapter.ViewHolder> {
    
    private List<PanFile> files = new ArrayList<>();
    private OnFileClickListener onFileClickListener;
    
    public interface OnFileClickListener {
        void onFileClick(PanFile file);
        void onFileLongClick(PanFile file);
    }
    
    public void setOnFileClickListener(OnFileClickListener listener) {
        this.onFileClickListener = listener;
    }
    
    public void setFiles(List<PanFile> files) {
        this.files = files;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pan_file, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PanFile file = files.get(position);
        holder.bind(file);
    }
    
    @Override
    public int getItemCount() {
        return files.size();
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
                if (position != RecyclerView.NO_POSITION && onFileClickListener != null) {
                    onFileClickListener.onFileClick(files.get(position));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onFileClickListener != null) {
                    onFileClickListener.onFileLongClick(files.get(position));
                    return true;
                }
                return false;
            });
        }
        
        void bind(PanFile file) {
            nameTextView.setText(file.getName());
            
            if (file.isFolder()) {
                // 文件夹
                iconImageView.setImageResource(R.mipmap.ic_file_type_unknown);
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
