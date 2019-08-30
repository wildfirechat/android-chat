package cn.wildfire.chat.kit.conversation;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.lqr.emoji.EmotionLayout;
import com.lqr.emoji.IEmotionExtClickListener;
import com.lqr.emoji.IEmotionSelectedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.audio.AudioRecorderPanel;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.MentionGroupMemberActivity;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardHeightFrameLayout;
import cn.wildfire.chat.kit.widget.ViewPagerFixed;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;

import static cn.wildfire.chat.kit.conversation.ConversationActivity.REQUEST_PICK_MENTION_CONTACT;

public class ConversationInputPanel extends FrameLayout implements IEmotionSelectedListener {

    @Bind(R.id.inputContainerLinearLayout)
    LinearLayout inputContainerLinearLayout;
    @Bind(R.id.disableInputTipTextView)
    TextView disableInputTipTextView;

    @Bind(R.id.audioImageView)
    ImageView audioImageView;
    @Bind(R.id.audioButton)
    Button audioButton;
    @Bind(R.id.editText)
    EditText editText;
    @Bind(R.id.emotionImageView)
    ImageView emotionImageView;
    @Bind(R.id.extImageView)
    ImageView extImageView;
    @Bind(R.id.sendButton)
    Button sendButton;

    @Bind(R.id.emotionContainerFrameLayout)
    KeyboardHeightFrameLayout emotionContainerFrameLayout;
    @Bind(R.id.emotionLayout)
    EmotionLayout emotionLayout;
    @Bind(R.id.extContainerContainerLayout)
    KeyboardHeightFrameLayout extContainerFrameLayout;

    @Bind(R.id.conversationExtViewPager)
    ViewPagerFixed extViewPager;

    ConversationExtension extension;
    private Conversation conversation;
    private ConversationViewModel conversationViewModel;
    private InputAwareLayout rootLinearLayout;
    private FragmentActivity activity;
    private AudioRecorderPanel audioRecorderPanel;

    private long lastTypingTime;
    private String draftString;
    private static final int TYPING_INTERVAL_IN_SECOND = 10;
    private SharedPreferences sharedPreferences;

    private OnConversationInputPanelStateChangeListener onConversationInputPanelStateChangeListener;

    public ConversationInputPanel(@NonNull Context context) {
        super(context);
    }

    public ConversationInputPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

    }

    public ConversationInputPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConversationInputPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    public void setOnConversationInputPanelStateChangeListener(OnConversationInputPanelStateChangeListener onConversationInputPanelStateChangeListener) {
        this.onConversationInputPanelStateChangeListener = onConversationInputPanelStateChangeListener;
    }

    public void bind(FragmentActivity activity, InputAwareLayout rootInputAwareLayout) {

    }

    public void setupConversation(ConversationViewModel conversationViewModel, Conversation conversation) {
        this.conversationViewModel = conversationViewModel;
        this.conversation = conversation;
        this.extension.bind(conversationViewModel, conversation);

        setDraft();
    }

    public void disableInput(String tip) {
        collapse();
        inputContainerLinearLayout.setVisibility(GONE);
        disableInputTipTextView.setVisibility(VISIBLE);
        disableInputTipTextView.setText(tip);
    }

    public void enableInput() {
        inputContainerLinearLayout.setVisibility(VISIBLE);
        disableInputTipTextView.setVisibility(GONE);
    }

    public void onDestroy() {
        this.extension.onDestroy();
    }

    public void init(FragmentActivity activity, InputAwareLayout rootInputAwareLayout) {
        LayoutInflater.from(getContext()).inflate(R.layout.conversation_input_panel, this, true);
        ButterKnife.bind(this, this);

        this.activity = activity;
        this.rootLinearLayout = rootInputAwareLayout;

        this.extension = new ConversationExtension(activity, this, extViewPager);


        sharedPreferences = getContext().getSharedPreferences("sticker", Context.MODE_PRIVATE);

        // emotion
        emotionLayout.attachEditText(editText);
        emotionLayout.setEmotionAddVisiable(true);
        emotionLayout.setEmotionSettingVisiable(true);

        editText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                Editable buffer = ((EditText) v).getText();
                // If the cursor is at the end of a MentionSpan then remove the whole span
                int start = Selection.getSelectionStart(buffer);
                int end = Selection.getSelectionEnd(buffer);
                if (start == end) {
                    MentionSpan[] mentions = buffer.getSpans(start, end, MentionSpan.class);
                    if (mentions.length > 0) {
                        buffer.replace(
                                buffer.getSpanStart(mentions[0]),
                                buffer.getSpanEnd(mentions[0]),
                                ""
                        );
                        buffer.removeSpan(mentions[0]);
                        return true;
                    }
                }
                return false;
            }
            return false;
        });

        // audio record panel
        audioRecorderPanel = new AudioRecorderPanel(getContext());
        audioRecorderPanel.setRecordListener(new AudioRecorderPanel.OnRecordListener() {
            @Override
            public void onRecordSuccess(String audioFile, int duration) {
                //发送文件
                File file = new File(audioFile);
                if (file.exists()) {
                    conversationViewModel.sendAudioFile(Uri.parse(audioFile), duration);
                }
            }

            @Override
            public void onRecordFail(String reason) {
                Toast.makeText(activity, reason, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRecordStateChanged(AudioRecorderPanel.RecordState state) {
                if (state == AudioRecorderPanel.RecordState.START) {
                    TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_VOICE);
                    conversationViewModel.sendMessage(content);
                }
            }
        });

        // emotion
        emotionLayout.setEmotionSelectedListener(this);
        emotionLayout.setEmotionExtClickListener(new IEmotionExtClickListener() {
            @Override
            public void onEmotionAddClick(View view) {
                Toast.makeText(activity, "add", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onEmotionSettingClick(View view) {
                Toast.makeText(activity, "setting", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @OnClick(R.id.extImageView)
    void onExtImageViewClick() {
        if (audioButton.getVisibility() == VISIBLE) {
            return;
        }
        if (rootLinearLayout.getCurrentInput() == extContainerFrameLayout) {
            rootLinearLayout.showSoftkey(editText);
            hideConversationExtension();
        } else {
            emotionImageView.setImageResource(R.mipmap.ic_cheat_emo);
            showConversationExtension();
        }
    }

    @OnClick(R.id.emotionImageView)
    void onEmotionImageViewClick() {
        if (audioButton.getVisibility() == VISIBLE) {
            return;
        }
        if (rootLinearLayout.getCurrentInput() == emotionContainerFrameLayout) {
            hideEmotionLayout();
        } else {
            hideAudioButton();
            showEmotionLayout();
        }
    }

    @OnTextChanged(value = R.id.editText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void onInputTextChanged(CharSequence s, int start, int before, int count) {
        if (activity.getCurrentFocus() == editText) {
            if (conversation.type == Conversation.ConversationType.Group) {
                if (count == 1 && s.charAt(start) == '@') {
//                    if (start == 0 || s.charAt(start - 1) == ' ') {
                    mentionGroupMember();
//                    }
                }
            }
        }
    }

    private void mentionGroupMember() {
        Intent intent = new Intent(activity, MentionGroupMemberActivity.class);
        GroupViewModel groupViewModel = ViewModelProviders.of(activity).get(GroupViewModel.class);
        GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
        intent.putExtra("groupInfo", groupInfo);
        activity.startActivityForResult(intent, REQUEST_PICK_MENTION_CONTACT);
    }

    @OnTextChanged(value = R.id.editText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void afterInputTextChanged(Editable editable) {
        if (editText.getText().toString().trim().length() > 0) {
            if (activity.getCurrentFocus() == editText) {
                notifyTyping(TypingMessageContent.TYPING_TEXT);
            }
            sendButton.setVisibility(View.VISIBLE);
            extImageView.setVisibility(View.GONE);
        } else {
            sendButton.setVisibility(View.GONE);
            extImageView.setVisibility(View.VISIBLE);
        }
    }

    @OnClick(R.id.audioImageView)
    public void showRecordPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                return;
            }
        }

        if (audioButton.isShown()) {
            hideAudioButton();
            editText.requestFocus();
            rootLinearLayout.showSoftkey(editText);
        } else {
//            editText.clearFocus();
            showAudioButton();
            hideEmotionLayout();
            rootLinearLayout.hideSoftkey(editText, null);
            hideConversationExtension();
        }
    }

    @OnClick(R.id.sendButton)
    void sendMessage() {
        Editable content = editText.getText();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        TextMessageContent txtContent = new TextMessageContent(content.toString().trim());
        if (conversation.type == Conversation.ConversationType.Group) {
            MentionSpan[] mentions = content.getSpans(0, content.length(), MentionSpan.class);
            if (mentions != null && mentions.length > 0) {
                txtContent.mentionedType = 1;
                List<String> mentionedIds = new ArrayList<>();
                for (MentionSpan span : mentions) {
                    if (!mentionedIds.contains(span.getUid())) {
                        mentionedIds.add(span.getUid());
                    }
                    // mentionAll
                    if (span.isMentionAll()) {
                        txtContent.mentionedType = 2;
                        break;
                    }
                }
                if (txtContent.mentionedType == 1) {
                    txtContent.mentionedTargets = mentionedIds;
                }
            }
        }
        conversationViewModel.sendTextMsg(txtContent);
        editText.setText("");
    }

    public void onKeyboardShown() {
        hideEmotionLayout();
    }

    public void onKeyboardHidden() {
        // do nothing
    }

    private void setDraft() {
        ConversationInfo conversationInfo = conversationViewModel.getConversationInfo(conversation);
        if (conversationInfo == null) {
            return;
        }
        Draft draft = Draft.fromDraftJson(conversationInfo.draft);
        draftString = draft == null ? "" : draft.getContent();

        editText.setText(draftString);
    }

    public void onActivityPause() {
        Editable editable = editText.getText();
        if (TextUtils.isEmpty(editable.toString().trim())) {
            if (!TextUtils.isEmpty(draftString)) {
                conversationViewModel.saveDraft(conversation, null);
            }
            return;
        }
        if (TextUtils.equals(this.draftString, editable)) {
            return;
        }
        String draft = Draft.toDraftJson(editable);
        conversationViewModel.saveDraft(conversation, draft);
    }

    private void showAudioButton() {
        audioButton.setVisibility(View.VISIBLE);
        audioRecorderPanel.attach(rootLinearLayout, audioButton);
        editText.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        audioImageView.setImageResource(R.mipmap.ic_cheat_keyboard);
        rootLinearLayout.hideCurrentInput(editText);
        rootLinearLayout.hideAttachedInput(true);
    }


    private void hideAudioButton() {
        audioButton.setVisibility(View.GONE);
        audioRecorderPanel.deattch();
        editText.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(editText.getText())) {
            sendButton.setVisibility(View.GONE);
        } else {
            sendButton.setVisibility(View.VISIBLE);
        }
        rootLinearLayout.show(editText, emotionContainerFrameLayout);
        audioImageView.setImageResource(R.mipmap.ic_cheat_voice);
    }

    private void showEmotionLayout() {
        audioButton.setVisibility(View.GONE);
        emotionImageView.setImageResource(R.mipmap.ic_cheat_keyboard);
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelExpanded();
        }
    }

    private void hideEmotionLayout() {
        emotionImageView.setImageResource(R.mipmap.ic_cheat_emo);
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelCollapsed();
        }
    }

    private void showConversationExtension() {
        rootLinearLayout.show(editText, extContainerFrameLayout);
        if (audioButton.isShown()) {
            hideAudioButton();
        }
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelExpanded();
        }
    }

    private void hideConversationExtension() {
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelCollapsed();
        }
    }

    void collapse() {
        extension.reset();
        emotionImageView.setImageResource(R.mipmap.ic_cheat_emo);
        rootLinearLayout.hideAttachedInput(true);
        rootLinearLayout.hideCurrentInput(editText);
    }

    private void notifyTyping(int type) {
        if (conversation.type == Conversation.ConversationType.Single) {
            long now = System.currentTimeMillis();
            if (now - lastTypingTime > TYPING_INTERVAL_IN_SECOND * 1000) {
                lastTypingTime = now;
                TypingMessageContent content = new TypingMessageContent(type);
                conversationViewModel.sendMessage(content);
            }
        }
    }

    @Override
    public void onEmojiSelected(String key) {
//        LogUtils.e("onEmojiSelected : " + key);
    }

    @Override
    public void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath) {
        String remoteUrl = sharedPreferences.getString(stickerBitmapPath, null);
        conversationViewModel.sendStickerMsg(stickerBitmapPath, remoteUrl);
    }


    public interface OnConversationInputPanelStateChangeListener {
        /**
         * 输入面板展开
         */
        void onInputPanelExpanded();

        /**
         * 输入面板关闭
         */
        void onInputPanelCollapsed();
    }
}
