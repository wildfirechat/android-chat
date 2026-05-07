/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.voip;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.voip.SingleCallActivity;
import cn.wildfire.chat.kit.voip.VoipCallService;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;

public class PstnDialActivity extends WfcBaseActivity {

    private TextView numberTextView;
    private TextView btnDelete;
    private View btnCall;
    private StringBuilder numberBuilder = new StringBuilder();
    private Handler deleteHandler = new Handler(Looper.getMainLooper());
    private Runnable deleteRunnable;
    private Vibrator vibrator;

    @Override
    protected int contentLayout() {
        return R.layout.activity_pstn_dial;
    }

    @Override
    protected void afterViews() {
        setTitle("落地电话");
        numberTextView = findViewById(R.id.numberTextView);
        btnDelete = findViewById(R.id.btnDelete);
        btnCall = findViewById(R.id.btnCall);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        int[] buttonIds = new int[]{
            R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6,
            R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnStar, R.id.btn0, R.id.btnHash
        };
        String[] digits = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

        for (int i = 0; i < buttonIds.length; i++) {
            final String digit = digits[i];
            findViewById(buttonIds[i]).setOnClickListener(v -> {
                performHapticFeedback();
                appendDigit(digit);
            });
        }

        View deleteBtn = findViewById(R.id.btnDelete);
        deleteBtn.setOnClickListener(v -> deleteDigit());
        deleteBtn.setOnLongClickListener(v -> {
            deleteDigit();
            deleteRunnable = new Runnable() {
                @Override
                public void run() {
                    deleteDigit();
                    deleteHandler.postDelayed(this, 100);
                }
            };
            deleteHandler.postDelayed(deleteRunnable, 100);
            return true;
        });
        deleteBtn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                deleteHandler.removeCallbacks(deleteRunnable);
            }
            return false;
        });
        btnCall.setOnClickListener(v -> startPstnCall());
        updateCallButtonState();
    }

    private void performHapticFeedback() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(30);
            }
        }
    }

    private void appendDigit(String digit) {
        if (numberBuilder.length() < 20) {
            numberBuilder.append(digit);
            numberTextView.setText(numberBuilder.toString());
            btnDelete.setEnabled(true);
            updateCallButtonState();
        }
    }

    private void deleteDigit() {
        if (numberBuilder.length() > 0) {
            numberBuilder.deleteCharAt(numberBuilder.length() - 1);
            numberTextView.setText(numberBuilder.toString());
            if (numberBuilder.length() == 0) {
                btnDelete.setEnabled(false);
            }
            updateCallButtonState();
        }
    }

    private void updateCallButtonState() {
        boolean hasNumber = numberBuilder.length() > 0;
        btnCall.setEnabled(hasNumber);
        btnCall.setAlpha(hasNumber ? 1.0f : 0.4f);
    }

    private void startPstnCall() {
        String number = numberBuilder.toString().trim();
        if (TextUtils.isEmpty(number)) {
            return;
        }
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, Config.PSTN_ASSISTANT_ID);
        AVEngineKit.CallSession session = AVEngineKit.Instance().startCall(
            conversation,
            Collections.singletonList(Config.PSTN_ASSISTANT_ID),
            true,
            1,
            number,
            null
        );
        if (session != null) {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(false);

            Intent intent = new Intent(this, SingleCallActivity.class);
            startActivity(intent);
            VoipCallService.start(this, false);
            finish();
        }
    }
}
