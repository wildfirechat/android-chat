package cn.wildfire.chat.kit.group.manage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.JoinGroupRequest;

public class JoinGroupRequestListFragment extends Fragment {
    private String groupId;
    private GroupViewModel groupViewModel;
    private UserViewModel userViewModel;
    private JoinGroupRequestAdapter adapter;

    LinearLayout noRequestLinearLayout;
    LinearLayout requestListLinearLayout;
    RecyclerView recyclerView;

    public static JoinGroupRequestListFragment newInstance(String groupId) {
        Bundle args = new Bundle();
        args.putString("groupId", groupId);
        JoinGroupRequestListFragment fragment = new JoinGroupRequestListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        groupId = getArguments().getString("groupId");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_join_request_fragment, container, false);
        noRequestLinearLayout = view.findViewById(R.id.noRequestLinearLayout);
        requestListLinearLayout = view.findViewById(R.id.requestListLinearLayout);
        recyclerView = view.findViewById(R.id.requestListRecyclerView);

        init();
        return view;
    }

    private void init() {
        groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
        groupViewModel.clearJoinGroupRequestUnread(groupId);
        userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        userViewModel.userInfoLiveData().observe(this, userInfos -> {
            loadRequests();
        });
        loadRequests();
    }

    private void loadRequests() {
        // status -1 means all
        groupViewModel.getJoinGroupRequests(groupId, null, -1).observe(getViewLifecycleOwner(), new Observer<List<JoinGroupRequest>>() {
            @Override
            public void onChanged(List<JoinGroupRequest> joinGroupRequests) {
                if (joinGroupRequests != null && !joinGroupRequests.isEmpty()) {
                    noRequestLinearLayout.setVisibility(View.GONE);
                    requestListLinearLayout.setVisibility(View.VISIBLE);

                    if (adapter == null) {
                        adapter = new JoinGroupRequestAdapter(JoinGroupRequestListFragment.this);
                        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                        recyclerView.setAdapter(adapter);
                    }
                    adapter.setJoinGroupRequests(joinGroupRequests);
                } else {
                    noRequestLinearLayout.setVisibility(View.VISIBLE);
                    requestListLinearLayout.setVisibility(View.GONE);
                }
            }
        });
    }

    public void reload() {
        loadRequests();
    }
}
