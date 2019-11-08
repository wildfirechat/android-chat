package cn.wildfire.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.viewholder.CheckableUserViewHolder;
import cn.wildfirechat.chat.R;

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


    public void updateUserStatus(UIUserInfo userInfo) {
        if (users != null && userInfo != null) {
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getUserInfo().uid.equals(userInfo.getUserInfo().uid)) {
                    users.set(i, userInfo);
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
