/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.UserInfo;


public class SetNameActivity extends WfcBaseActivity {

    private UserInfo userInfo;

    EditText nameEditText;

    private MenuItem menuItem;

    @Override
    protected int contentLayout() {
        return R.layout.contact_set_name_activity;
    }

    protected void bindViews() {
        super.bindViews();
        nameEditText = findViewById(R.id.nameEditText);
        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onAliasEditTextChange();
            }
        });
    }

    @Override
    protected void afterViews() {
        userInfo = getIntent().getParcelableExtra("userInfo");
        if (userInfo == null) {
            finish();
            return;
        }
        if (!TextUtils.isEmpty(userInfo.name)) {
            nameEditText.setHint(userInfo.name);
        }
    }

    @Override
    protected int menu() {
        return R.menu.user_set_alias;
    }

    @Override
    protected void afterMenus(Menu menu) {
        menuItem = menu.findItem(R.id.save);
        menuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            changeAlias();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void onAliasEditTextChange() {
        menuItem.setEnabled(nameEditText.getText().toString().trim().length() > 0 ? true : false);
    }

    private void changeAlias() {
        String displayName = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, getString(R.string.wildfire_id_not_empty), Toast.LENGTH_SHORT).show();
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().changeName(displayName, new SimpleCallback<Void>() {
            @Override
            public void onUiSuccess(Void aVoid) {

                Toast.makeText(SetNameActivity.this, getString(R.string.modify_success), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(SetNameActivity.this, getString(R.string.modify_account_error, code, msg), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
