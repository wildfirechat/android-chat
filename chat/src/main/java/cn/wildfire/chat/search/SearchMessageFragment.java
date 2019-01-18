package cn.wildfire.chat.search;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import cn.wildfirechat.chat.R;

import java.util.List;
import java.util.Objects;

import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.conversation.ConversationActivity;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;

public class SearchMessageFragment extends Fragment implements SearchMessageResultAdapter.OnMessageClickListener {
    private Conversation conversation;
    private String keyword;
    private SearchViewModel searchViewModel;
    private SearchMessageResultAdapter adapter;
    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;
    @Bind(R.id.categoryTextView)
    TextView categoryTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_message_result_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        searchViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);
        adapter = new SearchMessageResultAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    public void search(Conversation conversation, String keyword) {
        if (Objects.equals(this.conversation, conversation) && Objects.equals(this.keyword, keyword)) {
            return;
        }
        adapter.reset();
        adapter.setOnMessageClickListener(this);
        searchViewModel.searchMessage(conversation, keyword).observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(@Nullable List<Message> messages) {
                adapter.setMessages(messages);
            }
        });
    }

    public void reset() {
        adapter.reset();
    }

    @Override
    public void onMessageClick(Message message) {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        intent.putExtra("conversation", message.conversation);
        intent.putExtra("toFocusMessageUid", message.messageUid);
        startActivity(intent);
        getActivity().finish();
    }
}
