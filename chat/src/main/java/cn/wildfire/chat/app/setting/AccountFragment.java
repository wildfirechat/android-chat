/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.chat.R;

/**
 * 账号与安全页。手机端装在 {@link AccountActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class AccountFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.changePasswordOptionItemView).setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        new MaterialDialog.Builder(requireContext())
            .items(R.array.change_password)
            .itemsCallback((dialog, v, position, text) -> {
                if (position == 0) {
                    WfcPageCompat.startPage(this, new Intent(getActivity(), ResetPasswordActivity.class));
                } else if (position == 1) {
                    WfcPageCompat.startPage(this, new Intent(getActivity(), ChangePasswordActivity.class));
                }
            })
            .show();
    }
}
