/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.UserInfo;

/**
 * 发送好友申请。手机端装在 {@link InviteFriendActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class InviteFriendFragment extends Fragment implements WfcPage {

    private TextView introTextView;
    private UserInfo userInfo;

    /**
     * 由启动 intent 造页面，供 {@link InviteFriendActivity} 与 {@code PaneRegistry} 共用。
     * 没有 userInfo 时返回 null —— 这一页没有对象就没有意义，右栏不应该压一层空白进去。
     */
    @Nullable
    public static InviteFriendFragment fromIntent(@Nullable Intent intent) {
        UserInfo userInfo = intent == null ? null : intent.getParcelableExtra("userInfo");
        if (userInfo == null) {
            return null;
        }
        InviteFriendFragment fragment = new InviteFriendFragment();
        Bundle args = new Bundle();
        args.putParcelable("userInfo", userInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userInfo = getArguments() == null ? null : getArguments().getParcelable("userInfo");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.contact_invite_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        introTextView = view.findViewById(R.id.introTextView);
        view.findViewById(R.id.clearImageButton).setOnClickListener(v -> introTextView.setText(""));

        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        UserInfo me = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        introTextView.setText(getString(R.string.invite_default_message, me == null ? "" : me.displayName));
    }

    @Override
    public int pageMenu() {
        return R.menu.contact_invite;
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            invite();
            return true;
        }
        return false;
    }

    private void invite() {
        ContactViewModel contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        contactViewModel.invite(userInfo.uid, introTextView.getText().toString())
            .observe(getViewLifecycleOwner(), errorCode -> {
                if (errorCode == 0) {
                    Toast.makeText(getActivity(), R.string.invite_sent, Toast.LENGTH_SHORT).show();
                    WfcPageCompat.finishPage(this);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.invite_error, errorCode), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
