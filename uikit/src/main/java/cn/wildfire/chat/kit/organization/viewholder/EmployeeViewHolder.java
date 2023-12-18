/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.organization.model.Employee;

public class EmployeeViewHolder extends OrganizationEntityViewHolder<Employee> {
    private TextView nameTextView;
    private ImageView portraitImageView;

    public EmployeeViewHolder(@NonNull View itemView) {
        super(itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
        this.portraitImageView = itemView.findViewById(R.id.portraitImageView);
    }

    @Override
    public void onBind(Employee employee) {
        this.nameTextView.setText(employee.name);
        Glide.with(portraitImageView).load(employee.portraitUrl).placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(10))
            .into(portraitImageView);
    }
}
