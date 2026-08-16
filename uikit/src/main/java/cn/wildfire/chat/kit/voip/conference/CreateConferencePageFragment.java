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
import android.widget.Button;
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
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.voip.conference.model.ConferenceInfo;
import cn.wildfire.chat.kit.widget.DateTimePickerHelper;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * 发起会议页。
 * <p>
 * 逐行搬自 {@link CreateConferenceActivity}，那个类现在只是手机端的壳。入口是会议入口页
 * （发现 tab → 会议 → 发起会议），入口页本身就在右栏，不迁的话要整屏跳出去再跳回来。
 * <strong>「创建并进入」仍启动全屏的 {@link ConferenceActivity}</strong>——音视频界面
 * 本来就是全屏形态，不在右栏里。
 */
public class CreateConferencePageFragment extends Fragment implements WfcPage {

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

    public static CreateConferencePageFragment fromIntent(Intent intent) {
        return new CreateConferencePageFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.av_conference_create_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleEditText = view.findViewById(R.id.conferenceTitleTextInputEditText);
        audienceSwitch = view.findViewById(R.id.audienceSwitch);
        passwordSwitch = view.findViewById(R.id.passwordSwitch);
        modeSwitch = view.findViewById(R.id.modeSwitch);
        advancedSwitch = view.findViewById(R.id.advanceSwitch);
        userCallIdSwitch = view.findViewById(R.id.userCallIdSwitch);
        joinConferenceButton = view.findViewById(R.id.joinConferenceBtn);
        endDateTimeTextView = view.findViewById(R.id.endDateTimeTextView);
        callIdTextView = view.findViewById(R.id.callIdTextView);
        passwordTextView = view.findViewById(R.id.passwordTextView);

        view.findViewById(R.id.endDateTimeRelativeLayout).setOnClickListener(v -> pickEndDateTime());
        joinConferenceButton.setOnClickListener(v -> onClickJoinBtn());
        audienceSwitch.setOnCheckedChangeListener(this::audienceChecked);
        passwordSwitch.setOnCheckedChangeListener(this::passwordChecked);
        titleEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                conferenceTitle(s);
            }
        });

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

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.create_conference;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        createConferenceMenuItem = menu.findItem(R.id.create);
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create) {
            createConference(false);
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
        DateTimePickerHelper.pickDateTime(requireContext(), new DateTimePickerHelper.PickDateTimeCallBack() {
            @Override
            public void onPick(Date date) {
                if (date.getTime() < System.currentTimeMillis()) {
                    Toast.makeText(getContext(), R.string.conference_end_time_invalid, Toast.LENGTH_SHORT).show();
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
        Toast.makeText(getContext(), R.string.conference_creating, Toast.LENGTH_SHORT).show();
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

        WfcUIKit.getWfcUIKit().getAppServiceProvider().createConference(info, new AppServiceProvider.CreateConferenceCallback() {
            @Override
            public void onSuccess(String conferenceId) {
                if (getView() == null) {
                    return;
                }
                info.setConferenceId(conferenceId);
                if (join) {
                    AVEngineKit.CallSession session = AVEngineKit.Instance().joinConference(conferenceId, false, info.getPin(), info.getOwner(), info.getConferenceTitle(), "", info.isAudience(), info.isAdvance(), !enableAudio, !enableVideo, false, null);
                    if (session != null) {
                        Intent intent = new Intent(getContext(), ConferenceActivity.class);
                        // 会议界面是全屏形态，不登记右栏，走原始 startActivity
                        startActivity(intent);
                        ConferenceManager.getManager().setCurrentConferenceInfo(info);
                        WfcPageCompat.finishAfterOpeningPage(CreateConferencePageFragment.this);
                    } else {
                        Toast.makeText(getContext(), R.string.conference_create_failed, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    WfcPageCompat.finishPage(CreateConferencePageFragment.this);
                }
            }

            @Override
            public void onFail(int code, String message) {
                if (getView() == null) {
                    return;
                }
                String errorMsg = !TextUtils.isEmpty(message) ? message : getString(R.string.conference_create_failed_code, code);
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "createConference fail, code: " + code + ", message: " + message);
                joinConferenceButton.setEnabled(true);
            }
        });
    }
}
