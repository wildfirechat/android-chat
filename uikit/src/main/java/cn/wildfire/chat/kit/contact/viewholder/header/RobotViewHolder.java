/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder.header;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;

/**
 * 联系人/选人页最外层的「机器人」入口，与组织架构入口并列，不进入组织树。
 */
public class RobotViewHolder extends HeaderViewHolder<HeaderValue> {
    private final TextView nameTextView;

    public RobotViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        this.nameTextView = itemView.findViewById(R.id.nameTextView);
    }

    @Override
    public void onBind(HeaderValue headerValue) {
        this.nameTextView.setText(R.string.org_robot);
    }
}
