/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.text.Editable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;


public class SetAliasActivity extends WfcBaseActivity {

    private String userId;
//    private Friend mFriend;

    EditText aliasEditText;

    private MenuItem menuItem;
    private ContactViewModel contactViewModel;

    protected void bindViews() {
        super.bindViews();
        aliasEditText = findViewById(R.id.aliasEditText);
        aliasEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                onAliasEditTextChange();
            }
        });
    }

    @Override
    protected int contentLayout() {
        return R.layout.contact_set_alias_activity;
    }

    @Override
    protected void afterViews() {
        userId = getIntent().getStringExtra("userId");
        if (TextUtils.isEmpty(userId)) {
            finish();
            return;
        }
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        String alias = contactViewModel.getFriendAlias(userId);
        if (!TextUtils.isEmpty(alias)) {
            aliasEditText.setHint(alias);
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
        menuItem.setEnabled(aliasEditText.getText().toString().trim().length() > 0 ? true : false);
    }

    private void changeAlias() {
        String displayName = aliasEditText.getText().toString().trim();
        contactViewModel.setFriendAlias(userId, displayName).observe(this, new Observer<OperateResult<Integer>>() {
            @Override
            public void onChanged(OperateResult<Integer> integerOperateResult) {
                if (integerOperateResult.isSuccess()) {
                    Toast.makeText(SetAliasActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(SetAliasActivity.this, "修改别名错误：" + integerOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
