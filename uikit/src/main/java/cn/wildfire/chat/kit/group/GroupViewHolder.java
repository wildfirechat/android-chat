/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    private GroupListAdapter adapter;
    ImageView portraitImageView;
    TextView nameTextView;
    TextView categoryTextView;
    View dividerLine;

    protected GroupInfo groupInfo;

    public GroupViewHolder(Fragment fragment, GroupListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        categoryTextView = itemView.findViewById(R.id.categoryTextView);
        dividerLine = itemView.findViewById(R.id.dividerLine);
    }

    // TODO hide the last diver line
    public void onBind(GroupInfo groupInfo) {
        this.groupInfo = groupInfo;
        categoryTextView.setVisibility(View.GONE);
        nameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
        Glide.with(fragment).load(this.groupInfo.portrait).placeholder(R.mipmap.ic_group_chat).into(portraitImageView);
    }

    public GroupInfo getGroupInfo() {
        return groupInfo;
    }
}
