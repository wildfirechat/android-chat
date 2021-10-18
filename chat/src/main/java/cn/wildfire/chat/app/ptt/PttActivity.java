/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.OnTouch;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.TalkingCallback;
import cn.wildfirechat.remote.GeneralCallback;

public class PttActivity extends WfcBaseActivity {
    @BindView(R.id.talkButton)
    Button talkButton;

    private String channelId;

    @Override
    protected int contentLayout() {
        return R.layout.ptt_activity;
    }

    @Override
    protected int menu() {
        List<String> xxx = PTTClient.getInstance().getSubscribedChannels();
        return R.menu.ptt;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        channelId = getIntent().getStringExtra("channelId");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.invite) {
            Intent intent = new Intent(this, PttInviteActivity.class);
            intent.putExtra("channelId", channelId);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void init(String channelId) {
        if (!PTTClient.getInstance().isInChannel(channelId)) {
            PTTClient.getInstance().joinChannel(channelId, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(PttActivity.this, "加入频道成功", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(PttActivity.this, "加入频道失败 " + errorCode, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }

    @OnTouch(R.id.talkButton)
    public boolean talk(View button, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            PTTClient.getInstance().requestTalk(channelId, new TalkingCallback() {
                @Override
                public void onStartTalking(String channelId) {
                    updateTalkButton(true);
                }

                @Override
                public void onTalkingEnd(String channelId, int reason) {
                    updateTalkButton(false);
                }

                @Override
                public void onRequestFail(String channelId, int errorCode) {
                    updateTalkButton(false);
                }
            });
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            PTTClient.getInstance().releaseTalking(channelId);
            updateTalkButton(false);
        }
        return true;
    }

    private void updateTalkButton(boolean talking) {
        talkButton.setPressed(talking);
        talkButton.setText(talking ? "松手释放" : "按住说话");
        float scale = talking ? 1.5f : 1.0f;
        talkButton.animate().scaleX(scale).scaleY(scale).setDuration(100).start();
    }

}
