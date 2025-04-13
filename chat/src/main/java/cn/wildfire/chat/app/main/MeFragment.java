/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.app.setting.AccountActivity;
import cn.wildfire.chat.app.setting.SettingActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.utils.LocaleUtils;
import cn.wildfire.chat.kit.conversation.file.FileRecordListActivity;
import cn.wildfire.chat.kit.favorite.FavoriteListActivity;
import cn.wildfire.chat.kit.settings.MessageNotifySettingActivity;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class MeFragment extends Fragment {

    LinearLayout meLinearLayout;
    ImageView portraitImageView;
    TextView nameTextView;
    TextView accountTextView;

    OptionItemView notificationOptionItem;

    OptionItemView settingOptionItem;

    OptionItemView fileRecordOptionItem;

    OptionItemView conversationOptionItem;

    private UserViewModel userViewModel;
    private UserInfo userInfo;
    private boolean isVisibleToUser;

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
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    @Override

    public void setMenuVisibility(boolean isvisible) {
        super.setMenuVisibility(isvisible);
        this.isVisibleToUser = isvisible;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.isVisibleToUser && userViewModel != null) {
            userViewModel.getUserInfoAsync(userViewModel.getUserId(), true)
                .observe(getViewLifecycleOwner(), info -> {
                    userInfo = info;
                    if (userInfo != null) {
                        updateUserInfo(userInfo);
                    }
                });
        }
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.meLinearLayout).setOnClickListener(v -> showMyInfo());
        view.findViewById(R.id.favOptionItemView).setOnClickListener(v -> fav());
        view.findViewById(R.id.accountOptionItemView).setOnClickListener(v -> account());
        view.findViewById(R.id.fileRecordOptionItemView).setOnClickListener(v -> files());
        view.findViewById(R.id.themeOptionItemView).setOnClickListener(v -> theme());
        view.findViewById(R.id.languageOptionItemView).setOnClickListener(v -> selectLanguage());
        view.findViewById(R.id.settingOptionItemView).setOnClickListener(v -> setting());
        view.findViewById(R.id.notificationOptionItemView).setOnClickListener(v -> msgNotifySetting());
        view.findViewById(R.id.conversationOptionItemView).setOnClickListener(v -> conversationSetting());
    }

    private void bindViews(View view) {
        meLinearLayout = view.findViewById(R.id.meLinearLayout);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        nameTextView = view.findViewById(R.id.nameTextView);
        accountTextView = view.findViewById(R.id.accountTextView);
        notificationOptionItem = view.findViewById(R.id.notificationOptionItemView);
        settingOptionItem = view.findViewById(R.id.settingOptionItemView);
        conversationOptionItem = view.findViewById(R.id.conversationOptionItemView);
        fileRecordOptionItem = view.findViewById(R.id.fileRecordOptionItemView);
    }

    private void updateUserInfo(UserInfo userInfo) {
        RequestOptions options = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 10)));
        Glide.with(this)
            .load(userInfo.portrait)
            .apply(options)
            .into(portraitImageView);
        nameTextView.setText(userInfo.displayName);
        accountTextView.setText(getString(R.string.account_label, userInfo.name));
    }

    private void init() {
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.getUserInfoAsync(userViewModel.getUserId(), true)
            .observe(getViewLifecycleOwner(), info -> {
                userInfo = info;
                if (userInfo != null) {
                    updateUserInfo(userInfo);
                }
            });
        userViewModel.userInfoLiveData().observeForever(userInfoLiveDataObserver);
        if (ChatManager.Instance().isCommercialServer()) {
            fileRecordOptionItem.setVisibility(View.VISIBLE);
        } else {
            fileRecordOptionItem.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userViewModel.userInfoLiveData().removeObserver(userInfoLiveDataObserver);
    }

    void showMyInfo() {
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    void fav() {
        Intent intent = new Intent(getActivity(), FavoriteListActivity.class);
        startActivity(intent);
    }

    void account() {
        Intent intent = new Intent(getActivity(), AccountActivity.class);
        startActivity(intent);
    }

    void files() {
        Intent intent = new Intent(getActivity(), FileRecordListActivity.class);
        startActivity(intent);
    }


    void theme() {
        SharedPreferences sp = getActivity().getSharedPreferences("wfc_kit_config", Context.MODE_PRIVATE);
        boolean darkTheme = sp.getBoolean("darkTheme", true);
        new MaterialDialog.Builder(getContext()).items(R.array.themes).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0 && darkTheme) {
                    sp.edit().putBoolean("darkTheme", false).apply();
                    restart();
                    return;
                }
                if (position == 1 && !darkTheme) {
                    sp.edit().putBoolean("darkTheme", true).apply();
                    restart();
                }
            }
        }).show();
    }

    void selectLanguage() {
        String currentLanguage = LocaleUtils.getLanguage(getContext());
        boolean isChinese = LocaleUtils.LANGUAGE_CHINESE.equals(currentLanguage);

        new MaterialDialog.Builder(getContext()).items(R.array.languages).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0 && !isChinese) {
                    // 选择中文
                    changeLanguage(LocaleUtils.LANGUAGE_CHINESE);
                    return;
                }
                if (position == 1 && isChinese) {
                    // 选择英文
                    changeLanguage(LocaleUtils.LANGUAGE_ENGLISH);
                }
            }
        }).show();
    }

    private void changeLanguage(String languageCode) {
        LocaleUtils.setLocale(getContext(), languageCode);
        // 重启应用以应用语言更改
        restart();
    }

    void setting() {
        Intent intent = new Intent(getActivity(), SettingActivity.class);
        startActivity(intent);
    }

    void msgNotifySetting() {
        Intent intent = new Intent(getActivity(), MessageNotifySettingActivity.class);
        startActivity(intent);
    }

    void conversationSetting() {
        // TODO
        // 设置背景等
    }

    private void restart() {
        // 创建一个指向主活动的新意图，并清除任务栈
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // 结束当前活动
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
