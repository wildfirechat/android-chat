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

public class GroupMuteFragment extends Fragment {
    private GroupInfo groupInfo;

    @BindView(R.id.muteSwitchButton)
    SwitchButton switchButton;

    public static GroupMuteFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        GroupMuteFragment fragment = new GroupMuteFragment();
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
        View view = inflater.inflate(R.layout.group_manage_mute_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        switchButton.setCheckedNoEvent(groupInfo.mute == 1);
        switchButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            groupViewModel.muteAll(groupInfo.target, isChecked, null, Collections.singletonList(0)).observe(this, booleanOperateResult -> {
                if (!booleanOperateResult.isSuccess()) {
                    switchButton.setCheckedNoEvent(!isChecked);
                    Toast.makeText(getActivity(), "禁言失败 " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
