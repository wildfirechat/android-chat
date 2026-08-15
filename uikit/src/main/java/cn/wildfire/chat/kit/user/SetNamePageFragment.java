/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.page.TextEditPageFragment;
import cn.wildfirechat.model.UserInfo;

/**
 * 「修改账号（wildfire id）」整页，逐行搬自 {@link SetNameActivity}。
 */
public class SetNamePageFragment extends TextEditPageFragment {

    private UserInfo userInfo;

    public static SetNamePageFragment newInstance(UserInfo userInfo) {
        Bundle args = new Bundle();
        args.putParcelable("userInfo", userInfo);
        SetNamePageFragment fragment = new SetNamePageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static SetNamePageFragment fromIntent(Intent intent) {
        UserInfo userInfo = intent.getParcelableExtra("userInfo");
        return userInfo == null ? null : newInstance(userInfo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userInfo = getArguments() == null ? null : getArguments().getParcelable("userInfo");
    }

    @Override
    protected int contentLayout() {
        return R.layout.contact_set_name_activity;
    }

    @Override
    protected int editTextId() {
        return R.id.nameEditText;
    }

    @Override
    public int pageMenu() {
        return R.menu.user_set_alias;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.save;
    }

    @Override
    protected void onPageViewCreated(@NonNull View view) {
        if (userInfo != null) {
            setHint(userInfo.name);
        }
    }

    @Override
    protected void onConfirm(String text) {
        if (text.isEmpty()) {
            Toast.makeText(getActivity(), getString(R.string.wildfire_id_not_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().changeName(text, new SimpleCallback<Void>() {
            @Override
            public void onUiSuccess(Void aVoid) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(), getString(R.string.modify_success), Toast.LENGTH_SHORT).show();
                finishPage();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getActivity(),
                    getString(R.string.modify_account_error, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
