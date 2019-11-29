package cn.wildfire.chat.app.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.app.main.model.MainModel;
import cn.wildfire.chat.app.setting.SettingActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

public class MeFragment extends Fragment {

    @BindView(R.id.meLinearLayout)
    LinearLayout meLinearLayout;
    @BindView(R.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R.id.nameTextView)
    TextView nameTextView;
    @BindView(R.id.accountTextView)
    TextView accountTextView;

    @BindView(R.id.notificationOptionItemView)
    OptionItemView notificationOptionItem;

    @BindView(R.id.passwordOptionItemView)
    OptionItemView passwordOptionItemView;

    @BindView(R.id.settintOptionItemView)
    OptionItemView settingOptionItem;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    private Observer<List<UserInfo>> userInfoLiveDataObserver = new Observer<List<UserInfo>>() {
        @Override
        public void onChanged(@Nullable List<UserInfo> userInfos) {
            if (userInfos == null) {
                return;
            }
            for (UserInfo info : userInfos) {
                if (info.uid.equals(userViewModel.getUserId())) {
                    userInfo = info;
                    updateUserInfo(userInfo);
                    break;
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment_me, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void updateUserInfo(UserInfo userInfo) {
        Glide.with(this).load(userInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop()).into(portraitImageView);
        nameTextView.setText(userInfo.displayName);
        accountTextView.setText("账号: " + userInfo.name);
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.getUserInfoAsync(userViewModel.getUserId(), true)
                .observe(this, info -> {
                    userInfo = info;
                    if (userInfo != null) {
                        updateUserInfo(userInfo);
                    }
                });
        userViewModel.userInfoLiveData().observeForever(userInfoLiveDataObserver);

        if(MainModel.clientConfig.getIsOpenAdmin().equals("0")) {
            notificationOptionItem.setVisibility(View.GONE);
        }else{
            notificationOptionItem.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userViewModel.userInfoLiveData().removeObserver(userInfoLiveDataObserver);
    }
    @OnClick(R.id.notificationOptionItemView)
    void showAdmin(){
        //加载管理页面
        WfcWebViewActivity.loadUrl(getContext(), UIUtils.getString(R.string.app_admin), MainModel.clientConfig.getApiAdmin());
    }
    @OnClick(R.id.passwordOptionItemView)
    void showPasswordOption(){
        //加载修改密码页面
        WfcWebViewActivity.loadUrl(getContext(), UIUtils.getString(R.string.passwordOption), MainModel.clientConfig.getPasswdsoupprt());
    }

    @OnClick(R.id.meLinearLayout)
    void showMyInfo() {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    @OnClick(R.id.settintOptionItemView)
    void setting() {
        Intent intent = new Intent(getActivity(), SettingActivity.class);
        startActivity(intent);
    }
}

