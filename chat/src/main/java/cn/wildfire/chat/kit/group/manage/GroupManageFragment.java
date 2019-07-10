package cn.wildfire.chat.kit.group.manage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupManageFragment extends Fragment {
    private GroupInfo groupInfo;

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
        return view;
    }

    @OnClick(R.id.managerOptionItemView)
    void showGroupManagerSetting() {
        Intent intent = new Intent(getActivity(), GroupManagerListActivity.class);
        intent.putExtra("groupInfo", groupInfo);
        startActivity(intent);
    }

    @OnClick(R.id.muteOptionItemView)
    void showGroupMuteSetting() {

    }

    @OnClick(R.id.permissionOptionItemView)
    void showMemberPermissionSetting() {

    }

    @OnClick(R.id.jionOptionItemView)
    void showJoinTypeSetting() {

    }

    @OnClick(R.id.searchOptionItemView)
    void showSearchSetting() {

    }
}
