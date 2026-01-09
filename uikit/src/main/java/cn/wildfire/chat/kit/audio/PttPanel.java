/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.ptt.TalkingCallback;
import cn.wildfirechat.remote.ChatManager;

public class PttPanel implements View.OnTouchListener {
    private int maxDuration;
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
    private View talkingContentView;

    private SoundPool soundPool;
    private int startSoundId;
    private int stopSoundId;

    private ValueAnimator volumeAnimator;
    private ValueAnimator countDownAnimator;
    private int currentVolumeLevel = 0;

    public PttPanel(Context context) {
        this.context = context;
        this.handler = ChatManager.Instance().getMainHandler();
        this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
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
        this.button.setText(R.string.ptt_hold_to_talk);
        this.button.setOnTouchListener(this);
        this.conversation = conversation;
        this.maxDuration = PTTClient.getInstance().getMaxSpeakTime(conversation) * 1000;
        this.startSoundId = this.soundPool.load(context, R.raw.ptt_begin, 1);
        this.stopSoundId = this.soundPool.load(context, R.raw.ptt_end, 1);

        PTTClient.getInstance().setEnablePtt(conversation, true);
    }

    public void deattch() {
        if (rootView == null) {
            return;
        }
        PTTClient.getInstance().setEnablePtt(conversation, false);
        rootView = null;
        button = null;
        this.conversation = null;
        this.soundPool.unload(this.startSoundId);
        this.soundPool.unload(this.stopSoundId);
        this.handler.removeCallbacks(this::tick);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (button == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_pressed);
                // 添加按钮按下的缩放动画
                button.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .start();
                requestTalk();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_normal);
                // 恢复按钮缩放
                button.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start();
                stopTalk();
                break;
            default:
                break;
        }
        return true;
    }

    public void requestTalk() {
        handler.removeCallbacks(this::tick);
        // TODO 开始、结束、失败，播放对应的声音提示
        PTTClient.getInstance().requestTalk(conversation, new TalkingCallback() {
            @Override
            public int talkingPriority(Conversation conversation) {
                return 0;
            }

            @Override
            public void onStartTalking(Conversation conversation) {
                startTime = System.currentTimeMillis();
                isTalking = true;
                playSoundEffect(true);
                showTalking();
                tick();
            }

            @Override
            public void onTalkingEnd(Conversation conversation, int reason) {
                isTalking = false;
                playSoundEffect(false);
                stopTalk();
            }

            @Override
            public void onRequestFail(Conversation conversation, int errorCode) {
                // do nothing
                Toast.makeText(context, context.getString(R.string.ptt_request_failed, errorCode), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAmplitudeUpdate(int averageAmplitude) {
                updateVolume(averageAmplitude);
            }

            @Override
            public SoundMessageContent onCreateSoundMessageContent(String soundFilePath) {
                return TalkingCallback.super.onCreateSoundMessageContent(soundFilePath);
            }
        });
    }

    public boolean isShowingTalking() {
        return talkingWindow != null;
    }

    private void playSoundEffect(boolean start) {
        if (this.soundPool != null) {
            this.soundPool.play(start ? startSoundId : stopSoundId, 0.1f, 0.1f, 0, 0, 1);
        }
    }

    private void stopTalk() {
        PTTClient.getInstance().releaseTalking(conversation);
        this.isTalking = false;
        hideTalking();
    }

    private void showTalking() {
        if (talkingWindow == null) {
            View view = View.inflate(context, R.layout.ptt_popup_wi_vo, null);
            talkingContentView = view.findViewById(R.id.ptt_content_view);
            stateImageView = view.findViewById(R.id.rc_ptt_state_image);
            stateTextView = view.findViewById(R.id.rc_ptt_state_text);
            countDownTextView = view.findViewById(R.id.rc_ptt_timer);
            talkingWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            talkingWindow.setFocusable(false);
            talkingWindow.setOutsideTouchable(false);
            talkingWindow.setTouchable(true);
            // 让 PopupWindow 延伸到状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                talkingWindow.setClippingEnabled(false);
            }
        }

        talkingWindow.showAtLocation(rootView, Gravity.TOP | Gravity.START, 0, 0);

        // 开始入场动画
        animateShowTalking();

        if (isCountDown) {
            countDownTextView.setVisibility(View.VISIBLE);
            stateImageView.setVisibility(View.GONE);
        } else {
            stateImageView.setVisibility(View.VISIBLE);
            stateImageView.setImageResource(R.mipmap.ic_volume_1);
            countDownTextView.setVisibility(View.GONE);
        }
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText(R.string.ptt_release_to_end);
//        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
    }

    private void hideTalking() {
        if (talkingWindow == null) {
            return;
        }

        // 取消所有动画
        cancelAllAnimations();

        // 添加退出动画
        animateHideTalking(() -> {
            // 在动画结束后再次检查，避免被其他地方置空
            if (talkingWindow != null) {
                talkingWindow.dismiss();
                talkingWindow = null;
            }
            talkingContentView = null;
            stateImageView = null;
            stateTextView = null;
            countDownTextView = null;
            isCountDown = false;
        });
    }

    /**
     * @param seconds
     */
    private void showCountDown(int seconds) {
        stateImageView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText("松手结束对讲");
//        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
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
            }
            handler.postDelayed(this::tick, 100);
        }
    }

    private void updateVolume(int averageAmplitude) {
        if (conversation == null || this.stateImageView == null) {
            return;
        }
        int db = (averageAmplitude / 1000) % 8;

        // 使用动画平滑过渡音量变化
        animateVolumeChange(db);
    }

    private void animateVolumeChange(int targetLevel) {
        if (targetLevel == currentVolumeLevel) {
            return;
        }

        if (volumeAnimator != null && volumeAnimator.isRunning()) {
            volumeAnimator.cancel();
        }

        volumeAnimator = ValueAnimator.ofInt(currentVolumeLevel, targetLevel);
        volumeAnimator.setDuration(150);
        volumeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        volumeAnimator.addUpdateListener(animation -> {
            int level = (int) animation.getAnimatedValue();
            updateVolumeIcon(level);
        });
        volumeAnimator.start();

        currentVolumeLevel = targetLevel;
    }

    private void updateVolumeIcon(int db) {
        int iconRes;
        switch (db) {
            case 0:
                iconRes = R.mipmap.ic_volume_1;
                break;
            case 1:
                iconRes = R.mipmap.ic_volume_2;
                break;
            case 2:
                iconRes = R.mipmap.ic_volume_3;
                break;
            case 3:
                iconRes = R.mipmap.ic_volume_4;
                break;
            case 4:
                iconRes = R.mipmap.ic_volume_5;
                break;
            case 5:
                iconRes = R.mipmap.ic_volume_6;
                break;
            case 6:
                iconRes = R.mipmap.ic_volume_7;
                break;
            default:
                iconRes = R.mipmap.ic_volume_8;
        }

        if (stateImageView != null) {
            stateImageView.setImageResource(iconRes);
            // 添加轻微的缩放动画
            stateImageView.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    if (stateImageView != null) {
                        stateImageView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                    }
                })
                .start();
        }
    }

    /**
     * 显示对讲窗口的入场动画
     */
    private void animateShowTalking() {
        if (talkingContentView == null) {
            return;
        }

        // 设置初始状态
        talkingContentView.setAlpha(0f);
        talkingContentView.setScaleX(0.5f);
        talkingContentView.setScaleY(0.5f);

        // 创建动画集合
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(talkingContentView, "alpha", 0f, 1f);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(talkingContentView, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(talkingContentView, "scaleY", 0.5f, 1f);

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator);
        animatorSet.setDuration(250);
        animatorSet.setInterpolator(new OvershootInterpolator(1.5f));
        animatorSet.start();
    }

    /**
     * 隐藏对讲窗口的退出动画
     */
    private void animateHideTalking(Runnable onAnimationEnd) {
        if (talkingContentView == null) {
            if (onAnimationEnd != null) {
                onAnimationEnd.run();
            }
            return;
        }

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(talkingContentView, "alpha", 1f, 0f);
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(talkingContentView, "scaleX", 1f, 0.5f);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(talkingContentView, "scaleY", 1f, 0.5f);

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator);
        animatorSet.setDuration(200);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });
        animatorSet.start();
    }

    /**
     * 取消所有动画
     */
    private void cancelAllAnimations() {
        if (volumeAnimator != null && volumeAnimator.isRunning()) {
            volumeAnimator.cancel();
            volumeAnimator = null;
        }

        if (countDownAnimator != null && countDownAnimator.isRunning()) {
            countDownAnimator.cancel();
            countDownAnimator = null;
        }

        if (stateImageView != null) {
            stateImageView.clearAnimation();
        }

        if (talkingContentView != null) {
            talkingContentView.clearAnimation();
        }
    }
}
