package cn.wildfire.chat.kit.live;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.live.model.CreateLiveRequest;
import cn.wildfire.chat.kit.live.model.LiveInfo;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class CreateLiveActivity extends WfcBaseActivity {

    private FixedTextInputEditText titleEditText;
    private FixedTextInputEditText descEditText;
    private SwitchMaterial audienceSwitch;
    private SwitchMaterial audioOnlySwitch;
    private SwitchMaterial recordSwitch;
    private SwitchMaterial groupLiveSwitch;
    private Button createLiveBtn;
    private String groupId;

    @Override
    protected int contentLayout() {
        return R.layout.activity_create_live;
    }

    @Override
    protected void bindViews() {
        super.bindViews();
        titleEditText = findViewById(R.id.liveTitleEditText);
        descEditText = findViewById(R.id.liveDescEditText);
        audienceSwitch = findViewById(R.id.audienceSwitch);
        audioOnlySwitch = findViewById(R.id.audioOnlySwitch);
        recordSwitch = findViewById(R.id.recordSwitch);
        groupLiveSwitch = findViewById(R.id.groupLiveSwitch);
        createLiveBtn = findViewById(R.id.createLiveBtn);
    }

    @Override
    protected void bindEvents() {
        super.bindEvents();
        createLiveBtn.setOnClickListener(v -> createLive());
        titleEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                createLiveBtn.setEnabled(!TextUtils.isEmpty(s.toString()));
            }
        });
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(getString(R.string.live_streaming_default_title)); // Using default string or custom
        } else {
            titleEditText.setText("直播");
        }
        setTitle("创建直播");

        groupId = getIntent().getStringExtra("groupId");
    }

    private void createLive() {
        String title = titleEditText.getText().toString();
        if (TextUtils.isEmpty(title)) {
            return;
        }
        createLiveBtn.setEnabled(false);
        CreateLiveRequest request = new CreateLiveRequest();
        request.setTitle(title);
        request.setDescription(descEditText.getText() != null ? descEditText.getText().toString() : "");
        request.setAudience(audienceSwitch.isChecked());
        request.setAudioOnly(audioOnlySwitch.isChecked());
        request.setGroupId(groupLiveSwitch.isChecked() ? groupId : null);
        request.setRecord(recordSwitch.isChecked());
        request.setMaxParticipantCount(8);
        request.setStartTime(0);

        Toast.makeText(this, "正在创建直播...", Toast.LENGTH_SHORT).show();
        LiveStreamingKit.getInstance().createLive(request, new SimpleCallback<LiveInfo>() {
            @Override
            public void onUiSuccess(LiveInfo liveInfo) {
                startLiveStream(liveInfo);
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(CreateLiveActivity.this, "创建失败: " + msg, Toast.LENGTH_SHORT).show();
                createLiveBtn.setEnabled(true);
            }
        });
    }

    private void startLiveStream(LiveInfo liveInfo) {
        LiveStreamingKit.getInstance().startLiveStream(liveInfo.getLiveId(), new SimpleCallback<Void>() {

            @Override
            public void onUiSuccess(Void unused) {
                Intent intent = new Intent(CreateLiveActivity.this, LiveHostActivity.class);
                intent.putExtra("liveInfo", liveInfo);
                startActivity(intent);
                finish();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(CreateLiveActivity.this, " 开始直播失败: " + msg, Toast.LENGTH_SHORT).show();
                createLiveBtn.setEnabled(true);
            }
        });
    }
}
