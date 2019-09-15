package cn.wildfire.chat.kit.group;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import cn.wildfire.chat.kit.contact.pick.PickUserFragment;
import cn.wildfire.chat.kit.contact.pick.PickUserViewModel;
import cn.wildfirechat.model.GroupInfo;

public class PickGroupMemberFragment extends PickUserFragment {
    private GroupInfo groupInfo;

    public static PickGroupMemberFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        PickGroupMemberFragment fragment = new PickGroupMemberFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupInfo = getArguments().getParcelable("groupInfo");
    }

    @Override
    protected void setupPickFromUsers() {
        PickUserViewModel pickUserViewModel = ViewModelProviders.of(getActivity()).get(PickUserViewModel.class);
        GroupViewModel groupViewModel = ViewModelProviders.of(getActivity()).get(GroupViewModel.class);
        groupViewModel.getGroupMemberUIUserInfosLiveData(groupInfo.target, false).observe(this, uiUserInfos -> {
            showContent();
            pickUserViewModel.setUsers(uiUserInfos);
            userListAdapter.setUsers(uiUserInfos);
        });
    }
}
