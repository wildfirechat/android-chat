package cn.wildfire.chat.kit.contact.group;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.newfriend.JoinGroupRequestListAdapter;


public class JoinGroupFragment extends Fragment {

    LinearLayout noNewJoinGroupLinearLayout;
    LinearLayout newGroupMemberListLinearLayout;
    RecyclerView recyclerView;

    private ContactViewModel contactViewModel;

    private JoinGroupRequestListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_join_group_fragment_layout, container, false);
        // 视图初始化
        noNewJoinGroupLinearLayout = view.findViewById(R.id.noNewJoinGroupLinearLayout);
        newGroupMemberListLinearLayout = view.findViewById(R.id.newGroupMemberListLinearLayout);
        recyclerView = view.findViewById(R.id.groupRequestListRecyclerView);
        
        // 初始化RecyclerView
        adapter = new JoinGroupRequestListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupDataObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次进入页面强制刷新数据
        contactViewModel.reloadGroupRequests();
    }

    private void setupDataObservers() {
        contactViewModel = WfcUIKit.getAppScopeViewModel(ContactViewModel.class);
        // 添加初次加载判断
        if (contactViewModel.groupRequestLiveData().getValue() == null) {
            contactViewModel.reloadGroupRequests();
        }
        contactViewModel.groupRequestLiveData().observe(getViewLifecycleOwner(), requests -> {
            if (requests != null && !requests.isEmpty()) {
                noNewJoinGroupLinearLayout.setVisibility(View.GONE);
                newGroupMemberListLinearLayout.setVisibility(View.VISIBLE);
                adapter.setGroupJoinRequests(requests);
                adapter.notifyDataSetChanged();
            } else {
                noNewJoinGroupLinearLayout.setVisibility(View.VISIBLE);
                newGroupMemberListLinearLayout.setVisibility(View.GONE);
            }
        });
    }

    // 删除原有的init()和bindViews()方法
}