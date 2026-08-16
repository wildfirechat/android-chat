/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.pick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.UserInfo;

/**
 * 「勾一批联系人回传给调用方」整页：联系人列表 + 确定菜单 + 回传结果。
 * <p>
 * 逐行搬自 {@link PickContactActivity}（确定菜单、{@code PickUserViewModel} 装配、
 * {@code onContactPicked}），那个类现在只是手机端的壳。列表本身是父类
 * {@link PickContactFragment}，它已经用 {@code WfcPageCompat.pageScope} 取
 * {@code PickUserViewModel}，因此本页与列表、搜索、已选栏拿到的是同一个实例，
 * 且随本页出栈而清空 —— 右栏里两个 tab 各开一个选人页不会串选中状态。
 */
public class PickContactPageFragment extends PickContactFragment implements WfcPage {

    private TextView confirmTextView;

    private final Observer<Object> checkStatusObserver = obj -> updateConfirmStatus();

    public static PickContactPageFragment fromIntent(Intent intent) {
        PickContactPageFragment fragment = new PickContactPageFragment();
        Bundle args = new Bundle();
        args.putInt(PickContactActivity.PARAM_MAX_COUNT, intent.getIntExtra(PickContactActivity.PARAM_MAX_COUNT, 0));
        args.putStringArrayList(PickContactActivity.PARAM_INITIAL_CHECKED_IDS,
            intent.getStringArrayListExtra(PickContactActivity.PARAM_INITIAL_CHECKED_IDS));
        args.putStringArrayList(PickContactActivity.PARA_UNCHECKABLE_IDS,
            intent.getStringArrayListExtra(PickContactActivity.PARA_UNCHECKABLE_IDS));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        // 父类在这里才把 pickUserViewModel 取出来，之后才能配置它
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            int maxCount = args.getInt(PickContactActivity.PARAM_MAX_COUNT, 0);
            if (maxCount > 0) {
                pickUserViewModel.setMaxPickCount(maxCount);
            }
            pickUserViewModel.setInitialCheckedIds(args.getStringArrayList(PickContactActivity.PARAM_INITIAL_CHECKED_IDS));
            pickUserViewModel.setUncheckableIds(args.getStringArrayList(PickContactActivity.PARA_UNCHECKABLE_IDS));
        }
        pickUserViewModel.userCheckStatusUpdateLiveData().observeForever(checkStatusObserver);
    }

    @Override
    public void onDestroy() {
        pickUserViewModel.userCheckStatusUpdateLiveData().removeObserver(checkStatusObserver);
        super.onDestroy();
    }

    // ==================== WfcPage ====================

    @Override
    public int pageMenu() {
        return R.menu.contact_pick;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.confirm);
        if (item == null) {
            return;
        }
        View actionView = item.getActionView();
        confirmTextView = actionView == null ? null : actionView.findViewById(R.id.confirm_tv);
        if (confirmTextView != null) {
            confirmTextView.setOnClickListener(v -> onConfirmClick());
        }
        updateConfirmStatus();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            onConfirmClick();
            return true;
        }
        return false;
    }

    /**
     * 一个都没勾时把「完成」置灰。
     * <p>
     * 改造前置灰的是 {@code MenuItem}，但点击是 actionView 上的 {@code OnClickListener} 处理的，
     * {@code MenuItem} 的 enabled 拦不住它，于是空手点「完成」也会回传一个空列表。
     * 这里跟兄弟页 {@code CreateConversationPageFragment} 一致，置灰 actionView 本身。
     */
    private void updateConfirmStatus() {
        if (confirmTextView == null || pickUserViewModel == null) {
            return;
        }
        List<UIUserInfo> checked = pickUserViewModel.getCheckedUsers();
        if (checked == null || checked.isEmpty()) {
            confirmTextView.setText(R.string.contact_pick_confirm);
            confirmTextView.setEnabled(false);
        } else {
            confirmTextView.setText(getString(R.string.contact_pick_confirm_with_count, checked.size()));
            confirmTextView.setEnabled(true);
        }
    }

    private void onConfirmClick() {
        ArrayList<UserInfo> pickedInfos = new ArrayList<>();
        List<UIUserInfo> checked = pickUserViewModel.getCheckedUsers();
        if (checked != null) {
            for (UIUserInfo info : checked) {
                pickedInfos.add(info.getUserInfo());
            }
        }
        Intent data = new Intent();
        data.putExtra(PickContactActivity.RESULT_PICKED_USERS, pickedInfos);
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, data);
        WfcPageCompat.finishPage(this);
    }
}
