package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.ProgressFragment;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupMember;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class GroupMemberListFragment extends ProgressFragment implements GroupMemberListAdapter.OnMemberClickListener {
    private GroupInfo groupInfo;
    private GroupMemberListAdapter groupMemberListAdapter;

    @BindView(R.id.memberRecyclerView)
    RecyclerView memberRecyclerView;

    public static GroupMemberListFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupMemberListFragment fragment = new GroupMemberListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_member_list;
    }

    @Override
    protected void afterViews(View view) {
        super.afterViews(view);
        ButterKnife.bind(this, view);
        groupMemberListAdapter = new GroupMemberListAdapter(groupInfo);
        memberRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 5));
        memberRecyclerView.setAdapter(groupMemberListAdapter);
        groupMemberListAdapter.setOnMemberClickListener(this);
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(this, userInfos -> loadAndShowGroupMembers());
        loadAndShowGroupMembers();
    }

    private void loadAndShowGroupMembers() {
        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupMemberUserInfosLiveData(groupInfo.target, false).observe(this, uiUserInfos -> {
            showContent();
            groupMemberListAdapter.setMembers(uiUserInfos);
            groupMemberListAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onUserMemberClick(UserInfo userInfo) {
        GroupMember groupMember = ChatManager.Instance().getGroupMember(groupInfo.target, ChatManager.Instance().getUserId());
        if (groupInfo != null && groupInfo.privateChat == 1 && groupMember.type == GroupMember.GroupMemberType.Normal) {
            return;
        }
        Intent intent = new Intent(getActivity(), UserInfoActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }
}
