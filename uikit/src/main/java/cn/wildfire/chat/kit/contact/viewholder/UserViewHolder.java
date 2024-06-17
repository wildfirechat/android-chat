/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserViewModel;

public class UserViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected UserListAdapter adapter;
    ImageView portraitImageView;
    TextView nameTextView;
    TextView descTextView;
    protected TextView categoryTextView;

    protected UIUserInfo userInfo;

    public UserViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
        categoryTextView = itemView.findViewById(R.id.categoryTextView);
    }

    public void onBind(UIUserInfo userInfo) {
        this.userInfo = userInfo;
        if (userInfo.isShowCategory()) {
            categoryTextView.setVisibility(View.VISIBLE);
            categoryTextView.setText(userInfo.getCategory());
        } else {
            categoryTextView.setVisibility(View.GONE);
        }
        UserViewModel userViewModel = ViewModelProviders.of(fragment).get(UserViewModel.class);
        nameTextView.setText(userViewModel.getUserDisplayNameEx(userInfo.getUserInfo()));
        if (!TextUtils.isEmpty(userInfo.getDesc())) {
            descTextView.setVisibility(View.VISIBLE);
            descTextView.setText(userInfo.getDesc());
        } else {
            descTextView.setVisibility(View.GONE);
        }
        Glide.with(fragment).load(userInfo.getUserInfo().portrait).placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(10))
            .into(portraitImageView);
    }

    public UIUserInfo getBindContact() {
        return userInfo;
    }
}
