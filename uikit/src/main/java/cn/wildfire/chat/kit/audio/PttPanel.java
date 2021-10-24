/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.TalkingCallback;
import cn.wildfirechat.remote.ChatManager;

public class PttPanel implements View.OnTouchListener {
    private int maxDuration = Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * 1000;
    private int countDown = 10 * 1000;
    private boolean isTalking;
    private long startTime;
    private boolean isCountDown;

    private Context context;
    private Conversation conversation;
    private View rootView;
    private Button button;
    private Handler handler;

    private TextView countDownTextView;
    private TextView stateTextView;
    private ImageView stateImageView;
    private PopupWindow talkingWindow;

    public PttPanel(Context context) {
        this.context = context;
        this.handler = ChatManager.Instance().getMainHandler();
        ;
    }

    /**
     * 将{@link PttPanel}附加到button上面
     *
     * @param rootView 录音界面显示的rootView
     * @param button   长按触发录音的按钮
     */
    public void attach(View rootView, Button button, Conversation conversation) {
        this.rootView = rootView;
        this.button = button;
        this.button.setText("按住 对讲");
        this.button.setOnTouchListener(this);
        this.conversation = conversation;
        this.maxDuration = PTTClient.getInstance().getMaxSpeakTime(conversation) * 1000;
    }

    public void deattch() {
        rootView = null;
        button = null;
        this.conversation = null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (button == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_pressed);
                requestTalk();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_normal);
                if (isTalking) {
                    stopTalk();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void requestTalk() {
        // FIXME: 2018/10/10 权限是否有权限，没权限提示错误，并返回
        handler.removeCallbacks(this::hideTalking);
        PTTClient.getInstance().requestTalk(conversation, new TalkingCallback() {
            @Override
            public void onStartTalking(Conversation conversation) {
                showTalking();
                startTime = System.currentTimeMillis();
                isTalking = true;
                showTalking();
                tick();
            }

            @Override
            public void onTalkingEnd(Conversation conversation, int reason) {
                stopTalk();
            }

            @Override
            public void onRequestFail(Conversation conversation, int errorCode) {
                // do nothing
                Toast.makeText(context, "请求对讲失败 " + errorCode, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public boolean isShowingTalking() {
        return talkingWindow != null;
    }

    private void stopTalk() {
        if (!isTalking) {
            return;
        }
        PTTClient.getInstance().releaseTalking(conversation);
        hideTalking();

        isTalking = false;

    }

    private void showTalking() {
        if (talkingWindow == null) {
            View view = View.inflate(context, R.layout.ptt_popup_wi_vo, null);
            stateImageView = view.findViewById(R.id.rc_ptt_state_image);
            stateTextView = view.findViewById(R.id.rc_ptt_state_text);
            countDownTextView = view.findViewById(R.id.rc_ptt_timer);
            talkingWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            talkingWindow.setFocusable(false);
            talkingWindow.setOutsideTouchable(false);
            talkingWindow.setTouchable(true);
        }

        talkingWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);

        if (isCountDown) {
            countDownTextView.setVisibility(View.VISIBLE);
            stateImageView.setVisibility(View.GONE);
        } else {
            stateImageView.setVisibility(View.VISIBLE);
            stateImageView.setImageResource(R.mipmap.ic_volume_1);
            countDownTextView.setVisibility(View.GONE);
        }
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText("松手结束对讲");
        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
    }

    private void hideTalking() {
        if (talkingWindow == null) {
            return;
        }
        talkingWindow.dismiss();
        talkingWindow = null;
        stateImageView = null;
        stateTextView = null;
        countDownTextView = null;
        isCountDown = false;
    }

    /**
     * @param seconds
     */
    private void showCountDown(int seconds) {
        stateImageView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText("松手结束对讲");
        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
        countDownTextView.setText(String.format("%s", seconds));
        countDownTextView.setVisibility(View.VISIBLE);
    }

    private void tick() {
        if (isTalking) {
            long now = System.currentTimeMillis();
            if (now - startTime > maxDuration) {
                // timeout, do othing
                return;
            } else if (now - startTime > (maxDuration - countDown)) {
                isCountDown = true;
                int tmp = (int) ((maxDuration - (now - startTime)) / 1000);
                tmp = Math.max(tmp, 1);
                showCountDown(tmp);
            } else {
                updateVolume();
            }
            handler.postDelayed(this::tick, 100);
        }
    }

    private void updateVolume() {
        int db = ((int) (System.currentTimeMillis() / 1000)) % 8;
        switch (db) {
            case 0:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_1);
                break;
            case 1:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_2);
                break;
            case 2:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_3);
                break;
            case 3:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_4);
                break;
            case 4:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_5);
                break;
            case 5:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_6);
                break;
            case 6:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_7);
                break;
            default:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_8);
        }

    }
}
