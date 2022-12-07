/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.search.viewHolder;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupSearchResult;

public class GroupViewHolder extends ResultItemViewHolder<GroupSearchResult> {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;

    public GroupViewHolder(Fragment fragment, View itemView) {
        super(fragment, itemView);
        ButterKnife.bind(this, itemView);
    }


    @Override
    public void onBind(String keyword, GroupSearchResult groupSearchResult) {
        GroupInfo groupInfo = groupSearchResult.groupInfo;
        nameTextView.setText(!TextUtils.isEmpty(groupInfo.remark) ? groupInfo.remark : groupInfo.name);
        String portrait = groupSearchResult.groupInfo.portrait;
        if (TextUtils.isEmpty(portrait)) {
            portrait = ImageUtils.getGroupGridPortrait(fragment.getContext(), groupSearchResult.groupInfo.target, 60);
        }
        GlideApp
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_group_chat)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
            .into(portraitImageView);

        String desc = "";
        if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Group_Name_Mask) > 0) {
            desc = "群名称包含: " + keyword;
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Group_Remark_Mask) > 0) {
            desc = "群备注包含: " + keyword;
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Member_Name_Mask) > 0) {
            desc = "群成员名包含: " + keyword;
        } else if ((groupSearchResult.marchedType & GroupSearchResult.GroupSearchMarchTypeMask.Member_Alias_Mask) > 0) {
            desc = "群成员备注包含: " + keyword;
        }
        descTextView.setText(desc);
    }
}
