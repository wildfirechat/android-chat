/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.workspace;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Calendar;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.UserInfo;

/**
 * 平板双栏下工作台 tab 的左栏。
 * <p>
 * 工作台的正文是一整个远端网页（见 {@link WebViewFragment}），塞进左栏没法看，所以左栏
 * 让给一块迎宾面板（问候语 + 日期），真正的网页常驻右栏栈底 —— 与 flutter 端
 * {@code chat/lib/pad/pad_workspace_welcome.dart} 的 {@code PadWorkspaceWelcome} 同一套形态。
 * 手机不走这里（工作台在手机上就是整页网页）。
 * <p>
 * 这里刻意只放"不点也不动"的静态信息：左栏一旦出现可点的入口，用户就会预期它在右栏里打开，
 * 而右栏此刻被工作台网页占着，两者会互相打架。
 */
public class WorkspaceWelcomeFragment extends Fragment {

    private ImageView portraitImageView;
    private TextView greetingTextView;
    private TextView dayTextView;
    private TextView dateTextView;

    private UserViewModel userViewModel;
    private String displayName;

    private final Observer<List<UserInfo>> userInfoLiveDataObserver = userInfos -> {
        if (userInfos == null || userViewModel == null) {
            return;
        }
        for (UserInfo info : userInfos) {
            if (info.uid.equals(userViewModel.getUserId())) {
                updateUserInfo(info);
                break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.workspace_welcome_fragment, container, false);
        portraitImageView = view.findViewById(R.id.workspacePortraitImageView);
        greetingTextView = view.findViewById(R.id.workspaceGreetingTextView);
        dayTextView = view.findViewById(R.id.workspaceDayTextView);
        dateTextView = view.findViewById(R.id.workspaceDateTextView);

        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.getUserInfoAsync(userViewModel.getUserId(), false)
            .observe(getViewLifecycleOwner(), info -> {
                if (info != null) {
                    updateUserInfo(info);
                }
            });
        userViewModel.userInfoLiveData().observeForever(userInfoLiveDataObserver);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        userViewModel.userInfoLiveData().removeObserver(userInfoLiveDataObserver);
    }

    private void updateUserInfo(UserInfo userInfo) {
        RequestOptions options = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .circleCrop();
        Glide.with(this)
            .load(userInfo.portrait)
            .apply(options)
            .into(portraitImageView);
        displayName = userViewModel.getUserDisplayName(userInfo);
        greetingTextView.setText(greetingText(displayName));
    }

    private void updateDate() {
        Calendar calendar = Calendar.getInstance();
        // 时间段（早上好/中午好...）可能因为面板一直停留在前台而跨界，每次可见都重算一次
        greetingTextView.setText(greetingText(displayName));
        dayTextView.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
        dateTextView.setText(getString(R.string.workspace_welcome_date_format,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)));
    }

    private CharSequence greetingText(@Nullable String displayName) {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour < 6) {
            greetingRes = R.string.workspace_greeting_night;
        } else if (hour < 12) {
            greetingRes = R.string.workspace_greeting_morning;
        } else if (hour < 14) {
            greetingRes = R.string.workspace_greeting_noon;
        } else if (hour < 18) {
            greetingRes = R.string.workspace_greeting_afternoon;
        } else {
            greetingRes = R.string.workspace_greeting_evening;
        }
        String greeting = getString(greetingRes);
        if (displayName == null || displayName.trim().isEmpty()) {
            return greeting;
        }
        return getString(R.string.workspace_greeting_format, greeting, displayName.trim());
    }
}
