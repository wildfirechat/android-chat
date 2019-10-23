package cn.wildfire.chat.app.main;

import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.HashMap;
import java.util.Map;

import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.login.model.PCSession;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;

public class PCLoginActivity extends WfcBaseActivity {
    private String token;
    private PCSession pcSession;
    @BindView(R.id.confirmButton)
    Button confirmButton;

    @Override
    protected void beforeViews() {
        token = getIntent().getStringExtra("token");
        if (TextUtils.isEmpty(token)) {
            finish();
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.pc_login_activity;
    }

    @Override
    protected void afterViews() {
        scanPCLogin(token);
    }

    @OnClick(R.id.confirmButton)
    void confirmPCLogin() {
        UserViewModel userViewModel =ViewModelProviders.of(this).get(UserViewModel.class);
        confirmPCLogin(token, userViewModel.getUserId());
    }

    private void scanPCLogin(String token) {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("处理中")
                .progress(true, 100)
                .build();
        dialog.show();

        AppService.Instance().scanPCLogin(token, new AppService.ScanPCCallback() {
            @Override
            public void onUiSuccess(PCSession pcSession) {
                if (isFinishing()) {
                    return;
                }
                dialog.dismiss();
                PCLoginActivity.this.pcSession = pcSession;
                if (pcSession.getStatus() == 1) {
                    confirmButton.setEnabled(true);
                } else {
                    Toast.makeText(PCLoginActivity.this, "status: " + pcSession.getStatus(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void confirmPCLogin(String token, String userId) {
        AppService.Instance().confirmPCLogin(token, userId, new AppService.PCLoginCallback() {
            @Override
            public void onUiSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCLoginActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
