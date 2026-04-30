/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.voip;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
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
    private StringBuilder numberBuilder = new StringBuilder();

    @Override
    protected int contentLayout() {
        return R.layout.activity_pstn_dial;
    }

    @Override
    protected void afterViews() {
        setTitle("电话助手");
        numberTextView = findViewById(R.id.numberTextView);
        btnDelete = findViewById(R.id.btnDelete);

        int[] buttonIds = new int[]{
            R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6,
            R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnStar, R.id.btn0, R.id.btnHash
        };
        String[] digits = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"};

        for (int i = 0; i < buttonIds.length; i++) {
            final String digit = digits[i];
            findViewById(buttonIds[i]).setOnClickListener(v -> appendDigit(digit));
        }

        findViewById(R.id.btnDelete).setOnClickListener(v -> deleteDigit());
        findViewById(R.id.btnCall).setOnClickListener(v -> startPstnCall());
    }

    private void appendDigit(String digit) {
        if (numberBuilder.length() < 20) {
            numberBuilder.append(digit);
            numberTextView.setText(numberBuilder.toString());
            btnDelete.setEnabled(true);
        }
    }

    private void deleteDigit() {
        if (numberBuilder.length() > 0) {
            numberBuilder.deleteCharAt(numberBuilder.length() - 1);
            numberTextView.setText(numberBuilder.toString());
            if (numberBuilder.length() == 0) {
                btnDelete.setEnabled(false);
            }
        }
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
