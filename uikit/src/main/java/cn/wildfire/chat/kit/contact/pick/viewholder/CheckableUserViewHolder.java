package cn.wildfire.chat.kit.contact.pick.viewholder;

import android.view.View;
import android.widget.CheckBox;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.CheckableUserListAdapter;
import cn.wildfire.chat.kit.contact.viewholder.UserViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;

public class CheckableUserViewHolder extends UserViewHolder {
    @BindView(R2.id.checkbox)
    CheckBox checkBox;

    public CheckableUserViewHolder(Fragment fragment, CheckableUserListAdapter adapter, View itemView) {
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
