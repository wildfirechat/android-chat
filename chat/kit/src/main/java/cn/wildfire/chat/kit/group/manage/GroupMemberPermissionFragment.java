package cn.wildfire.chat.kit.group.manage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.kyleduo.switchbutton.SwitchButton;

import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class GroupMemberPermissionFragment extends Fragment {
    private GroupInfo groupInfo;

    @BindView(R.id.privateChatSwitchButton)
    SwitchButton privateChatSwitchButton;

    public static GroupMemberPermissionFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupMemberPermissionFragment fragment = new GroupMemberPermissionFragment();
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
        View view = inflater.inflate(R.layout.group_manage_member_permission_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        privateChatSwitchButton.setCheckedNoEvent(groupInfo.privateChat == 0);
        privateChatSwitchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            groupViewModel.preventPrivateChat(groupInfo.target, isChecked, null, Collections.singletonList(0)).observe(this, booleanOperateResult -> {
                if (!booleanOperateResult.isSuccess()) {
                    privateChatSwitchButton.setCheckedNoEvent(!isChecked);
                    Toast.makeText(getActivity(), "设置群成员权限失败 " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
