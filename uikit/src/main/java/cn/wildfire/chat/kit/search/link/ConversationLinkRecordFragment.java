package cn.wildfire.chat.kit.search.link;

import android.content.Intent;
import android.net.Uri;
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

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;

public class ConversationLinkRecordFragment extends Fragment {
    private RecyclerView recyclerView;
    private View emptyView;
    private TextView emptyTextView;
    private LinkRecordAdapter adapter;
    private ConversationLinkRecordViewModel viewModel;
    private Conversation conversation;

    public static ConversationLinkRecordFragment newInstance(Conversation conversation) {
        ConversationLinkRecordFragment fragment = new ConversationLinkRecordFragment();
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
        return inflater.inflate(R.layout.fragment_conversation_link_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyView = view.findViewById(R.id.emptyView);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        viewModel = new ViewModelProvider(this).get(ConversationLinkRecordViewModel.class);
        viewModel.setConversation(conversation);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        viewModel.loadLinkMessages().observe(getViewLifecycleOwner(), linkItems -> {
            if (linkItems == null || linkItems.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
                emptyTextView.setText(R.string.no_link_records);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                adapter = new LinkRecordAdapter(linkItems, this::onLinkClick);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void onLinkClick(ConversationLinkRecordViewModel.LinkItem linkItem) {
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(R.string.open_link)
                .setItems(new CharSequence[]{getString(R.string.open_in_browser), getString(R.string.jump_to_message)}, (dialog, which) -> {
                    if (which == 0) {
                        openInBrowser(linkItem.url);
                    } else {
                        jumpToMessage(linkItem);
                    }
                })
                .show();
    }

    private void openInBrowser(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void jumpToMessage(ConversationLinkRecordViewModel.LinkItem linkItem) {
        Intent intent = ConversationActivity.buildConversationIntent(
                getContext(),
                linkItem.conversation,
                null,
                linkItem.message.messageId
        );
        startActivity(intent);
    }
}

