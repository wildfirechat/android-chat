/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;


import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.UserInfo;

public class InviteFriendActivity extends WfcBaseActivity {
    TextView introTextView;

    private UserInfo userInfo;

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.clearImageButton).setOnClickListener(v -> clear());
    }

    protected void bindViews() {
        super.bindViews();
        introTextView = findViewById(R.id.introTextView);
    }

    @Override
    protected void afterViews() {
        userInfo = getIntent().getParcelableExtra("userInfo");
        if (userInfo == null) {
            finish();
        }
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo me = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        introTextView.setText(getString(R.string.invite_default_message, (me == null ? "" : me.displayName)));
    }

    @Override
    protected int contentLayout() {
        return R.layout.contact_invite_activity;
    }

    @Override
    protected int menu() {
        return R.menu.contact_invite;
    }

    void clear() {
        introTextView.setText("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            invite();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void invite() {
        ContactViewModel contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        contactViewModel.invite(userInfo.uid, introTextView.getText().toString())
            .observe(this, errorCode -> {
                if (errorCode == 0) {
                    Toast.makeText(InviteFriendActivity.this, R.string.invite_sent, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(InviteFriendActivity.this, getString(R.string.invite_error, errorCode), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
