/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;


import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.DateTimePickerHelper;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

public class CreateConferenceActivity extends WfcBaseActivity {
    FixedTextInputEditText titleEditText;
    SwitchMaterial passwordSwitch;
    SwitchMaterial audienceSwitch;
    SwitchMaterial modeSwitch;
    SwitchMaterial advancedSwitch;
    SwitchMaterial userCallIdSwitch;

    Button joinConferenceButton;

    TextView endDateTimeTextView;
    TextView callIdTextView;
    TextView passwordTextView;

    private Date endDateTime;

    private MenuItem createConferenceMenuItem;

    private String title;
    private String conferenceId;
    private String password;
    private boolean enableVideo = false;
    private boolean enableAudio = true;

    private static final String TAG = "createConference";

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.endDateTimeRelativeLayout).setOnClickListener(v -> pickEndDateTime());
        findViewById(R.id.joinConferenceBtn).setOnClickListener(v -> onClickJoinBtn());
        audienceSwitch.setOnCheckedChangeListener(this::audienceChecked);
        passwordSwitch.setOnCheckedChangeListener(this::passwordChecked);
        titleEditText.addTextChangedListener(new SimpleTextWatcher(){
            @Override
            public void afterTextChanged(Editable s) {
                conferenceTitle(s);
            }
        });
    }

    protected void bindViews() {
        super.bindViews();
        titleEditText = findViewById(R.id.conferenceTitleTextInputEditText);
        audienceSwitch = findViewById((R.id.audienceSwitch));
        passwordSwitch  = findViewById(R.id.passwordSwitch);
        modeSwitch = findViewById((R.id.modeSwitch));
        advancedSwitch = findViewById((R.id.advanceSwitch));
        userCallIdSwitch = findViewById(R.id.userCallIdSwitch);
        joinConferenceButton = findViewById(R.id.joinConferenceBtn);
        endDateTimeTextView = findViewById(R.id.endDateTimeTextView);
        callIdTextView = findViewById(R.id.callIdTextView);
        passwordTextView = findViewById(R.id.passwordTextView);
    }

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_create_activity;
    }

    @Override
    protected int menu() {
        return R.menu.create_conference;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void afterMenus(Menu menu) {
        createConferenceMenuItem = menu.findItem(R.id.create);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createConference(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void afterViews() {
        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(getString(R.string.conference_title_default, userInfo.displayName));
        } else {
            titleEditText.setText(getString(R.string.conference_title_unnamed));
        }
        advancedSwitch.setChecked(false);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        endDateTime = calendar.getTime();
        endDateTimeTextView.setText(endDateTime.toString());
    }

    void audienceChecked(CompoundButton button, boolean checked) {
        if (checked) {
            modeSwitch.setChecked(true);
            modeSwitch.setEnabled(false);
        } else {
            modeSwitch.setChecked(true);
            modeSwitch.setEnabled(true);
        }
    }

    void passwordChecked(CompoundButton button, boolean checked) {
        if (checked) {
            new MaterialDialog.Builder(this)
                .content(R.string.conference_enter_password)
                .input(getString(R.string.conference_password_hint), "123456", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        password = input.toString();
                        if (!TextUtils.isEmpty(password)) {
                            passwordTextView.setText(password);
                            passwordTextView.setVisibility(View.VISIBLE);
                        } else {
                            passwordTextView.setVisibility(View.GONE);
                        }
                    }
                })
                .inputRange(6, 6)
                .inputType(2)
                .cancelable(false)
                .build()
                .show();
        } else {
            password = null;
            passwordTextView.setText("");
            passwordTextView.setVisibility(View.GONE);
        }
    }

    void conferenceTitle(Editable editable) {
        this.title = editable.toString();
        if (!TextUtils.isEmpty(title)) {
            joinConferenceButton.setEnabled(true);
            if (createConferenceMenuItem != null) {
                createConferenceMenuItem.setEnabled(true);
            }
        } else {
            joinConferenceButton.setEnabled(false);
            if (createConferenceMenuItem != null) {
                createConferenceMenuItem.setEnabled(false);
            }
        }
    }

    void pickEndDateTime() {
        DateTimePickerHelper.pickDateTime(this, new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(CreateConferenceActivity.this, R.string.conference_end_time_invalid, Toast.LENGTH_SHORT).show();
                } else {
                    endDateTimeTextView.setText(date.toString());
                    endDateTime = date;
                }
            }

            @Override
            public void onCancel() {

            }
        });
    }

    public void onClickJoinBtn() {
        createConference(true);
    }

    private void createConference(boolean join) {
        joinConferenceButton.setEnabled(false);
        ConferenceInfo info = new ConferenceInfo();
        Toast.makeText(this, R.string.conference_creating, Toast.LENGTH_SHORT).show();
        info.setPassword(password);
        info.setConferenceTitle(titleEditText.getText().toString());
        Random random = new Random();
        String pin = String.format("%d%d%d%d", random.nextInt() % 10, random.nextInt() % 10, random.nextInt() % 10, random.nextInt() % 10);
        info.setPin(pin);

        info.setOwner(ChatManager.Instance().getUserId());
        info.setStartTime(System.currentTimeMillis() / 1000);
        info.setEndTime(endDateTime.getTime() / 1000);
        info.setAudience(!audienceSwitch.isChecked());
        info.setAllowTurnOnMic(modeSwitch.isChecked());
        info.setAdvance(advancedSwitch.isChecked());
        // 可根据实际情况调整
        info.setMaxParticipants(20);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().createConference(info, new GeneralCallback2() {
            @Override
            public void onSuccess(String conferenceId) {
                info.setConferenceId(conferenceId);
                if (join) {
                    AVEngineKit.CallSession session = AVEngineKit.Instance().startConference(conferenceId, false, info.getPin(), info.getOwner(), info.getConferenceTitle(), "", info.isAudience(), info.isAdvance(), false, !enableAudio, !enableVideo, info.getMaxParticipants(), null);
                    if (session != null) {
                        Intent intent = new Intent(CreateConferenceActivity.this, ConferenceActivity.class);
                        startActivity(intent);
                        ConferenceManager.getManager().setCurrentConferenceInfo(info);
                        finish();
                    } else {
                        Toast.makeText(CreateConferenceActivity.this, R.string.conference_create_failed, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    finish();
                }
            }

            @Override
            public void onFail(int i) {
                Toast.makeText(CreateConferenceActivity.this, getString(R.string.conference_create_failed_code, i), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "createConference fail" + i);
                joinConferenceButton.setEnabled(true);
            }
        });
    }
}
