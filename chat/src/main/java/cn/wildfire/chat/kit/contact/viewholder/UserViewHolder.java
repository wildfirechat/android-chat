package cn.wildfire.chat.kit.contact.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;

public class UserViewHolder extends RecyclerView.ViewHolder {
    protected Fragment fragment;
    protected UserListAdapter adapter;
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.nameTextView)
    TextView nameTextView;
    @Bind(R.id.categoryTextView)
    TextView categoryTextView;

    protected UIUserInfo userInfo;

    public UserViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(itemView);
        this.fragment = fragment;
        this.adapter = adapter;
        ButterKnife.bind(this, itemView);
    }

    public void onBind(UIUserInfo userInfo) {
        this.userInfo = userInfo;
        if (userInfo.isShowCategory()) {
            categoryTextView.setVisibility(View.VISIBLE);
            categoryTextView.setText(userInfo.getCategory());
        } else {
            categoryTextView.setVisibility(View.GONE);
        }
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        nameTextView.setText(userViewModel.getUserDisplayName(userInfo.getUserInfo()));
        GlideApp.with(fragment).load(userInfo.getUserInfo().portrait).error(R.mipmap.default_header).into(portraitImageView);
    }

    public UIUserInfo getBindContact() {
        return userInfo;
    }
}
