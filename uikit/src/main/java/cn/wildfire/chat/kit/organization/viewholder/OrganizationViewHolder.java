/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Organization;

public class OrganizationViewHolder extends OrganizationEntityViewHolder<Organization> {
    private TextView nameTextView;
    private ImageView portraitImageView;

    public OrganizationViewHolder(@NonNull View itemView) {
        super(itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
        this.portraitImageView = itemView.findViewById(R.id.portraitImageView);
    }

    @Override
    public void onBind(Organization organization) {
        this.nameTextView.setText(organization.name + "(" + organization.memberCount + ")");
    }
}
