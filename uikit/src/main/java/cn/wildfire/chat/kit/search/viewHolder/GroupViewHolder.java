/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupSearchResult;

public class GroupViewHolder extends ResultItemViewHolder<GroupSearchResult> {
    ImageView portraitImageView;
    TextView nameTextView;
    TextView descTextView;

    public GroupViewHolder(Fragment fragment, View itemView) {
        super(fragment, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        portraitImageView = itemView.findViewById(R.id.portraitImageView);
        nameTextView = itemView.findViewById(R.id.nameTextView);
        descTextView = itemView.findViewById(R.id.descTextView);
    }


    @Override
    public void onBind(String keyword, GroupSearchResult groupSearchResult) {
        GroupInfo groupInfo = groupSearchResult.groupInfo;
        nameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
        String portrait = groupSearchResult.groupInfo.portrait;
        Glide
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_group_chat)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
            .into(portraitImageView);

        String desc = "";
        if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Group_Name_Mask) > 0) {
            desc = fragment.getString(R.string.group_name_contains, keyword);
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Group_Remark_Mask) > 0) {
            desc = fragment.getString(R.string.group_remark_contains, keyword);
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Member_Name_Mask) > 0) {
            desc = fragment.getString(R.string.group_member_name_contains, keyword);
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Member_Alias_Mask) > 0) {
            desc = fragment.getString(R.string.group_member_alias_contains, keyword);
        }
        descTextView.setText(desc);
    }
}
