package cn.wildfire.chat.kit.search.media;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfirechat.model.Conversation;

public class ConversationMediaFragment extends Fragment {
    private RecyclerView recyclerView;
    private ConversationMediaAdapter adapter;
    private ConversationMediaViewModel viewModel;
    private Conversation conversation;
    private List<ConversationMediaViewModel.MediaItem> allMediaItems = new ArrayList<>();

    public static ConversationMediaFragment newInstance(Conversation conversation) {
        ConversationMediaFragment fragment = new ConversationMediaFragment();
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
        return inflater.inflate(R.layout.fragment_conversation_media, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);

        viewModel = new ViewModelProvider(this).get(ConversationMediaViewModel.class);
        viewModel.setConversation(conversation);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        viewModel.loadMediaMessages().observe(getViewLifecycleOwner(), mediaGroups -> {
            if (mediaGroups != null) {
                allMediaItems.clear();
                for (ConversationMediaViewModel.MediaMonthGroup group : mediaGroups) {
                    allMediaItems.addAll(group.items);
                }

                adapter = new ConversationMediaAdapter(mediaGroups, this::onMediaItemClick);
                recyclerView.setAdapter(adapter);
            }
        });
    }

    private void onMediaItemClick(ConversationMediaViewModel.MediaItem mediaItem) {
        List<MediaEntry> entries = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < allMediaItems.size(); i++) {
            MediaEntry entry = new MediaEntry(allMediaItems.get(i).message);
            entries.add(entry);

            if (allMediaItems.get(i).message.messageId == mediaItem.message.messageId) {
                currentIndex = i;
            }
        }

        boolean isSecretChat = conversation.type == Conversation.ConversationType.SecretChat;
        MMPreviewActivity.previewMedia(getContext(), entries, currentIndex, isSecretChat);
    }
}

