package cn.wildfire.chat.kit.contact.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import cn.wildfire.chat.kit.contact.ContactAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.viewholder.CheckableContactViewHolder;
import cn.wildfirechat.chat.R;

public class CheckableContactAdapter extends ContactAdapter {
    private int maxCheckCount;

    public CheckableContactAdapter(Fragment fragment) {
        super(fragment);
    }

    public void setMaxCheckCount(int maxCheckCount) {
        this.maxCheckCount = maxCheckCount;
    }

    public int getMaxCheckCount() {
        return maxCheckCount;
    }


    public void updateContactStatus(UIUserInfo userInfo) {
        if (contacts != null && userInfo != null) {
            for (int i = 0; i < contacts.size(); i++) {
                if (contacts.get(i).getUserInfo().uid.equals(userInfo.getUserInfo().uid)) {
                    contacts.set(i, userInfo);
                    notifyItemChanged(headerCount() + i);
                }
            }
        }
    }

    public List<UIUserInfo> getCheckedContacts() {
        List<UIUserInfo> checkedContacts = new ArrayList<>();
        if (contacts == null || contacts.isEmpty()) {
            return checkedContacts;
        }
        for (UIUserInfo userInfo : contacts) {
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
        CheckableContactViewHolder viewHolder = new CheckableContactViewHolder(fragment, this, itemView);

        itemView.setOnClickListener(v -> {
            UIUserInfo userInfo = viewHolder.getBindContact();
            if (onContactClickListener != null) {
                onContactClickListener.onContactClick(userInfo);
            }
        });
        return viewHolder;
    }
}
