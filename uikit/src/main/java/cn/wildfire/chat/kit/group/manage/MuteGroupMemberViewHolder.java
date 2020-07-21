package cn.wildfire.chat.kit.group.manage;

import android.view.View;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;

public class MuteGroupMemberViewHolder extends FooterViewHolder<FooterValue> {

    public MuteGroupMemberViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(FooterValue footerValue) {
        // do nothing
    }
}
