package cn.wildfire.chat.kit.search.bydate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.message.Message;

public class ConversationSearchByDateFragment extends Fragment {
    private RecyclerView monthRecyclerView;
    private MonthListAdapter monthAdapter;
    private ConversationSearchByDateViewModel viewModel;
    private Conversation conversation;

    public static ConversationSearchByDateFragment newInstance(Conversation conversation) {
        ConversationSearchByDateFragment fragment = new ConversationSearchByDateFragment();
        Bundle args = new Bundle();
        args.putParcelable("conversation", conversation);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversation = getArguments().getParcelable("conversation");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_conversation_search_by_date, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        monthRecyclerView = view.findViewById(R.id.monthRecyclerView);

        viewModel = new ViewModelProvider(this).get(ConversationSearchByDateViewModel.class);
        viewModel.setConversation(conversation);

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        monthRecyclerView.setLayoutManager(layoutManager);

        // 设置日期点击监听
        CalendarView.OnDateClickListener dateClickListener = (year, month, day) -> {
            viewModel.getFirstMessageOfDay(year, month, day).observe(getViewLifecycleOwner(), message -> {
                if (message != null) {
                    jumpToConversationAndHighlight(message);
                } else {
                    Toast.makeText(getContext(), "未找到消息", Toast.LENGTH_SHORT).show();
                }
            });
        };

        // 加载月份列表
        viewModel.loadMonths().observe(getViewLifecycleOwner(), months -> {
            monthAdapter = new MonthListAdapter(months, dateClickListener);
            monthRecyclerView.setAdapter(monthAdapter);

            android.util.Log.d("CalendarDebug", "Fragment: Received " + (months != null ? months.size() : 0) + " months");

            // 滚动到当前月份（第一个位置）
            if (months != null && !months.isEmpty()) {
                monthRecyclerView.post(() -> {
                    monthRecyclerView.scrollToPosition(0);
                    android.util.Log.d("CalendarDebug", "Fragment: Scrolled to position 0");
                });
            }
        });

        // 观察每个月的消息数据更新
        viewModel.getMonthMessageCountLiveData().observe(getViewLifecycleOwner(), monthData -> {
            if (monthAdapter != null) {
                monthAdapter.updateMonthData(monthData.index, monthData.dayMessageCount);
            }
        });
    }

    private void jumpToConversationAndHighlight(Message message) {
        Intent intent = ConversationActivity.buildConversationIntent(
                getContext(),
                message.conversation,
                null,
                message.messageId
        );
        intent.putExtra("highlightMessageId", message.messageId);
        startActivity(intent);
    }
}
