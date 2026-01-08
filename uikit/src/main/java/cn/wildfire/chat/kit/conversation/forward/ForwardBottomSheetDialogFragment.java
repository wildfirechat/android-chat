package cn.wildfire.chat.kit.conversation.forward;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.utils.WfcTextUtils;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.Conversation;

public class ForwardBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private List<Conversation> targetConversations;
    private List<Message> messages;
    private OnSendListener onSendListener;
    private View bottomButtonLayout;
    private Button sendButtonSmall;
    private View rootView;
    private BottomSheetBehavior<View> behavior;

    public static ForwardBottomSheetDialogFragment newInstance(List<Conversation> conversations, List<Message> messages) {
        ForwardBottomSheetDialogFragment fragment = new ForwardBottomSheetDialogFragment();
        fragment.targetConversations = conversations;
        fragment.messages = messages;
        return fragment;
    }

    public void setOnSendListener(OnSendListener onSendListener) {
        this.onSendListener = onSendListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_forward_bottom_sheet, container, false);
        rootView = view;
        initView(view);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                behavior = BottomSheetBehavior.from(bottomSheet);
                // 确保默认是展开状态，或者是 collapsed 但允许展开
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }

        // Add global layout listener to detect keyboard
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (rootView != null) {
            rootView.getViewTreeObserver().removeOnGlobalLayoutListener(globalLayoutListener);
        }
    }

    private ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Rect r = new Rect();
            rootView.getWindowVisibleDisplayFrame(r);
            int screenHeight = rootView.getRootView().getHeight();
            int keypadHeight = screenHeight - r.bottom;

            if (keypadHeight > screenHeight * 0.15) { // Keyboard is open
                updateUiForKeyboard(true);
            } else { // Keyboard is close
                updateUiForKeyboard(false);
            }
        }
    };

    private void updateUiForKeyboard(boolean isKeyboardVisible) {
        if (bottomButtonLayout == null || sendButtonSmall == null) return;

        if (isKeyboardVisible) {
            if (behavior != null && behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            if (bottomButtonLayout.getVisibility() == View.VISIBLE) {
                bottomButtonLayout.setVisibility(View.GONE);
                sendButtonSmall.setVisibility(View.VISIBLE);
            }
        } else {
            if (bottomButtonLayout.getVisibility() == View.GONE) {
                bottomButtonLayout.setVisibility(View.VISIBLE);
                sendButtonSmall.setVisibility(View.GONE);
            }
        }
    }

    private void initView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.targetRecyclerView);
        TextView messagePreviewTextView = view.findViewById(R.id.messagePreviewTextView);
        EditText messageEditText = view.findViewById(R.id.messageEditText);
        Button cancelButton = view.findViewById(R.id.cancelButton);
        Button sendButton = view.findViewById(R.id.sendButton);
        sendButtonSmall = view.findViewById(R.id.sendButtonSmall);
        bottomButtonLayout = view.findViewById(R.id.bottomButtonLayout);

        messagePreviewTextView.setText(getMessagesPreview());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ForwardTargetAdapter adapter = new ForwardTargetAdapter(targetConversations);
        recyclerView.setAdapter(adapter);

        cancelButton.setOnClickListener(v -> dismiss());

        View.OnClickListener sendListener = v -> {
            if (onSendListener != null) {
                String message = messageEditText.getText().toString().trim();
                onSendListener.onSend(message);
            }
            dismiss();
        };

        sendButton.setOnClickListener(sendListener);
        sendButtonSmall.setOnClickListener(sendListener);

        // Remove OnFocusChangeListener as we use OnGlobalLayoutListener now
    }

    private String getMessagesPreview() {
        if (messages.size() == 1) {
            Message message = messages.get(0);
            if (message.content instanceof ImageMessageContent) {
                return "[图片]";
            } else if (message.content instanceof VideoMessageContent) {
                return "[视频]";
            } else if (message.content instanceof CompositeMessageContent) {
                return "[聊天记录]";
            } else {
                return WfcTextUtils.htmlToText(message.digest());
            }
        } else {
            return getString(R.string.forward_batch_messages, messages.size());
        }
    }


    public interface OnSendListener {
        void onSend(String extraMessage);
    }
}
