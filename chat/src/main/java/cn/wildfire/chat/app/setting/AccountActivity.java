/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.setting;

import android.content.Intent;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;

public class AccountActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.account_activity;
    }


    @OnClick(R.id.changePasswordOptionItemView)
    void changePassword() {
        new MaterialDialog.Builder(this).items(R.array.change_password).itemsCallback(new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                if (position == 0) {
                    Intent intent = new Intent(AccountActivity.this, ResetPasswordActivity.class);
                    startActivity(intent);
                } else if (position == 1) {
                    Intent intent = new Intent(AccountActivity.this, ChangePasswordActivity.class);
                    startActivity(intent);
                }
            }
        }).show();
    }
}
