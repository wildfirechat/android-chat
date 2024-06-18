/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.mesh.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.DomainInfo;

public class DomainViewHolder extends RecyclerView.ViewHolder {
    private TextView nameTextView;
    private ImageView portraitImageView;

    public DomainViewHolder(@NonNull View itemView) {
        super(itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
        this.portraitImageView = itemView.findViewById(R.id.portraitImageView);
    }

    public void onBind(DomainInfo domainInfo) {
        this.nameTextView.setText(domainInfo.name);
    }
}
