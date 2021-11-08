/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import static cn.wildfire.chat.kit.conversation.ConversationFragment.REQUEST_PICK_MENTION_CONTACT;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.lqr.emoji.EmotionLayout;
import com.lqr.emoji.IEmotionExtClickListener;
import com.lqr.emoji.IEmotionSelectedListener;
import com.lqr.emoji.LQREmotionKit;
import com.lqr.emoji.MoonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.audio.AudioRecorderPanel;
import cn.wildfire.chat.kit.audio.PttPanel;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.Mention;
import cn.wildfire.chat.kit.conversation.mention.MentionGroupMemberActivity;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardHeightFrameLayout;
import cn.wildfire.chat.kit.widget.ViewPagerFixed;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.remote.ChatManager;

public class ConversationInputPanel extends FrameLayout implements IEmotionSelectedListener {

    @BindView(R2.id.inputContainerLinearLayout)
    LinearLayout inputContainerLinearLayout;
    @BindView(R2.id.disableInputTipTextView)
    TextView disableInputTipTextView;

    @BindView(R2.id.audioImageView)
    ImageView audioImageView;
    @BindView(R2.id.pttImageView)
    ImageView pttImageView;
    @BindView(R2.id.audioButton)
    Button audioButton;
    @BindView(R2.id.editText)
    EditText editText;
    @BindView(R2.id.emotionImageView)
    ImageView emotionImageView;
    @BindView(R2.id.extImageView)
    ImageView extImageView;
    @BindView(R2.id.sendButton)
    Button sendButton;

    @BindView(R2.id.emotionContainerFrameLayout)
    KeyboardHeightFrameLayout emotionContainerFrameLayout;
    @BindView(R2.id.emotionLayout)
    EmotionLayout emotionLayout;
    @BindView(R2.id.extContainerContainerLayout)
    KeyboardHeightFrameLayout extContainerFrameLayout;

    @BindView(R2.id.conversationExtViewPager)
    ViewPagerFixed extViewPager;

    @BindView(R2.id.refRelativeLayout)
    RelativeLayout refRelativeLayout;
    @BindView(R2.id.refEditText)
    EditText refEditText;

    ConversationExtension extension;
    private Conversation conversation;
    private MessageViewModel messageViewModel;
    private ConversationViewModel conversationViewModel;
    private InputAwareLayout rootLinearLayout;
    private Fragment fragment;
    private FragmentActivity activity;
    private AudioRecorderPanel audioRecorderPanel;
    private PttPanel pttPanel;

    private long lastTypingTime;
    private String draftString;
    private static final int TYPING_INTERVAL_IN_SECOND = 10;
    private static final int MAX_EMOJI_PER_MESSAGE = 50;
    private int messageEmojiCount = 0;
    private SharedPreferences sharedPreferences;

    private boolean isPttMode = false;

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

    public void setupConversation(Conversation conversation) {
        this.conversation = conversation;
        this.extension.bind(this.messageViewModel, conversation);

        setDraft();
    }

    private QuoteInfo quoteInfo;

    public void quoteMessage(Message message) {
        this.quoteInfo = QuoteInfo.initWithMessage(message);
        refRelativeLayout.setVisibility(VISIBLE);
        refEditText.setText(quoteInfo.getUserDisplayName() + ": " + quoteInfo.getMessageDigest());
    }

    private void clearQuoteMessage() {
        if (refRelativeLayout.getVisibility() == VISIBLE) {
            refEditText.setText("");
            refRelativeLayout.setVisibility(GONE);
        }
        quoteInfo = null;
    }

    public void disableInput(String tip) {
        closeConversationInputPanel();
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

    public void init(Fragment fragment, InputAwareLayout rootInputAwareLayout) {
        LayoutInflater.from(getContext()).inflate(R.layout.conversation_input_panel, this, true);
        ButterKnife.bind(this, this);

        this.activity = fragment.getActivity();
        this.fragment = fragment;
        this.rootLinearLayout = rootInputAwareLayout;

        this.extension = new ConversationExtension(fragment, this, extViewPager);


        sharedPreferences = getContext().getSharedPreferences("sticker", Context.MODE_PRIVATE);

        // emotion
        emotionLayout.setEmotionAddVisiable(true);
        emotionLayout.setEmotionSettingVisiable(true);

        // audio record panel
        audioRecorderPanel = new AudioRecorderPanel(getContext());
        audioRecorderPanel.setRecordListener(new AudioRecorderPanel.OnRecordListener() {
            @Override
            public void onRecordSuccess(String audioFile, int duration) {
                //发送文件
                File file = new File(audioFile);
                if (file.exists()) {
                    messageViewModel.sendAudioFile(conversation, Uri.parse(audioFile), duration);
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
                    messageViewModel.sendMessage(conversation, content);
                }
            }
        });
        SharedPreferences sp = fragment.getContext().getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        boolean pttEnabled = sp.getBoolean("pttEnabled", true);
        if (pttEnabled && PTTClient.checkAddress(ChatManager.Instance().getHost())){
            pttImageView.setVisibility(View.VISIBLE);
            pttPanel = new PttPanel(getContext());
        }

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

        messageViewModel = ViewModelProviders.of(fragment).get(MessageViewModel.class);
        conversationViewModel = ViewModelProviders.of(fragment).get(ConversationViewModel.class);

    }

    @OnClick(R2.id.extImageView)
    void onExtImageViewClick() {
        if (audioButton.getTag() != null) {
            return;
        }
        if (rootLinearLayout.getCurrentInput() == extContainerFrameLayout) {
            hideConversationExtension();
            rootLinearLayout.showSoftkey(editText);
        } else {
            emotionImageView.setImageResource(R.mipmap.ic_cheat_emo);
            showConversationExtension();
        }
    }

    @OnClick(R2.id.emotionImageView)
    void onEmotionImageViewClick() {

        if (audioRecorderPanel.isShowingRecorder() || (pttPanel != null && pttPanel.isShowingTalking())) {
            return;
        }
        if (rootLinearLayout.getCurrentInput() == emotionContainerFrameLayout) {
            hideEmotionLayout();
            rootLinearLayout.showSoftkey(editText);
        } else {
            hideAudioButton();
            showEmotionLayout();
        }
    }

    @OnClick(R2.id.clearRefImageButton)
    void onClearRefImageButtonClick() {
        clearQuoteMessage();
        updateConversationDraft();
    }

    @OnTextChanged(value = R2.id.editText, callback = OnTextChanged.Callback.TEXT_CHANGED)
    void onInputTextChanged(CharSequence s, int start, int before, int count) {
        if (activity.getCurrentFocus() == editText) {
            if (conversation.type == Conversation.ConversationType.Group) {
                if (before == 0 && count == 1 && s.charAt(start) == '@') {
//                    if (start == 0 || s.charAt(start - 1) == ' ') {
                    mentionGroupMember();
//                    }
                }
                // delete
                if (before == 1 && count == 0) {
                    Editable text = editText.getText();
                    MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
                    if (spans != null) {
                        for (MentionSpan span : spans) {
                            if (text.getSpanEnd(span) == start && text.getSpanFlags(span) == Spanned.SPAN_INCLUSIVE_EXCLUSIVE) {
                                text.delete(text.getSpanStart(span), text.getSpanEnd(span));
                                text.removeSpan(span);
                                break;
                            } else if (start >= text.getSpanStart(span) && start < text.getSpanEnd(span)) {
                                text.removeSpan(span);
                                break;
                            }
                        }
                    }
                }

                // insert
                if (before == 0 && count > 0) {
                    Editable text = editText.getText();
                    MentionSpan[] spans = text.getSpans(0, text.length(), MentionSpan.class);
                    if (spans != null) {
                        for (MentionSpan span : spans) {
                            if (start >= text.getSpanStart(span) && start < text.getSpanEnd(span)) {
                                text.removeSpan(span);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void mentionGroupMember() {
        Intent intent = new Intent(activity, MentionGroupMemberActivity.class);
        GroupViewModel groupViewModel = ViewModelProviders.of(fragment).get(GroupViewModel.class);
        GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
        intent.putExtra("groupInfo", groupInfo);
        fragment.startActivityForResult(intent, REQUEST_PICK_MENTION_CONTACT);
    }

    @OnTextChanged(value = R2.id.editText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
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

    @OnClick({R2.id.audioImageView, R2.id.pttImageView})
    public void showRecordPanel(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                fragment.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                return;
            }
        }

        if (audioButton.isShown()) {
            hideAudioButton();
            editText.requestFocus();
            rootLinearLayout.showSoftkey(editText);
        } else {
//            editText.clearFocus();

            if (view.getId() == R.id.pttImageView) {
                isPttMode = true;
            }
            showAudioButton();
            hideEmotionLayout();
            rootLinearLayout.hideSoftkey(editText, null);
            hideConversationExtension();
        }
    }


    @OnClick(R2.id.sendButton)
    void sendMessage() {
        messageEmojiCount = 0;
        Editable content = editText.getText();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        TextMessageContent txtContent = new TextMessageContent(content.toString().trim());
        if (this.quoteInfo != null) {
            txtContent.setQuoteInfo(quoteInfo);
        }
        clearQuoteMessage();

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
        messageViewModel.sendTextMsg(conversation, txtContent);
        editText.setText("");
    }

    public void onKeyboardShown() {
        hideEmotionLayout();
    }

    public void onKeyboardHidden() {
        // do nothing
    }

    private void setDraft() {
        MentionSpan[] spans = editText.getText().getSpans(0, editText.getText().length(), MentionSpan.class);
        if (spans != null) {
            for (MentionSpan span : spans) {
                editText.getText().removeSpan(span);
            }
        }
        ConversationInfo conversationInfo = conversationViewModel.getConversationInfo(conversation);
        if (conversationInfo == null || TextUtils.isEmpty(conversationInfo.draft)) {
            return;
        }
        Draft draft = Draft.fromDraftJson(conversationInfo.draft);
        if (draft == null || (TextUtils.isEmpty(draft.getContent()) && draft.getQuoteInfo() == null)) {
            return;
        }
        draftString = draft.getContent();
        messageEmojiCount = draft.getEmojiCount();

        quoteInfo = draft.getQuoteInfo();
        if (quoteInfo != null) {
            refRelativeLayout.setVisibility(VISIBLE);
            refEditText.setText(quoteInfo.getUserDisplayName() + ": " + quoteInfo.getMessageDigest());
        }

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(draftString);
        List<Mention> mentions = draft.getMentions();
        if (mentions != null) {
            for (Mention mention : mentions) {
                if (mention.isMentionAll()) {
                    spannableStringBuilder.setSpan(new MentionSpan(true), mention.getStart(), mention.getEnd(), Spanned.SPAN_MARK_MARK);
                } else {
                    spannableStringBuilder.setSpan(new MentionSpan(mention.getUid()), mention.getStart(), mention.getEnd(), Spanned.SPAN_MARK_MARK);
                }
            }
        }

        editText.setText(spannableStringBuilder);
//         FIXME: 4/16/21 恢复草稿时，消息列表界面会抖动，且没有滑动到最后
//        editText.requestFocus();
    }

    public void setInputText(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        editText.setText(text);
        editText.setSelection(text.length());
        editText.requestFocus();
        rootLinearLayout.showSoftkey(editText);
    }

    public void onActivityPause() {
        updateConversationDraft();
    }

    private void updateConversationDraft() {
        Editable editable = editText.getText();
        String draft = Draft.toDraftJson(editable, messageEmojiCount, quoteInfo);
        if (conversation != null) {
            messageViewModel.saveDraft(conversation, draft);
        }
    }

    private void showAudioButton() {
        audioButton.setVisibility(View.VISIBLE);
        if (isPttMode) {
            pttPanel.attach(rootLinearLayout, audioButton, conversation);
        } else {
            audioRecorderPanel.attach(rootLinearLayout, audioButton);
        }
        editText.setVisibility(View.GONE);
        extImageView.setVisibility(VISIBLE);
        sendButton.setVisibility(View.GONE);
        audioImageView.setImageResource(R.mipmap.ic_cheat_keyboard);
        rootLinearLayout.hideCurrentInput(editText);
        rootLinearLayout.hideAttachedInput(true);
    }


    private void hideAudioButton() {
        audioButton.setVisibility(View.GONE);
        audioRecorderPanel.deattch();
        if (pttPanel != null){
            pttPanel.deattch();
            isPttMode = false;
        }
        editText.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(editText.getText())) {
            extImageView.setVisibility(VISIBLE);
            sendButton.setVisibility(View.GONE);
        } else {
            extImageView.setVisibility(GONE);
            sendButton.setVisibility(View.VISIBLE);
        }
        audioImageView.setImageResource(R.mipmap.ic_cheat_voice);
    }

    private void showEmotionLayout() {
        audioButton.setVisibility(View.GONE);
        emotionImageView.setImageResource(R.mipmap.ic_cheat_keyboard);
        rootLinearLayout.show(editText, emotionContainerFrameLayout);
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

    void closeConversationInputPanel() {
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
                messageViewModel.sendMessage(conversation, content);
            }
        }
    }

    @Override
    public void onEmojiSelected(String key) {
        Editable editable = editText.getText();
        if (key.equals("/DEL")) {
            messageEmojiCount--;
            messageEmojiCount = messageEmojiCount < 0 ? 0 : messageEmojiCount;
            editText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        } else {
            if (messageEmojiCount >= MAX_EMOJI_PER_MESSAGE) {
                Toast.makeText(activity, "最多允许输入" + MAX_EMOJI_PER_MESSAGE + "个表情符号", Toast.LENGTH_SHORT).show();
                return;
            }
            messageEmojiCount++;
            int code = Integer.decode(key);
            char[] chars = Character.toChars(code);
            String value = Character.toString(chars[0]);
            for (int i = 1; i < chars.length; i++) {
                value += Character.toString(chars[i]);
            }

            int start = editText.getSelectionStart();
            int end = editText.getSelectionEnd();
            start = (start < 0 ? 0 : start);
            end = (start < 0 ? 0 : end);
            editable.replace(start, end, value);

            int editEnd = editText.getSelectionEnd();
            MoonUtils.replaceEmoticons(LQREmotionKit.getContext(), editable, 0, editable.toString().length());
            editText.setSelection(editEnd);
        }
    }

    @Override
    public void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath) {
        String remoteUrl = sharedPreferences.getString(stickerBitmapPath, null);
        messageViewModel.sendStickerMsg(conversation, stickerBitmapPath, remoteUrl);
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
