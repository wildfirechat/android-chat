package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.ModifyGroupInfoType;

public class SetGroupNameActivity extends WfcBaseActivity {
    @BindView(R.id.nameEditText)
    EditText nameEditText;

    private MenuItem confirmMenuItem;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;

    public static final int RESULT_SET_GROUP_NAME_SUCCESS = 100;

    @Override
    protected int contentLayout() {
        return R.layout.group_set_name_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);

        nameEditText.setText(groupInfo.name);
        nameEditText.setSelection(groupInfo.name.length());
    }

    @Override
    protected int menu() {
        return R.menu.group_set_group_name;
    }

    @Override
    protected void afterMenus(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.confirm);
        if (nameEditText.getText().toString().trim().length() > 0) {
            confirmMenuItem.setEnabled(true);
        } else {
            confirmMenuItem.setEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            setGroupName();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnTextChanged(R.id.nameEditText)
    void onTextChanged() {
        if (confirmMenuItem != null) {
            confirmMenuItem.setEnabled(nameEditText.getText().toString().trim().length() > 0);
        }
    }

    private void setGroupName() {
        groupInfo.name = nameEditText.getText().toString().trim();
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("请稍后...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        groupViewModel.modifyGroupInfo(groupInfo.target, ModifyGroupInfoType.Modify_Group_Name, groupInfo.name, null, Collections.singletonList(0)).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult operateResult) {
                dialog.dismiss();
                if (operateResult.isSuccess()) {
                    Toast.makeText(SetGroupNameActivity.this, "修改群名称成功", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent();
                    intent.putExtra("groupName", groupInfo.name);
                    setResult(RESULT_SET_GROUP_NAME_SUCCESS, intent);
                    finish();
                } else {
                    Toast.makeText(SetGroupNameActivity.this, "修改群名称失败: " + operateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
