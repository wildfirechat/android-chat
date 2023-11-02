/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_DisplayName;

import android.os.Bundle;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

public class ChangeMyNameActivity extends WfcBaseActivity {

    private MenuItem confirmMenuItem;
    EditText nameEditText;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void bindViews() {
        super.bindViews();
        nameEditText = findViewById(R.id.nameEditText);
        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                inputNewName();
            }
        });
    }

    @Override
    protected void afterViews() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);

        userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if (userInfo == null) {
            Toast.makeText(this, "用户不存在", Toast.LENGTH_SHORT).show();
            finish();
        }
        initView();
    }

    @Override
    protected int contentLayout() {
        return R.layout.user_change_my_name_activity;
    }

    @Override
    protected int menu() {
        return R.menu.user_change_my_name;
    }

    @Override
    protected void afterMenus(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.save);
        confirmMenuItem.setEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            changeMyName();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        if (userInfo != null) {
            nameEditText.setText(userInfo.displayName);
        }
        nameEditText.setSelection(nameEditText.getText().toString().trim().length());
    }

    void inputNewName() {
        if (confirmMenuItem != null) {
            if (nameEditText.getText().toString().trim().length() > 0) {
                confirmMenuItem.setEnabled(true);
            } else {
                confirmMenuItem.setEnabled(false);
            }
        }
    }


    private void changeMyName() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content("修改中...")
            .progress(true, 100)
            .build();
        dialog.show();
        String nickName = nameEditText.getText().toString().trim();
        ModifyMyInfoEntry entry = new ModifyMyInfoEntry(Modify_DisplayName, nickName);
        userViewModel.modifyMyInfo(Collections.singletonList(entry)).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(ChangeMyNameActivity.this, "修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChangeMyNameActivity.this, "修改失败", Toast.LENGTH_SHORT).show();
                }
                dialog.dismiss();
                finish();
            }
        });
    }
}
