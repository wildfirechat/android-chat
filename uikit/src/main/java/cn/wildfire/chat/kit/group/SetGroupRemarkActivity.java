/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.GroupInfo;

public class SetGroupRemarkActivity extends WfcBaseActivity {
    EditText remarkEditText;

    private MenuItem confirmMenuItem;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    protected void bindViews() {
        super.bindViews();
        remarkEditText = findViewById(R.id.remarkEditText);
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_set_remark_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);

        if (!TextUtils.isEmpty(groupInfo.remark)) {
            remarkEditText.setText(groupInfo.remark);
            remarkEditText.setSelection(groupInfo.remark.length());
        }
    }

    @Override
    protected int menu() {
        return R.menu.group_set_group_remark;
    }

    @Override
    protected void afterMenus(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.confirm);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            setGroupRemark();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setGroupRemark() {
        String remark = remarkEditText.getText().toString().trim();
        if (remark.equals(groupInfo.remark)) {
            finish();
            return;
        }
        groupInfo.remark = remark;
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.processing)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();

        groupViewModel.setGroupRemark(groupInfo.target, groupInfo.remark).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult operateResult) {
                dialog.dismiss();
                if (operateResult.isSuccess()) {
                    Toast.makeText(SetGroupRemarkActivity.this, getString(R.string.modify_group_remark_success), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(SetGroupRemarkActivity.this, getString(R.string.modify_group_remark_failed, operateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
