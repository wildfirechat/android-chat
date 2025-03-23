/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
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
import androidx.lifecycle.Observer;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.GroupInfo;

public class GroupManageFragment extends Fragment {
    private GroupInfo groupInfo;
    OptionItemView joinOptionItemView;
    OptionItemView searchOptionItemView;
    OptionItemView historyOptionItemView;

    private GroupViewModel groupViewModel;

    public static GroupManageFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupManageFragment fragment = new GroupManageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
        if (groupInfo == null) {
            getActivity().finish();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_manage_fragment, container, false);
        if (groupInfo != null) {
            bindViews(view);
            bindEvents(view);
            init();
        }
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.managerOptionItemView).setOnClickListener(_v -> showGroupManagerSetting());
        view.findViewById(R.id.muteOptionItemView).setOnClickListener(_v -> showGroupMuteSetting());
        view.findViewById(R.id.permissionOptionItemView).setOnClickListener(_v -> showMemberPermissionSetting());
        view.findViewById(R.id.joinOptionItemView).setOnClickListener(_v -> showJoinTypeSetting());
        view.findViewById(R.id.searchOptionItemView).setOnClickListener(_v -> showSearchSetting());
        view.findViewById(R.id.historyMessageOptionItemView).setOnClickListener(_v -> showHistoryMessageSetting());
    }

    private void bindViews(View view) {
        joinOptionItemView = view.findViewById(R.id.joinOptionItemView);
        searchOptionItemView = view.findViewById(R.id.searchOptionItemView);
        historyOptionItemView = view.findViewById(R.id.historyMessageOptionItemView);
    }

    private void setupGroupOptionsView(GroupInfo groupInfo) {
        String[] types = getResources().getStringArray(R.array.group_join_type);
        joinOptionItemView.setDesc(types[groupInfo.joinType]);

        types = getResources().getStringArray(R.array.group_search_type);
        searchOptionItemView.setDesc(types[groupInfo.searchable]);

        types = getResources().getStringArray(R.array.group_history_message);
        historyOptionItemView.setDesc(types[groupInfo.historyMessage]);

    }

    private void init() {
        setupGroupOptionsView(groupInfo);
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(getActivity(), new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                for (GroupInfo info : groupInfos) {
                    if (info.target.equals(groupInfo.target)) {
                        groupInfo = info;
                        setupGroupOptionsView(groupInfo);
                        break;
                    }
                }

            }
        });
    }

    void showGroupManagerSetting() {
        Intent intent = new Intent(getActivity(), GroupManagerListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    void showGroupMuteSetting() {
        Intent intent = new Intent(getActivity(), GroupMuteOrAllowActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);

    }

    void showMemberPermissionSetting() {
        Intent intent = new Intent(getActivity(), GroupMemberPermissionActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    void showJoinTypeSetting() {
        new MaterialDialog.Builder(getActivity())
            .items(R.array.group_join_type)
            .itemsCallback((dialog, itemView, position, text) -> {
                groupViewModel.setGroupJoinType(groupInfo.target, position, null, Collections.singletonList(0))
                    .observe(GroupManageFragment.this, booleanOperateResult -> {
                        if (booleanOperateResult.isSuccess()) {
                            joinOptionItemView.setDesc((String) text);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.modify_group_join_type_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .show();
    }

    void showSearchSetting() {
        new MaterialDialog.Builder(getActivity())
            .items(R.array.group_search_type)
            .itemsCallback((dialog, itemView, position, text) -> {
                groupViewModel.setGroupSearchType(groupInfo.target, position, null, Collections.singletonList(0))
                    .observe(GroupManageFragment.this, booleanOperateResult -> {
                        if (booleanOperateResult.isSuccess()) {
                            searchOptionItemView.setDesc((String) text);
                        } else {
                            Toast.makeText(getActivity(), getString(R.string.modify_group_search_type_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .show();
    }

    void showHistoryMessageSetting() {
        new MaterialDialog.Builder(getActivity())
            .items(R.array.group_history_message)
            .itemsCallback((dialog, itemView, position, text) -> {
                groupViewModel.setGroupHistoryMessage(groupInfo.target, position, null, Collections.singletonList(0))
                    .observe(GroupManageFragment.this, new Observer<OperateResult<Boolean>>() {
                        @Override
                        public void onChanged(OperateResult<Boolean> booleanOperateResult) {
                            if (booleanOperateResult.isSuccess()) {
                                historyOptionItemView.setDesc((String) text);
                            } else {
                                Toast.makeText(getActivity(), getString(R.string.modify_group_history_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
            })
            .show();
    }
}
