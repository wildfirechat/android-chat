package cn.wildfire.chat.contact.pick.viewholder;

import androidx.fragment.app.Fragment;
import android.view.View;
import android.widget.CheckBox;

import cn.wildfirechat.chat.R;
import cn.wildfire.chat.contact.model.UIUserInfo;
import cn.wildfire.chat.contact.pick.CheckableContactAdapter;
import cn.wildfire.chat.contact.viewholder.ContactViewHolder;

import butterknife.Bind;

public class CheckableContactViewHolder extends ContactViewHolder {
    @Bind(R.id.checkbox)
    CheckBox checkBox;

    public CheckableContactViewHolder(Fragment fragment, CheckableContactAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UIUserInfo userInfo) {
        super.onBind(userInfo);

        checkBox.setVisibility(View.VISIBLE);
        if (!userInfo.isCheckable()) {
            checkBox.setEnabled(false);
            checkBox.setChecked(true);
        } else {
            checkBox.setEnabled(true);
            checkBox.setChecked(userInfo.isChecked());
        }
        checkBox.setEnabled(userInfo.isCheckable());
    }
}
