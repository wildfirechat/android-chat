package cn.wildfire.chat.kit.voip;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;

public class VoipDummyActivity extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            finish();
        } else {
            Intent intent;
            if (session.getConversation().type == Conversation.ConversationType.Single) {
                intent = new Intent(this, SingleCallActivity.class);
            } else {
                intent = new Intent(this, MultiCallActivity.class);
            }
            startActivity(intent);
            finish();
        }
    }
}
