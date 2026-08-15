/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.wildfire.chat.kit.AppServiceProvider;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.TextEditPageFragment;
import cn.wildfire.chat.kit.widget.LengthFilter;
import cn.wildfirechat.model.GroupInfo;

/**
 * 「群公告」整页，逐行搬自 {@link SetGroupAnnouncementActivity}。
 * <p>
 * 与其它文本编辑页的差别：公告要先从 app server 拉一次，且只有当输入与服务端当前公告
 * <strong>不同</strong>时保存才可用。
 */
public class SetGroupAnnouncementPageFragment extends TextEditPageFragment {

    private GroupInfo groupInfo;
    private GroupAnnouncement currentGroupAnnouncement;

    public static SetGroupAnnouncementPageFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        SetGroupAnnouncementPageFragment fragment = new SetGroupAnnouncementPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static SetGroupAnnouncementPageFragment fromIntent(Intent intent) {
        GroupInfo groupInfo = intent.getParcelableExtra("groupInfo");
        return groupInfo == null ? null : newInstance(groupInfo);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments() == null ? null : getArguments().getParcelable("groupInfo");
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_set_announcement_activity;
    }

    @Override
    protected int editTextId() {
        return R.id.announcementEditText;
    }

    @Override
    public int pageMenu() {
        return R.menu.group_set_group_name;
    }

    @Override
    protected int confirmMenuItemId() {
        return R.id.confirm;
    }

    @Override
    protected void onPageViewCreated(@NonNull View view) {
        editText.setFilters(new InputFilter[]{
            new LengthFilter(2000, maxTextLength ->
                Toast.makeText(getActivity(), getString(R.string.group_announcement_max_length), Toast.LENGTH_SHORT).show())
        });
        WfcUIKit.getWfcUIKit().getAppServiceProvider().getGroupAnnouncement(groupInfo.target,
            new AppServiceProvider.GetGroupAnnouncementCallback() {
                @Override
                public void onUiSuccess(GroupAnnouncement announcement) {
                    if (!isAdded()) {
                        return;
                    }
                    currentGroupAnnouncement = announcement;
                    if (TextUtils.isEmpty(editText.getText())) {
                        editText.setText(announcement.text);
                    }
                    updateConfirmState();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    if (isAdded()) {
                        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
                    }
                }
            });
    }

    /**
     * 与服务端当前公告一致时不允许保存。公告还没拉回来（或拉取失败）时退化成「有内容就能存」，
     * 与改造前一致 —— 否则第一次设置公告时会因为拉不到旧公告而永远存不了。
     */
    @Override
    protected boolean isConfirmEnabled() {
        return currentGroupAnnouncement == null
            ? !TextUtils.isEmpty(text())
            : !TextUtils.equals(text(), currentGroupAnnouncement.text);
    }

    @Override
    protected void onConfirm(String text) {
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.processing)
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        WfcUIKit.getWfcUIKit().getAppServiceProvider().updateGroupAnnouncement(groupInfo.target, text,
            new AppServiceProvider.UpdateGroupAnnouncementCallback() {
                @Override
                public void onUiSuccess(GroupAnnouncement announcement) {
                    if (!isAdded()) {
                        return;
                    }
                    dialog.dismiss();
                    Toast.makeText(getActivity(), getString(R.string.set_group_announcement_success), Toast.LENGTH_SHORT).show();
                    finishPage();
                }

                @Override
                public void onUiFailure(int code, String msg) {
                    if (!isAdded()) {
                        return;
                    }
                    dialog.dismiss();
                    Toast.makeText(getActivity(),
                        getString(R.string.set_group_announcement_failed, code, msg), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
