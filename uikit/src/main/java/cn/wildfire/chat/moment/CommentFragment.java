package cn.wildfire.chat.moment;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardAwareLinearLayout;
import cn.wildfire.chat.kit.R;

public class CommentFragment extends Fragment implements KeyboardAwareLinearLayout.OnKeyboardHiddenListener,
        KeyboardAwareLinearLayout.OnKeyboardShownListener {
    private InputAwareLayout inputAwareLayout;
    private FeedCommentPanel commentInputPanel;
    private EditText editText;

    private long feedId;
    private long commentId;

    private int commentPanelTop = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            feedId = bundle.getLong("feedId", 0);
            commentId = bundle.getLong("commentId", 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.moment_comment_fragment, container, false);
        inputAwareLayout = view.findViewById(R.id.rootInputAwareLayout);
        inputAwareLayout.addOnKeyboardHiddenListener(this);
        inputAwareLayout.addOnKeyboardShownListener(this);
        commentInputPanel = view.findViewById(R.id.commentPanel);

        commentInputPanel.init(this, inputAwareLayout);
        editText = commentInputPanel.editText;
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        inputAwareLayout.showSoftkey(editText);
        commentPanelTop = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("commentPanelTop", 0);
    }

    @Override
    public void onKeyboardShown() {
        commentInputPanel.onKeyboardShown();
        if (commentPanelTop > 0) {
            return;
        }

        commentInputPanel.post(() -> {
            int[] location = new int[2];
            commentInputPanel.getLocationInWindow(location);
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putInt("commentPanelTop", location[1])
                    .apply();
        });
    }

    /**
     * 把评论交回给承载本输入条的那一页。
     * <p>
     * 改造前取的是 {@code getActivity()}（当时那一页就是 Activity）；现在本输入条挂在
     * {@link BaseFeedPageFragment} 的子 FragmentManager 上，所以取父 Fragment ——
     * 平板右栏里同时可能开着好几页朋友圈，取 Activity 会拿到双栏主界面。
     */
    public void comment(String comment) {
        if (!(getParentFragment() instanceof BaseFeedPageFragment)) {
            return;
        }
        BaseFeedPageFragment page = (BaseFeedPageFragment) getParentFragment();
        if (feedId > 0 && commentId == 0) {
            page.commentFeed(feedId, comment);
            return;
        }
        if (feedId > 0 && commentId > 0) {
            page.replyComment(feedId, commentId, comment);
        }

    }

    public void hideKeyboard() {
        inputAwareLayout.hideSoftkey(editText, null);
    }

    @Override
    public void onKeyboardHidden() {
        commentInputPanel.onKeyboardHidden();
    }
}
