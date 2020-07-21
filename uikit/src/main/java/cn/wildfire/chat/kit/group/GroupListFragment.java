package cn.wildfire.chat.kit.group;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetGroupsCallback;

public class GroupListFragment extends Fragment implements OnGroupItemClickListener {
    @BindView(R2.id.groupRecyclerView)
    RecyclerView recyclerView;
    @BindView(R2.id.tipTextView)
    TextView tipTextView;
    @BindView(R2.id.groupsLinearLayout)
    LinearLayout groupsLinearLayout;

    private GroupListAdapter groupListAdapter;
    private OnGroupItemClickListener onGroupItemClickListener;

    public void setOnGroupItemClickListener(OnGroupItemClickListener onGroupItemClickListener) {
        this.onGroupItemClickListener = onGroupItemClickListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.group_list_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        groupListAdapter = new GroupListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(groupListAdapter);
        groupListAdapter.setOnGroupItemClickListener(this);

        ChatManager.Instance().getFavGroups(new GetGroupsCallback() {
            @Override
            public void onSuccess(List<GroupInfo> groupInfos) {
                if (groupInfos == null || groupInfos.isEmpty()) {
                    groupsLinearLayout.setVisibility(View.GONE);
                    tipTextView.setVisibility(View.VISIBLE);
                    return;
                }
                groupListAdapter.setGroupInfos(groupInfos);
                groupListAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFail(int errorCode) {
                groupsLinearLayout.setVisibility(View.GONE);
                tipTextView.setVisibility(View.VISIBLE);
                tipTextView.setText("error: " + errorCode);
            }
        });
    }

    @Override
    public void onGroupClick(GroupInfo groupInfo) {
        if (onGroupItemClickListener != null) {
            onGroupItemClickListener.onGroupClick(groupInfo);
            return;
        }
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfo.target);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        getActivity().finish();
    }
}
