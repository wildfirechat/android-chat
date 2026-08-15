/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import static cn.wildfirechat.model.ModifyMyInfoType.Modify_DisplayName;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.UserInfo;

/**
 * 修改自己的昵称。手机端装在 {@link ChangeMyNameActivity} 这个空壳里，平板上同一份实现进右栏。
 */
public class ChangeMyNameFragment extends Fragment implements WfcPage {

    private MenuItem confirmMenuItem;
    private EditText nameEditText;

    private UserViewModel userViewModel;
    private UserInfo userInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_change_my_name_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nameEditText = view.findViewById(R.id.nameEditText);
        nameEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmEnabled();
            }
        });

        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        if (userInfo == null) {
            Toast.makeText(getActivity(), getString(R.string.user_no_found), Toast.LENGTH_SHORT).show();
            WfcPageCompat.finishPage(this);
            return;
        }
        nameEditText.setText(userInfo.displayName);
        nameEditText.setSelection(nameEditText.getText().toString().trim().length());
    }

    @Override
    public int pageMenu() {
        return R.menu.user_change_my_name;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        confirmMenuItem = menu.findItem(R.id.save);
        // 菜单可能在输入之后才重建（右栏换页、旋转），按当前输入框内容算，不要一律置灰
        updateConfirmEnabled();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save) {
            changeMyName();
            return true;
        }
        return false;
    }

    private void updateConfirmEnabled() {
        if (confirmMenuItem != null && nameEditText != null) {
            confirmMenuItem.setEnabled(nameEditText.getText().toString().trim().length() > 0);
        }
    }

    private void changeMyName() {
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(getString(R.string.creating))
            .progress(true, 100)
            .build();
        dialog.show();
        String nickName = nameEditText.getText().toString().trim();
        ModifyMyInfoEntry entry = new ModifyMyInfoEntry(Modify_DisplayName, nickName);
        userViewModel.modifyMyInfo(Collections.singletonList(entry))
            .observe(getViewLifecycleOwner(), result -> {
                Toast.makeText(getActivity(),
                    getString(result.isSuccess() ? R.string.modify_success : R.string.modify_fail),
                    Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                WfcPageCompat.finishPage(this);
            });
    }
}
