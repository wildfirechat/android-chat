/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.DateTimePickerHelper;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 预定会议页。
 * <p>
 * 逐行搬自 {@link OrderConferenceActivity}，那个类现在只是手机端的壳。入口是会议入口页
 * （发现 tab → 会议 → 预定会议），入口页本身就在右栏，不迁的话要整屏跳出去再跳回来。
 */
public class OrderConferencePageFragment extends Fragment implements WfcPage {

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

    public static OrderConferencePageFragment fromIntent(Intent intent) {
        return new OrderConferencePageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.av_conference_order_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleEditText = view.findViewById(R.id.conferenceTitleTextInputEditText);
        audienceSwitch = view.findViewById(R.id.audienceSwitch);
        passwordSwitch = view.findViewById(R.id.passwordSwitch);
        modeSwitch = view.findViewById(R.id.modeSwitch);
        advancedSwitch = view.findViewById(R.id.advanceSwitch);
        endDateTimeTextView = view.findViewById(R.id.endDateTimeTextView);
        startDateTimeTextView = view.findViewById(R.id.startDateTimeTextView);
        passwordTextView = view.findViewById(R.id.passwordTextView);

        view.findViewById(R.id.endDateTimeRelativeLayout).setOnClickListener(v -> pickEndDateTime());
        view.findViewById(R.id.startDateTimeRelativeLayout).setOnClickListener(v -> pickStartDateTime());

        audienceSwitch.setOnCheckedChangeListener(this::audienceChecked);
        passwordSwitch.setOnCheckedChangeListener(this::passwordChecked);
        titleEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                conferenceTitle(s);
            }
        });

        UserInfo userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(getString(R.string.conference_title_default, userInfo.displayName));
        } else {
            titleEditText.setText(getString(R.string.conference_title_unnamed));
        }
        advancedSwitch.setChecked(false);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        endDateTime = calendar.getTime();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.order_conference;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        orderConferenceMenuItem = menu.findItem(R.id.create);
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createConference();
            return true;
        }
        return false;
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
            new MaterialDialog.Builder(requireContext())
                .content(R.string.conference_enter_password)
                .input(getString(R.string.conference_password_hint), "123456", false, new MaterialDialog.InputCallback() {
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
        DateTimePickerHelper.pickDateTime(requireContext(), new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(getContext(), R.string.conference_end_time_invalid, Toast.LENGTH_SHORT).show();
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
        DateTimePickerHelper.pickDateTime(requireContext(), new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(getContext(), R.string.conference_start_time_invalid, Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getContext(), R.string.conference_time_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (endDateTime.before(startDateTime)) {
            Toast.makeText(getContext(), R.string.conference_end_before_start_error, Toast.LENGTH_SHORT).show();
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

        WfcUIKit.getWfcUIKit().getAppServiceProvider().createConference(info, new AppServiceProvider.CreateConferenceCallback() {
            @Override
            public void onSuccess(String s) {
                if (getView() == null) {
                    return;
                }
                Toast.makeText(getContext(), R.string.conference_order_success, Toast.LENGTH_SHORT).show();
                WfcPageCompat.finishPage(OrderConferencePageFragment.this);
            }

            @Override
            public void onFail(int code, String message) {
                if (getView() == null) {
                    return;
                }
                String errorMsg = !TextUtils.isEmpty(message) ? message : getString(R.string.conference_order_failed, code);
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "createConference fail, code: " + code + ", message: " + message);
            }
        });
    }
}
