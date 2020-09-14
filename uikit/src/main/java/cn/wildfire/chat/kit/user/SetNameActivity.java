/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.model.UserInfo;


public class SetNameActivity extends WfcBaseActivity {

    private UserInfo userInfo;

    @BindView(R2.id.nameEditText)
    EditText nameEditText;

    private MenuItem menuItem;

    @Override
    protected int contentLayout() {
        return R.layout.contact_set_name_activity;
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

    @OnTextChanged(R2.id.nameEditText)
    void onAliasEditTextChange() {
        menuItem.setEnabled(nameEditText.getText().toString().trim().length() > 0 ? true : false);
    }

    private void changeAlias() {
        String displayName = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "野火ID不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        WfcUIKit.getWfcUIKit().getAppServiceProvider().changeName(displayName, new SimpleCallback<Void>() {
            @Override
            public void onUiSuccess(Void aVoid) {

                Toast.makeText(SetNameActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(SetNameActivity.this, "修改账号错误：" + code + " " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
