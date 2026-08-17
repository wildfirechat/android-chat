/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;

/**
 * 群「全员禁言 / 例外名单」页。手机端装在 {@link GroupMuteOrAllowActivity} 这个空壳里，
 * 平板上同一份实现进右栏。
 * <p>
 * 顶部开关控制全员禁言；下半屏嵌一个成员列表，全员禁言开着时它是「白名单（允许发言）」，
 * 关着时是「黑名单（禁止发言）」，所以开关一变就要整块换掉。
 */
public class GroupMuteOrAllowFragment extends Fragment {

    private SwitchMaterial switchButton;
    private GroupInfo groupInfo;
    private GroupViewModel groupViewModel;
    private GroupMemberMuteOrAllowListFragment listFragment;

    /**
     * 没有 groupInfo 就无从谈起，返回 null 让调用方放弃。
     */
    @Nullable
    public static GroupMuteOrAllowFragment fromIntent(@Nullable Intent intent) {
        GroupInfo groupInfo = intent == null ? null : intent.getParcelableExtra("groupInfo");
        if (groupInfo == null) {
            return null;
        }
        GroupMuteOrAllowFragment fragment = new GroupMuteOrAllowFragment();
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_manage_mute_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        switchButton = view.findViewById(R.id.muteSwitchButton);
        groupInfo = getArguments() == null ? null : getArguments().getParcelable("groupInfo");
        if (groupInfo == null) {
            return;
        }

        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        // 别处（比如另一端）改了群设置，这里的 groupInfo 要跟着换新，否则后续操作拿的是旧快照
        groupViewModel.groupInfoUpdateLiveData().observe(getViewLifecycleOwner(), groupInfos -> {
            if (groupInfos == null) {
                return;
            }
            for (GroupInfo info : groupInfos) {
                if (info.target.equals(groupInfo.target)) {
                    groupInfo = info;
                    break;
                }
            }
        });

        switchButton.setChecked(groupInfo.mute == 1);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) ->
            groupViewModel.muteAll(groupInfo.target, isChecked, null, Collections.singletonList(0))
                .observe(getViewLifecycleOwner(), booleanOperateResult -> {
                    if (!booleanOperateResult.isSuccess()) {
                        switchButton.setChecked(!isChecked);
                        Toast.makeText(getActivity(),
                            getString(R.string.mute_operation_failed, booleanOperateResult.getErrorCode()),
                            Toast.LENGTH_SHORT).show();
                    } else {
                        showMemberList(true);
                    }
                }));

        showMemberList(false);
    }

    /**
     * 装配下半屏的成员列表。
     * <p>
     * 用 {@code getChildFragmentManager()} 而不是 Activity 的：本页在平板上只是右栏栈上的一层，
     * 用 Activity 的 FragmentManager 会把这个列表挂到双栏主界面上去，且 {@code containerFrameLayout}
     * 这个 id 在壳布局里也有一个，会挂错地方。子 FragmentManager 只在本页视图里找容器。
     */
    private void showMemberList(boolean forceUpdate) {
        if (listFragment == null || forceUpdate) {
            listFragment = GroupMemberMuteOrAllowListFragment.newInstance(groupInfo, groupInfo.mute == 1);
        }
        getChildFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, listFragment)
            .commit();
    }
}
