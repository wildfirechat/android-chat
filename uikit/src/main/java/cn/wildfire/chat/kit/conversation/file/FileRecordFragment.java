package cn.wildfire.chat.kit.conversation.file;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.FileRecord;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FileRecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileRecordFragment extends Fragment {

    private static final String CONVERSATION = "conversation";

    @BindView(R2.id.fileRecordLinearLayout)
    LinearLayout fileRecordLinearLayout;
    @BindView(R2.id.fileRecordRecyclerView)
    RecyclerView fileRecordRecyclerView;
    @BindView(R2.id.tipTextView)
    TextView tipTextView;

    private boolean isLoading = false;
    private FileRecordViewModel fileRecordViewModel;

    private FileRecordAdapter fileRecordAdapter;

    private Conversation conversation;

    public FileRecordFragment() {
        // Required empty public constructor
    }

    public static FileRecordFragment newInstance(Conversation conversation) {
        FileRecordFragment fragment = new FileRecordFragment();
        Bundle args = new Bundle();
        args.putParcelable(CONVERSATION, conversation);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversation = getArguments().getParcelable(CONVERSATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.file_record_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        fileRecordAdapter = new FileRecordAdapter();
        fileRecordRecyclerView.setAdapter(fileRecordAdapter);
        fileRecordRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        fileRecordRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));

        fileRecordRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    isLoading = true;
                    loadFileRecords();
                }
            }
        });

        fileRecordViewModel = new ViewModelProvider(this).get(FileRecordViewModel.class);
        loadFileRecords();
    }

    private void loadFileRecords() {
        LiveData<OperateResult<List<FileRecord>>> data;
        List<FileRecord> fileRecords = fileRecordAdapter.getFileRecords();
        long beforeMessageUid = 0;
        if (fileRecords != null && !fileRecords.isEmpty()) {
            beforeMessageUid = fileRecords.get(fileRecords.size() - 1).messageUid;
        }
        if (conversation != null) {
            data = fileRecordViewModel.getConversationFileRecords(conversation, beforeMessageUid, 20);
        } else {
            data = fileRecordViewModel.getMyFileRecords(beforeMessageUid, 20);
        }

        data.observe(getViewLifecycleOwner(), listOperateResult -> {
            if (listOperateResult.isSuccess()) {
                fileRecordAdapter.addFileRecords(listOperateResult.getResult());
                if (fileRecords != null && !fileRecords.isEmpty()) {
                    fileRecordAdapter.notifyItemInserted(fileRecords.size());
                } else {
                    fileRecordAdapter.notifyDataSetChanged();
                }
            }
            if (fileRecordAdapter.getFileRecords() == null || fileRecordAdapter.getFileRecords().isEmpty()) {
                fileRecordLinearLayout.setVisibility(View.GONE);
                tipTextView.setVisibility(View.VISIBLE);
            }
            isLoading = false;
        });
    }
}