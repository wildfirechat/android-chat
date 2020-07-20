package cn.wildfire.chat.kit.group;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.GroupInfo;

public class SetGroupAnnouncementActivity extends WfcBaseActivity {
    @BindView(R2.id.announcementEditText)
    EditText announcementEditText;

    private MenuItem confirmMenuItem;
    private GroupInfo groupInfo;

    @Override
    protected int contentLayout() {
        return R.layout.group_set_announcement_activity;
    }

    @Override
    protected void afterViews() {
        groupInfo = getIntent().getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            finish();
            return;
        }

        WfcUIKit.getWfcUIKit().getAppServiceProvider().getGroupAnnouncement(groupInfo.target, new AppServiceProvider.GetGroupAnnouncementCallback() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                if (isFinishing()) {
                    return;
                }
                if (TextUtils.isEmpty(announcementEditText.getText())) {
                    announcementEditText.setText(announcement.text);
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(SetGroupAnnouncementActivity.this, "获取群公告失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected int menu() {
        return R.menu.group_set_group_name;
    }

    @Override
    protected void afterMenus(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.confirm);
        if (announcementEditText.getText().toString().trim().length() > 0) {
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

    @OnTextChanged(R2.id.announcementEditText)
    void onTextChanged() {
        if (confirmMenuItem != null) {
            confirmMenuItem.setEnabled(announcementEditText.getText().toString().trim().length() > 0);
        }
    }

    private void setGroupName() {
        String announcement = announcementEditText.getText().toString().trim();
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("请稍后...")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();

        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateGroupAnnouncement(groupInfo.target, announcement, new AppServiceProvider.UpdateGroupAnnouncementCallback() {
            @Override
            public void onUiSuccess(GroupAnnouncement announcement) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(SetGroupAnnouncementActivity.this, "设置群公告成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                Toast.makeText(SetGroupAnnouncementActivity.this, "设置群公告失败: " + code + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
