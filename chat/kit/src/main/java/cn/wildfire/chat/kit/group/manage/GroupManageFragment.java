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
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupManageFragment extends Fragment {
    private GroupInfo groupInfo;
    @BindView(R.id.joinOptionItemView)
    OptionItemView joinOptionItemView;
    @BindView(R.id.searchOptionItemView)
    OptionItemView searchOptionItemView;

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_manage_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        String[] types = getResources().getStringArray(R.array.group_join_type);
        joinOptionItemView.setDesc(types[groupInfo.joinType]);
    }

    @OnClick(R.id.managerOptionItemView)
    void showGroupManagerSetting() {
        Intent intent = new Intent(getActivity(), GroupManagerListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R.id.muteOptionItemView)
    void showGroupMuteSetting() {
        Intent intent = new Intent(getActivity(), GroupMuteActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);

    }

    @OnClick(R.id.permissionOptionItemView)
    void showMemberPermissionSetting() {
        Intent intent = new Intent(getActivity(), GroupMemberPermissionActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R.id.joinOptionItemView)
    void showJoinTypeSetting() {
        GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        new MaterialDialog.Builder(getActivity())
                .items(R.array.group_join_type)
                .itemsCallback((dialog, itemView, position, text) -> {
                    groupViewModel.setGroupJoinType(groupInfo.target, position, null, Collections.singletonList(0))
                            .observe(GroupManageFragment.this, booleanOperateResult -> {
                                if (booleanOperateResult.isSuccess()) {
                                    joinOptionItemView.setDesc((String) text);
                                } else {
                                    Toast.makeText(getActivity(), "修改加群方式失败", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .show();
    }

    @OnClick(R.id.searchOptionItemView)
    void showSearchSetting() {
        new MaterialDialog.Builder(getActivity())
                .items(R.array.group_search_type)
                .itemsCallback((dialog, itemView, position, text) -> {
                    searchOptionItemView.setDesc((String) text);
                })
                .show();
    }
}
