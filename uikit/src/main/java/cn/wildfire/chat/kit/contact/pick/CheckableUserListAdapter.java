/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.viewholder.CheckableUserViewHolder;

public class CheckableUserListAdapter extends UserListAdapter {
    private int maxCheckCount;

    public CheckableUserListAdapter(Fragment fragment) {
        super(fragment);
    }

    public void setMaxCheckCount(int maxCheckCount) {
        this.maxCheckCount = maxCheckCount;
    }

    public int getMaxCheckCount() {
        return maxCheckCount;
    }


    public void updateUserStatus(List<UIUserInfo> checkedUserInfo) {
        if (users != null) {
            UIUserInfo ui;
            boolean found;
            for (int i = 0; i < users.size(); i++) {
                ui = users.get(i);
                found = false;
                for (UIUserInfo cu : checkedUserInfo) {
                    if (ui.getUserInfo().uid.equals(cu.getUserInfo().uid)) {
                        if (!ui.isChecked()) {
                            ui.setChecked(true);
                            notifyItemChanged(headerCount() + i);
                        }
                        found = true;
                        break;
                    }
                }
                if (!found && ui.isChecked()) {
                    ui.setChecked(false);
                    notifyItemChanged(headerCount() + i);
                }
            }
        }
    }

    public List<UIUserInfo> getCheckedContacts() {
        List<UIUserInfo> checkedContacts = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            return checkedContacts;
        }
        for (UIUserInfo userInfo : users) {
            if (userInfo.isChecked()) {
                checkedContacts.add(userInfo);
            }
        }
        return checkedContacts;
    }

    @Override
    protected RecyclerView.ViewHolder onCreateContactViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.contact_item_contact, parent, false);
        CheckableUserViewHolder viewHolder = new CheckableUserViewHolder(fragment, this, itemView);

        itemView.setOnClickListener(v -> {
            UIUserInfo userInfo = viewHolder.getBindContact();
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(userInfo);
            }
        });
        return viewHolder;
    }
}
