/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.TextEditPageFragment;

/**
 * 「设置备注名」整页，逐行搬自 {@link SetAliasActivity}。
 */
public class SetAliasPageFragment extends TextEditPageFragment {

    private String userId;
    private UserViewModel userViewModel;

    public static SetAliasPageFragment newInstance(String userId) {
        Bundle args = new Bundle();
        args.putString("userId", userId);
        SetAliasPageFragment fragment = new SetAliasPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static SetAliasPageFragment fromIntent(Intent intent) {
        String userId = intent.getStringExtra("userId");
        return TextUtils.isEmpty(userId) ? null : newInstance(userId);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = getArguments() == null ? null : getArguments().getString("userId");
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
    }

    @Override
    protected int contentLayout() {
        return R.layout.contact_set_alias_activity;
    }

    @Override
    protected int editTextId() {
        return R.id.aliasEditText;
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
        // 已有备注名只作为提示，不预填 —— 与改造前一致
        setHint(userViewModel.getFriendAlias(userId));
    }

    @Override
    protected void onConfirm(String text) {
        userViewModel.setFriendAlias(userId, text).observe(getViewLifecycleOwner(), result -> {
            if (result.isSuccess()) {
                Toast.makeText(getActivity(), getString(R.string.modify_success), Toast.LENGTH_SHORT).show();
                finishPage();
            } else {
                Toast.makeText(getActivity(),
                    getString(R.string.modify_alias_error, result.getErrorCode()), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
