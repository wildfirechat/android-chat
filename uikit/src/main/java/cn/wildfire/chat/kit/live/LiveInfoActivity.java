package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.util.UnstableApi;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.live.model.LiveInfo;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

@UnstableApi
public class LiveInfoActivity extends WfcBaseActivity {

    private String liveId;
    private LiveInfo liveInfo;

    private TextView titleTextView;
    private TextView hostTextView;
    private TextView liveIdTextView;
    private TextView statusTextView;
    private Button joinLiveButton;

    @Override
    protected int contentLayout() {
        return R.layout.activity_live_info;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        titleTextView = findViewById(R.id.titleTextView);
        hostTextView = findViewById(R.id.hostTextView);
        liveIdTextView = findViewById(R.id.liveIdTextView);
        statusTextView = findViewById(R.id.statusTextView);
        joinLiveButton = findViewById(R.id.joinLiveButton);
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        joinLiveButton.setOnClickListener(v -> joinLive());
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        setTitle("直播详情");
        liveId = getIntent().getStringExtra("liveId");

        if (TextUtils.isEmpty(liveId)) {
            finish();
            return;
        }

        loadLiveInfo();
    }

    private void loadLiveInfo() {
        LiveKit.getInstance().getLiveInfo(liveId, new SimpleCallback<LiveInfo>() {
            @Override
            public void onUiSuccess(LiveInfo info) {
                liveInfo = info;
                updateUI();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(LiveInfoActivity.this, "加载失败: " + msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        if (liveInfo == null) return;
        
        titleTextView.setText(liveInfo.getTitle());
        liveIdTextView.setText(liveInfo.getLiveId());
        
        UserInfo hostUser = ChatManager.Instance().getUserInfo(liveInfo.getHost(), false);
        if (hostUser != null) {
            hostTextView.setText(hostUser.displayName);
        } else {
            hostTextView.setText(liveInfo.getHost());
        }

        String statusStr = "未开始";
        if (liveInfo.getStatus() == 1) {
            statusStr = "正在进行";
        } else if (liveInfo.getStatus() == 2) {
            statusStr = "已结束";
            joinLiveButton.setEnabled(false);
            joinLiveButton.setText("直播已结束");
        }
        statusTextView.setText(statusStr);
    }

    private void joinLive() {
        if (liveInfo == null) return;

        if (ChatManager.Instance().getUserId().equals(liveInfo.getHost())) {
            Intent intent = new Intent(this, LiveHostStreamActivity.class);
            intent.putExtra("liveInfo", liveInfo);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, LiveAudienceActivity.class);
            intent.putExtra("liveInfo", liveInfo);
            startActivity(intent);
        }
        finish();
    }
}
