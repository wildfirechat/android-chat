package cn.wildfire.chat.app.setting;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.app.Config;
import cn.wildfire.chat.app.main.SplashActivity;
import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.OKHttpHelper;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.settings.PrivacySettingActivity;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class SettingActivity extends WfcBaseActivity {
    @BindView(R.id.diagnoseOptionItemView)
    OptionItemView diagnoseOptionItemView;

    @Override
    protected int contentLayout() {
        return R.layout.setting_activity;
    }

    @OnClick(R.id.exitOptionItemView)
    void exit() {
        ChatManagerHolder.gChatManager.disconnect(true);
        SharedPreferences sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        sp.edit().clear().apply();

        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @OnClick(R.id.privacySettingOptionItemView)
    void privacySetting() {
        Intent intent = new Intent(this, PrivacySettingActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.diagnoseOptionItemView)
    void diagnose() {
        long start = System.currentTimeMillis();
        OKHttpHelper.get("http://" + Config.IM_SERVER_HOST + "/api/version", null, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String s) {
                long duration = (System.currentTimeMillis() - start) / 2;
                diagnoseOptionItemView.setDesc(duration + "ms");
                Toast.makeText(SettingActivity.this, "服务器延迟为：" + duration + "ms", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onUiFailure(int code, String msg) {
                diagnoseOptionItemView.setDesc("test failed");
                Toast.makeText(SettingActivity.this, "访问IM Server失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.aboutOptionItemView)
    void about() {
//        Intent intent = new Intent(this, AboutActivity.class);
//        startActivity(intent);
        String groupId = "q4Yuru44";
        new Thread(new Runnable() {
            @Override
            public void run() {
                Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupId, 0);
                String sender = ChatManager.Instance().getUserId();
                for (int i = 0; i < 300; i++) {
                    ChatManager.Instance().begainTransaction();
                    Log.d("jyj", "start insert " + i);
                    for (int j = 0; j < 1000; j++) {
                        String text = "hello " + (i * 300 + j);
                        MessageContent content = new TextMessageContent(text);
                        ChatManager.Instance().insertMessage(conversation, sender, content, MessageStatus.Unread, false, System.currentTimeMillis());
                    }
                    ChatManager.Instance().commitTransaction();
                }

            }
        }).start();
    }
}
