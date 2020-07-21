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
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.GroupInfo;

public class GroupManageFragment extends Fragment {
    private GroupInfo groupInfo;
    @BindView(R2.id.joinOptionItemView)
    OptionItemView joinOptionItemView;
    @BindView(R2.id.searchOptionItemView)
    OptionItemView searchOptionItemView;
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
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        groupViewModel.groupInfoUpdateLiveData().observe(getActivity(), new Observer<List<GroupInfo>>() {
            @Override
            public void onChanged(List<GroupInfo> groupInfos) {
                for (GroupInfo info : groupInfos) {
                    if (info.target.equals(groupInfo.target)) {
                        groupInfo = info;
                        break;
                    }
                }

            }
        });
    }

    @OnClick(R2.id.managerOptionItemView)
    void showGroupManagerSetting() {
        Intent intent = new Intent(getActivity(), GroupManagerListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.muteOptionItemView)
    void showGroupMuteSetting() {
        Intent intent = new Intent(getActivity(), GroupMuteActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);

    }

    @OnClick(R2.id.permissionOptionItemView)
    void showMemberPermissionSetting() {
        Intent intent = new Intent(getActivity(), GroupMemberPermissionActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R2.id.joinOptionItemView)
    void showJoinTypeSetting() {
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

    @OnClick(R2.id.searchOptionItemView)
    void showSearchSetting() {
        new MaterialDialog.Builder(getActivity())
            .items(R.array.group_search_type)
            .itemsCallback((dialog, itemView, position, text) -> {
                searchOptionItemView.setDesc((String) text);
            })
            .show();
    }
}
