package cn.wildfire.chat.kit.user;

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
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_DisplayName;

public class ChangeMyNameActivity extends WfcBaseActivity {

    private MenuItem confirmMenuItem;
    @BindView(R2.id.nameEditText)
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

    @OnTextChanged(value = R2.id.nameEditText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void inputNewName(CharSequence s, int start, int before, int count) {
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
