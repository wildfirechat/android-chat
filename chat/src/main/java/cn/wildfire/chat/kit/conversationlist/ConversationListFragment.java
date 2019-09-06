package cn.wildfire.chat.kit.conversationlist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.conversationlist.notification.ConnectionStatusNotification;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotificationViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.client.ConnectionStatus;
import cn.wildfirechat.model.Conversation;

public class ConversationListFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConversationListAdapter adapter;
    private static final List<Conversation.ConversationType> types = Arrays.asList(Conversation.ConversationType.Single,
            Conversation.ConversationType.Group,
            Conversation.ConversationType.Channel);
    private static final List<Integer> lines = Arrays.asList(0);

    private ConversationListViewModel conversationListViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversationlist_frament, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadConversations();
    }

    private void init() {
        adapter = new ConversationListAdapter(this);
        conversationListViewModel = ViewModelProviders
                .of(getActivity(), new ConversationListViewModelFactory(types, lines))
                .get(ConversationListViewModel.class);
        conversationListViewModel.getConversationListAsync(types, lines)
                .observe(this, conversationInfos -> {
                    adapter.setConversationInfos(conversationInfos);
                });
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        DividerItemDecoration itemDecor = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        itemDecor.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.recyclerview_horizontal_divider));
        recyclerView.addItemDecoration(itemDecor);
        recyclerView.setAdapter(adapter);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        conversationListViewModel.conversationInfoLiveData().observe(this, conversationInfo -> reloadConversations());
        conversationListViewModel.conversationRemovedLiveData().observe(this, conversation -> reloadConversations());
        conversationListViewModel.settingUpdateLiveData().observe(this, o -> reloadConversations());

        StatusNotificationViewModel statusNotificationViewModel = WfcUIKit.getAppScopeViewModel(StatusNotificationViewModel.class);
        statusNotificationViewModel.statusNotificationLiveData().observe(this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                adapter.updateStatusNotification(statusNotificationViewModel.getNotificationItems());
            }
        });
        conversationListViewModel.connectionStatusLiveData().observe(this, status -> {
            ConnectionStatusNotification connectionStatusNotification = new ConnectionStatusNotification();
            switch (status) {
                case ConnectionStatus.ConnectionStatusConnecting:
                    connectionStatusNotification.setValue("正在连接...");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusConnected:
                    statusNotificationViewModel.removeStatusNotification(connectionStatusNotification);
                    break;
                case ConnectionStatus.ConnectionStatusUnconnected:
                    connectionStatusNotification.setValue("连接失败");
                    statusNotificationViewModel.showStatusNotification(connectionStatusNotification);
                    break;
                default:
                    break;
            }
        });
    }

    private void reloadConversations() {
        conversationListViewModel.getConversationListAsync(types, lines)
                .observe(this, conversationInfos -> {
                    adapter.setConversationInfos(conversationInfos);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

}
