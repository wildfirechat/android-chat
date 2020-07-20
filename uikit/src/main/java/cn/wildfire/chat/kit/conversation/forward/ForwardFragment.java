package cn.wildfire.chat.kit.conversation.forward;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.kit.conversationlist.ConversationListViewModelFactory;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class ForwardFragment extends Fragment implements ForwardAdapter.OnConversationItemClickListener, ForwardAdapter.OnNewConversationItemClickListener {
    private static final int REQUEST_CODE_PICK_CONVERSATION_TARGET = 100;
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.forward_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }


    private void init() {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
        ForwardAdapter adapter = new ForwardAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
                Conversation.ConversationType.Group);
        List<Integer> liens = Arrays.asList(0);
        List<ConversationInfo> conversationInfos = conversationListViewModel.getConversationList(types, liens);
        adapter.setConversations(conversationInfos);
        adapter.setOnConversationItemClickListener(this);
        adapter.setNewConversationItemClickListener(this);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConversationItemClick(ConversationInfo conversationInfo) {
        ((ForwardActivity) getActivity()).forward(conversationInfo.conversation);
    }

    @Override
    public void onNewConversationItemClick() {
        Intent intent = new Intent(getActivity(), PickConversationTargetToForwardActivity.class);
        startActivityForResult(intent, 100);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO 现在就支持转发给一个人
        if (requestCode == REQUEST_CODE_PICK_CONVERSATION_TARGET && resultCode == Activity.RESULT_OK) {
            GroupInfo groupInfo = data.getParcelableExtra("groupInfo");
            if (groupInfo != null) {
                ((ForwardActivity) getActivity()).forward(groupInfo);
                return;
            }
            UserInfo userInfo = data.getParcelableExtra("userInfo");
            if (userInfo != null) {
                ((ForwardActivity) getActivity()).forward(userInfo);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
