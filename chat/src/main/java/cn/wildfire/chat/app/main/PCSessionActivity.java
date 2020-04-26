package cn.wildfire.chat.app.main;

import android.widget.Toast;

import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.PCOnlineInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;

public class PCSessionActivity extends WfcBaseActivity {

    private PCOnlineInfo pcOnlineInfo;

    @Override
    protected void beforeViews() {
        pcOnlineInfo = getIntent().getParcelableExtra("pcOnlineInfo");
        if (pcOnlineInfo == null) {
            finish();
        }
    }

    @Override
    protected int contentLayout() {
        return R.layout.pc_session_activity;
    }

    @OnClick(R.id.kickOffPCButton)
    void kickOffPC() {
        ChatManager.Instance().kickoffPCClient(pcOnlineInfo.getClientId(), new GeneralCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, "PC端已踢下线", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFail(int errorCode) {
                if (isFinishing()) {
                    return;
                }
                Toast.makeText(PCSessionActivity.this, "" + errorCode, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
