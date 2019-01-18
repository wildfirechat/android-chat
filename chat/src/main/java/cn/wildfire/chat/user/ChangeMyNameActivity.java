package cn.wildfire.chat.user;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import cn.wildfirechat.chat.R;

import java.util.Collections;

import butterknife.Bind;
import butterknife.OnTextChanged;
import cn.wildfire.chat.WfcBaseActivity;
import cn.wildfire.chat.common.OperateResult;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_DisplayName;

/**
 * @创建者 CSDN_LQR
 * @描述 更改名字界面
 */
public class ChangeMyNameActivity extends WfcBaseActivity {

    private MenuItem confirmMenuItem;
    @Bind(R.id.nameEditText)
    EditText nameEditText;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

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

    @OnTextChanged(value = R.id.nameEditText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void inputNewName(CharSequence s, int start, int before, int count) {
        if (nameEditText.getText().toString().trim().length() > 0) {
            confirmMenuItem.setEnabled(true);
        } else {
            confirmMenuItem.setEnabled(false);
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
