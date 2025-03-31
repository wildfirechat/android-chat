/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.viewholder.header;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.JoinGroupRequestValue;
import cn.wildfire.chat.kit.service.IMService;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback5;


@SuppressWarnings("unused")
public class JoinGroupRequestViewHolder extends HeaderViewHolder<JoinGroupRequestValue> {
    TextView unreadRequestCountTextView;
    private JoinGroupRequestValue value;


    public JoinGroupRequestViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
    }

    private void bindViews(View itemView) {
        unreadRequestCountTextView = itemView.findViewById(R.id.unreadGroupRequestCountTextView);
    }

    @Override
    public void onBind(JoinGroupRequestValue value) {
        this.value = value;
        IMService.Instance().getGroupApplyCount(ChatManager.Instance().getUserId(), new GeneralCallback5<Integer>() {
            @Override
            public void onSuccess(Integer count) {
                if (count > 0) {
                    unreadRequestCountTextView.setVisibility(View.VISIBLE);
                    unreadRequestCountTextView.setText("" + count);
                } else {
                    unreadRequestCountTextView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFail(int errorCode) {

            }
        });

    }
}
