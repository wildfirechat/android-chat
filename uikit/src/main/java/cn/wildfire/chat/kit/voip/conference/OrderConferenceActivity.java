/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.DateTimePickerHelper;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

public class OrderConferenceActivity extends WfcBaseActivity {
    FixedTextInputEditText titleEditText;
    SwitchMaterial passwordSwitch;
    SwitchMaterial audienceSwitch;
    SwitchMaterial modeSwitch;
    SwitchMaterial advancedSwitch;

    TextView endDateTimeTextView;
    TextView startDateTimeTextView;

    TextView passwordTextView;

    private Date endDateTime;
    private Date startDateTime;

    private MenuItem orderConferenceMenuItem;

    private String title;
    private String password;

    private static final String TAG = "orderConference";

    protected void bindEvents() {
        super.bindEvents();
        findViewById(R.id.endDateTimeRelativeLayout).setOnClickListener(v -> pickEndDateTime());
        findViewById(R.id.startDateTimeRelativeLayout).setOnClickListener(v -> pickStartDateTime());

        audienceSwitch.setOnCheckedChangeListener(this::audienceChecked);
        passwordSwitch.setOnCheckedChangeListener(this::passwordChecked);
        titleEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                conferenceTitle(s);
            }
        });
    }

    protected void bindViews() {
        super.bindViews();
        titleEditText = findViewById(R.id.conferenceTitleTextInputEditText);
        audienceSwitch = findViewById(R.id.audienceSwitch);
        passwordSwitch = findViewById(R.id.passwordSwitch);
        modeSwitch = findViewById((R.id.modeSwitch));
        advancedSwitch = findViewById((R.id.advanceSwitch));
        endDateTimeTextView = findViewById(R.id.endDateTimeTextView);
        startDateTimeTextView = findViewById(R.id.startDateTimeTextView);
        passwordTextView = findViewById(R.id.passwordTextView);
    }

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_order_activity;
    }

    @Override
    protected int menu() {
        return R.menu.order_conference;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void afterMenus(Menu menu) {
        orderConferenceMenuItem = menu.findItem(R.id.create);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createConference();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void afterViews() {
        UserInfo userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(userInfo.displayName + "的会议");
        } else {
            titleEditText.setText("会议");
        }
        advancedSwitch.setChecked(false);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        endDateTime = calendar.getTime();
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
                .content("请输入密码")
                .input("请输入6位数字", "123456", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        password = input.toString();
                        if (!TextUtils.isEmpty(password)) {
                            passwordTextView.setVisibility(View.VISIBLE);
                            passwordTextView.setText(password);
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
            if (orderConferenceMenuItem != null) {
                orderConferenceMenuItem.setEnabled(true);
            }
        } else {
            if (orderConferenceMenuItem != null) {
                orderConferenceMenuItem.setEnabled(false);
            }
        }
    }

    void pickEndDateTime() {
        DateTimePickerHelper.pickDateTime(this, new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(OrderConferenceActivity.this, "结束时间，不能早于当前时间", Toast.LENGTH_SHORT).show();
                    return;
                }
                endDateTimeTextView.setText(date.toString());
                endDateTime = date;
            }

            @Override
            public void onCancel() {

            }
        });
    }

    void pickStartDateTime() {
        DateTimePickerHelper.pickDateTime(this, new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(OrderConferenceActivity.this, "开始时间，不能早于当前时间", Toast.LENGTH_SHORT).show();
                    return;
                }
                startDateTimeTextView.setText(date.toString());
                startDateTime = date;
            }

            @Override
            public void onCancel() {

            }
        });
    }

    private void createConference() {
        if (startDateTime == null || endDateTime == null) {
            Toast.makeText(this, "请选择开始、结束时间", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDateTime.before(startDateTime)) {
            Toast.makeText(this, "结束时间，不能早于开始时间", Toast.LENGTH_SHORT).show();
            return;
        }
        ConferenceInfo info = new ConferenceInfo();
        info.setPassword(password);
        info.setConferenceTitle(titleEditText.getText().toString());
        Random random = new Random();
        String pin = String.format("%d%d%d%d", random.nextInt() % 10, random.nextInt() % 10, random.nextInt() % 10, random.nextInt() % 10);
        info.setPin(pin);

        info.setOwner(ChatManager.Instance().getUserId());
        info.setStartTime(startDateTime.getTime() / 1000);
        info.setEndTime(endDateTime.getTime() / 1000);

        WfcUIKit.getWfcUIKit().getAppServiceProvider().createConference(info, new GeneralCallback2() {
            @Override
            public void onSuccess(String s) {
                Toast.makeText(OrderConferenceActivity.this, "预定会议成功", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFail(int i) {
                Log.e(TAG, "createConference fail" + i);
            }
        });
    }

}
