package cn.wildfire.chat.kit.search.bydate;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

public class ConversationMessageByDateFragment extends Fragment {
    private RecyclerView monthRecyclerView;
    private View emptyView;
    private TextView emptyTextView;
    private MonthListAdapter monthAdapter;
    private ConversationMessageByDateViewModel viewModel;
    private Conversation conversation;

    public static ConversationMessageByDateFragment newInstance(Conversation conversation) {
        ConversationMessageByDateFragment fragment = new ConversationMessageByDateFragment();
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
        emptyView = view.findViewById(R.id.emptyView);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        viewModel = new ViewModelProvider(this).get(ConversationMessageByDateViewModel.class);
        viewModel.setConversation(conversation);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        monthRecyclerView.setLayoutManager(layoutManager);

        MessageCountCalendarView.OnDateClickListener dateClickListener = (year, month, day) -> {
            viewModel.getFirstMessageOfDay(year, month, day).observe(getViewLifecycleOwner(), message -> {
                if (message != null) {
                    jumpToConversationAndHighlight(message);
                } else {
                    Toast.makeText(getContext(), "未找到消息", Toast.LENGTH_SHORT).show();
                }
            });
        };

        viewModel.loadMonths().observe(getViewLifecycleOwner(), months -> {
            if (months == null || months.isEmpty()) {
                monthRecyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyTextView.setText(R.string.no_message_records);
            } else {
                monthRecyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                monthAdapter = new MonthListAdapter(months, dateClickListener);
                monthRecyclerView.setAdapter(monthAdapter);
                monthRecyclerView.post(() -> monthRecyclerView.scrollToPosition(0));
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
