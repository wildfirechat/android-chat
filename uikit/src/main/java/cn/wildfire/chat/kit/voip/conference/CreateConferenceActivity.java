package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.voip.MultiCallActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.remote.ChatManager;

public class CreateConferenceActivity extends WfcBaseActivity {
    @BindView(R2.id.conferenceTitleEditText)
    EditText titleEditText;
    @BindView(R2.id.conferenceDescEditText)
    EditText descEditText;
    @BindView((R2.id.audioOnly))
    Switch audioOnlySwitch;
    @BindView((R2.id.audience))
    Switch audienceSwitch;
    @BindView(R2.id.createConferenceBtn)
    Button createButton;

    @Override
    protected int contentLayout() {
        return R.layout.conference_create_activity;
    }

    @OnClick(R2.id.createConferenceBtn)
    public void onClickCreateBtn() {
        AVEngineKit.Instance().startConference(null, false, null, ChatManager.Instance().getUserId(), "myConference", "conf", false, null);
        Intent intent = new Intent(this, ConferenceActivity.class);
        startActivity(intent);
    }
}
