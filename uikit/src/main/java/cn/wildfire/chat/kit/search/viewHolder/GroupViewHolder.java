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
        nameTextView.setText(groupSearchResult.groupInfo.name);
        String portrait = groupSearchResult.groupInfo.portrait;
        if (TextUtils.isEmpty(portrait)) {
            portrait = ImageUtils.getGroupGridPortrait(fragment.getContext(), groupSearchResult.groupInfo.target, 60);
        }
        GlideApp
            .with(fragment)
            .load(portrait)
            .placeholder(R.mipmap.ic_group_cheat)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(fragment.getContext(), 4)))
            .into(portraitImageView);

        String desc = "";
        switch (groupSearchResult.marchedType) {
            case 0:
                desc = "群名称包含: " + keyword;
                break;
            case 1:
                desc = "群成员包含: " + keyword;
                break;
            case 2:
                desc = "群名称和群成员都包含: " + keyword;
                break;
            default:
                break;
        }
        descTextView.setText(desc);
    }
}
