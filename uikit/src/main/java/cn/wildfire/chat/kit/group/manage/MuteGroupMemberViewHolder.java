package cn.wildfire.chat.kit.group.manage;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.FooterValue;
import cn.wildfire.chat.kit.contact.viewholder.footer.FooterViewHolder;
import cn.wildfirechat.model.GroupInfo;

public class MuteGroupMemberViewHolder extends FooterViewHolder<FooterValue> {
    TextView titleTextView;

    public MuteGroupMemberViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        titleTextView = itemView.findViewById(R.id.nameTextView);
    }

    @Override
    public void onBind(FooterValue footerValue) {
        GroupInfo groupInfo = (GroupInfo) footerValue.getValue();
        titleTextView.setText(groupInfo.mute == 0 ? "群成员禁言" : "发言白名单");
    }
}
