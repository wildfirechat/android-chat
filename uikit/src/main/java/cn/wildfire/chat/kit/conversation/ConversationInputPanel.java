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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.emoji2.widget.EmojiEditText;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.lqr.emoji.EmotionLayout;
import com.lqr.emoji.IEmotionExtClickListener;
import com.lqr.emoji.IEmotionSelectedListener;
import com.lqr.emoji.StickerItem;
import com.lqr.emoji.StickerManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.audio.AudioRecorderPanel;
import cn.wildfire.chat.kit.audio.PttPanel;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExtension;
import cn.wildfire.chat.kit.conversation.mention.Mention;
import cn.wildfire.chat.kit.conversation.mention.MentionGroupMemberActivity;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.imagerecommend.ImageRecommendManager;
import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfire.chat.kit.widget.ImageRecommendView;
import cn.wildfire.chat.kit.widget.InputAwareLayout;
import cn.wildfire.chat.kit.widget.KeyboardHeightFrameLayout;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfire.chat.kit.widget.StickerRecommendView;
import cn.wildfire.chat.kit.widget.ViewPagerFixed;
import cn.wildfirechat.message.ChannelMenuEventMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.ChannelMenu;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.QuoteInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.permission.PermissionKit;

public class ConversationInputPanel extends FrameLayout implements IEmotionSelectedListener {

    LinearLayout inputContainerLinearLayout;
    TextView disableInputTipTextView;

    ImageView menuImageView;
    ImageView audioImageView;
    ImageView pttImageView;
    Button audioButton;
    EmojiEditText editText;
    ImageView emotionImageView;
    ImageView extImageView;
    Button sendButton;
    StickerRecommendView mStickerRecommendView;
    private static final int STICKER_RECOMMEND_MIN_LENGTH = 2;  // 触发推荐的最小输入长度
    ImageRecommendView mImageRecommendView;
    private android.widget.PopupWindow mImageRecommendPopup;

    LinearLayout channelMenuContainerLinearLayout;

    KeyboardHeightFrameLayout emotionContainerFrameLayout;
    EmotionLayout emotionLayout;
    KeyboardHeightFrameLayout extContainerFrameLayout;

    ViewPagerFixed extViewPager;

    RelativeLayout refRelativeLayout;
    EditText refEditText;

    ConversationExtension extension;
    private Conversation conversation;
    private String targetUser;
    private MessageViewModel messageViewModel;
    private ConversationViewModel conversationViewModel;
    private InputAwareLayout rootLinearLayout;
    private Fragment fragment;
    private FragmentActivity activity;
    private AudioRecorderPanel audioRecorderPanel;
    private PttPanel pttPanel;
    private int keyboardHeight;

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

    @Deprecated
    public void setupConversation(Conversation conversation, String targetUser) {
        setupConversation(conversation, targetUser, 0);
    }

    public void setupConversation(Conversation conversation, String targetUser, int keyboardHeight) {

        this.conversation = conversation;
        this.targetUser = targetUser;
        this.extension.bind(this.messageViewModel, conversation, targetUser);
        this.keyboardHeight = keyboardHeight;

        setDraft();
        if (conversation.type == Conversation.ConversationType.Channel) {
            showChannelMenu();
        } else {
            menuImageView.setVisibility(GONE);
        }

        SharedPreferences sp = fragment.getContext().getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        boolean pttEnabled = sp.getBoolean("pttEnabled", true);
        if (pttEnabled && PTTClient.checkAddress(ChatManager.Instance().getHost()) && conversation.type != Conversation.ConversationType.Channel) {
            pttImageView.setVisibility(View.VISIBLE);
            pttPanel = new PttPanel(getContext());
        }
    }

    private QuoteInfo quoteInfo;

    public void quoteMessage(Message message) {
        this.quoteInfo = QuoteInfo.initWithMessage(message);
        if (audioButton.getVisibility() == VISIBLE) {
            hideAudioButton();
        }
        refRelativeLayout.setVisibility(VISIBLE);
        refEditText.setText(quoteInfo.getUserDisplayName() + ": " + quoteInfo.getMessageDigest());
        editText.requestFocus();
        // FYI: https://stackoverflow.com/questions/5520085/android-show-softkeyboard-with-showsoftinput-is-not-working
        rootLinearLayout.postDelayed(() -> {
            rootLinearLayout.showSoftkey(editText);
        }, 100);
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
        if (audioRecorderPanel != null) {
            audioRecorderPanel.deattch();
        }
        if (pttPanel != null) {
            pttPanel.deattch();
        }
    }

    public void init(Fragment fragment, InputAwareLayout rootInputAwareLayout) {
        LayoutInflater.from(getContext()).inflate(R.layout.conversation_input_panel, this, true);
        bindViews();

        this.activity = fragment.getActivity();
        this.fragment = fragment;
        this.rootLinearLayout = rootInputAwareLayout;

        this.extension = new ConversationExtension(fragment, this, extViewPager);


        sharedPreferences = getContext().getSharedPreferences("sticker", Context.MODE_PRIVATE);

        // emotion
        emotionLayout.setEmotionAddVisiable(false);
        emotionLayout.setEmotionSettingVisiable(false);

        // audio record panel
        audioRecorderPanel = new AudioRecorderPanel(getContext());
        audioRecorderPanel.setRecordListener(new AudioRecorderPanel.OnRecordListener() {
            @Override
            public void onRecordSuccess(String audioFile, int duration) {
                //发送文件
                File file = new File(audioFile);
                if (file.exists()) {
                    messageViewModel.sendAudioFile(conversation, toUsers(), Uri.parse(audioFile), duration);
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
                    messageViewModel.sendMessage(conversation, toUsers(), content);
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

        messageViewModel = new ViewModelProvider(fragment).get(MessageViewModel.class);
        conversationViewModel = new ViewModelProvider(fragment).get(ConversationViewModel.class);

        if (conversation != null) {
            if (conversation.type == Conversation.ConversationType.Channel) {
                showChannelMenu();
            } else {
                menuImageView.setVisibility(GONE);
            }
        }
    }

    private void bindViews() {
        inputContainerLinearLayout = findViewById(R.id.inputContainerLinearLayout);
        disableInputTipTextView = findViewById(R.id.disableInputTipTextView);
        menuImageView = findViewById(R.id.menuImageView);
        audioImageView = findViewById(R.id.audioImageView);
        pttImageView = findViewById(R.id.pttImageView);
        audioButton = findViewById(R.id.audioButton);
        editText = findViewById(R.id.editText);
        emotionImageView = findViewById(R.id.emotionImageView);
        extImageView = findViewById(R.id.extImageView);
        sendButton = findViewById(R.id.sendButton);
        channelMenuContainerLinearLayout = findViewById(R.id.channelMenuContainerLinearLayout);
        emotionContainerFrameLayout = findViewById(R.id.emotionContainerFrameLayout);
        emotionLayout = findViewById(R.id.emotionLayout);
        extContainerFrameLayout = findViewById(R.id.extContainerContainerLayout);
        extViewPager = findViewById(R.id.conversationExtViewPager);
        refRelativeLayout = findViewById(R.id.refRelativeLayout);
        refEditText = findViewById(R.id.refEditText);

        extImageView.setOnClickListener(v -> onExtImageViewClick());
        emotionImageView.setOnClickListener(v -> onEmotionImageViewClick());
        findViewById(R.id.clearRefImageButton).setOnClickListener(v -> onClearRefImageButtonClick());
        menuImageView.setOnClickListener(v -> showChannelMenu());
        audioImageView.setOnClickListener(this::showRecordPanel);
        pttImageView.setOnClickListener(this::showRecordPanel);
        sendButton.setOnClickListener(v -> sendMessage());

        // 初始化表情推荐视图
        initStickerRecommendView();

        // 初始化图片推荐视图
        initImageRecommendView();

        editText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onInputTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                afterInputTextChanged(s);
            }
        });

    }

    void onExtImageViewClick() {
        android.util.Log.d("ConversationInputPanel", "onExtImageViewClick: querying recent images");
        if (audioButton.getTag() != null) {
            return;
        }

        if (rootLinearLayout.getCurrentInput() == extContainerFrameLayout) {
            hideConversationExtension();
            rootLinearLayout.showSoftkey(editText);
        } else {
            emotionImageView.setImageResource(R.mipmap.ic_chat_emo);
            showConversationExtension();

            // 只在显示扩展面板时才查询图片
            ImageRecommendManager.getInstance().queryRecentImages(getContext(), (imagePath, uri) -> {
                android.util.Log.d("ConversationInputPanel", "ImageRecommend callback: path=" + imagePath);
                if (fragment.getActivity() != null && fragment.isResumed()) {
                    fragment.getActivity().runOnUiThread(() -> {
                        showImageRecommendPopup(imagePath);
                    });
                }
            });
        }
    }

    void onEmotionImageViewClick() {

        if (audioRecorderPanel.isShowingRecorder() || (pttPanel != null && pttPanel.isShowingTalking())) {
            return;
        }
        if (rootLinearLayout.getCurrentInput() == emotionContainerFrameLayout) {
            hideEmotionLayout();
            rootLinearLayout.showSoftkey(editText);
        } else {
            emotionImageView.setImageResource(R.mipmap.ic_chat_keyboard);
            showEmotionLayout();
        }
    }

    void onClearRefImageButtonClick() {
        clearQuoteMessage();
        updateConversationDraft();
    }

    void onInputTextChanged(CharSequence s, int start, int before, int count) {
        if (activity.getCurrentFocus() == editText) {
            if (conversation.type == Conversation.ConversationType.Group || conversation.type == Conversation.ConversationType.Single) {
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
        if (conversation.type == Conversation.ConversationType.Group) {
            GroupViewModel groupViewModel = WfcUIKit.getAppScopeViewModel(GroupViewModel.class);
            GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
            intent.putExtra("groupInfo", groupInfo);
        }
        fragment.startActivityForResult(intent, REQUEST_PICK_MENTION_CONTACT);
    }

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

        // 检查动态表情推荐
        checkStickerRecommend(editable.toString());
    }

    public void showChannelMenu() {
        ChannelInfo channelInfo = ChatManager.Instance().getChannelInfo(conversation.target, false);
        if (channelInfo.menus == null || channelInfo.menus.isEmpty()) {
            Toast.makeText(activity, fragment.getString(R.string.channel_no_menu), Toast.LENGTH_SHORT).show();
            return;
        }

        if (channelMenuContainerLinearLayout.getVisibility() == VISIBLE) {
            menuImageView.setImageResource(R.mipmap.ic_chat_menu);
            audioImageView.setVisibility(VISIBLE);
            extImageView.setVisibility(VISIBLE);
            emotionImageView.setVisibility(VISIBLE);
            editText.setVisibility(VISIBLE);
            channelMenuContainerLinearLayout.removeAllViews();
            channelMenuContainerLinearLayout.setVisibility(GONE);
            return;
        }
        menuImageView.setImageResource(R.mipmap.ic_chat_keyboard);
        audioImageView.setVisibility(GONE);
        extImageView.setVisibility(GONE);
        emotionImageView.setVisibility(GONE);
        editText.setVisibility(GONE);
        rootLinearLayout.hideSoftkey(editText, null);
        channelMenuContainerLinearLayout.setVisibility(VISIBLE);
        channelMenuContainerLinearLayout.removeAllViews();
        if (channelInfo != null && channelInfo.menus != null) {
            for (ChannelMenu menu : channelInfo.menus) {
                TextView textView = new TextView(getContext());
                if (menu.subMenus != null && menu.subMenus.size() > 0) {
                    textView.setText("≡ " + menu.name);
                } else {
                    textView.setText(menu.name);
                }
                textView.setGravity(Gravity.CENTER);
                textView.setBackgroundColor(Color.parseColor("#F8F8F8"));
                Drawable drawable = getResources().getDrawable(R.drawable.selector_common_item);
                textView.setBackground(drawable);

                textView.setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
                textView.setOnClickListener(v -> {
                    if (menu.subMenus != null && menu.subMenus.size() > 0) {
                        PopupMenu popupMenu = new PopupMenu(getContext(), textView, Gravity.TOP);
                        for (ChannelMenu sm : menu.subMenus) {
                            MenuItem item = popupMenu.getMenu().add(sm.name);
                            item.setOnMenuItemClickListener(item1 -> {
                                openChannelMenu(sm);
                                return true;
                            });

                        }
                        popupMenu.show();
                    } else {
                        openChannelMenu(menu);
                    }
                });
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT, 0);
                params.leftMargin = 2;
                params.weight = 1;
                textView.setLayoutParams(params);
                channelMenuContainerLinearLayout.addView(textView);
            }
        }
    }

    public void showRecordPanel(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, new String[]{Manifest.permission.RECORD_AUDIO});
                PermissionKit.checkThenRequestPermission(activity, activity.getSupportFragmentManager(), tuples, granted -> {
                    // do nothing
                });
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

    private void openChannelMenu(ChannelMenu menu) {
        ChannelMenuEventMessageContent content = new ChannelMenuEventMessageContent();
        content.setMenu(menu);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);

        switch (menu.type) {
            case "view":
                if (!TextUtils.isEmpty(menu.url)) {
                    WfcWebViewActivity.loadUrl(getContext(), "", menu.url);
                }
                break;
            case "miniprogram":
                break;
            default:
                break;
        }
    }

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

        if (conversation.type == Conversation.ConversationType.Group || conversation.type == Conversation.ConversationType.Single) {
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
        messageViewModel.sendTextMsg(conversation, toUsers(), txtContent);
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
            pttImageView.setImageResource(R.mipmap.ic_chat_keyboard);
        } else {
            audioRecorderPanel.attach(rootLinearLayout, audioButton);
            audioImageView.setImageResource(R.mipmap.ic_chat_keyboard);
        }
        editText.setVisibility(View.GONE);
        extImageView.setVisibility(VISIBLE);
        sendButton.setVisibility(View.GONE);
        rootLinearLayout.hideCurrentInput(editText);
        rootLinearLayout.hideAttachedInput(true);
    }


    private void hideAudioButton() {
        audioButton.setVisibility(View.GONE);
        if (isPttMode) {
            pttPanel.deattch();
            pttImageView.setImageResource(R.mipmap.ic_ptt);
            isPttMode = false;
        } else {
            audioRecorderPanel.deattch();
            audioImageView.setImageResource(R.mipmap.ic_chat_voice);
        }
        editText.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(editText.getText())) {
            extImageView.setVisibility(VISIBLE);
            sendButton.setVisibility(View.GONE);
        } else {
            extImageView.setVisibility(GONE);
            sendButton.setVisibility(View.VISIBLE);
        }
    }

    private void showEmotionLayout() {
        rootLinearLayout.show(editText, emotionContainerFrameLayout);
        if (audioButton.isShown()) {
            hideAudioButton();
        }
        if (onConversationInputPanelStateChangeListener != null) {
            onConversationInputPanelStateChangeListener.onInputPanelExpanded();
        }
    }

    private void hideEmotionLayout() {
        emotionImageView.setImageResource(R.mipmap.ic_chat_emo);
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
        emotionImageView.setImageResource(R.mipmap.ic_chat_emo);
        rootLinearLayout.hideAttachedInput(true);
        rootLinearLayout.hideCurrentInput(editText);
    }

    private void notifyTyping(int type) {
        if (conversation.type == Conversation.ConversationType.Single || conversation.type == Conversation.ConversationType.Group) {
            long now = System.currentTimeMillis();
            if (now - lastTypingTime > TYPING_INTERVAL_IN_SECOND * 1000) {
                lastTypingTime = now;
                TypingMessageContent content = new TypingMessageContent(type);
                messageViewModel.sendMessage(conversation, toUsers(), content);
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
                Toast.makeText(activity, fragment.getString(R.string.max_emoji_per_message, MAX_EMOJI_PER_MESSAGE), Toast.LENGTH_SHORT).show();
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
            editText.setSelection(editEnd);
        }
    }

    @Override
    public void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath) {
        String key;
        if (conversation.type == Conversation.ConversationType.SecretChat) {
            key = conversation.target + "_" + stickerBitmapPath;
        } else {
            key = stickerBitmapPath;
        }
        String remoteUrl = sharedPreferences.getString(key, null);
        messageViewModel.sendStickerMsg(conversation, toUsers(), stickerBitmapPath, remoteUrl);
    }

    private List<String> toUsers() {
        if (TextUtils.isEmpty(this.targetUser)) {
            return null;
        }
        return Collections.singletonList(this.targetUser);
    }

    // ==================== 动态表情推荐功能 ====================

    /**
     * 初始化表情推荐视图
     */
    private void initStickerRecommendView() {
        // 创建推荐视图
        mStickerRecommendView = new StickerRecommendView(getContext());

        // 设置回调
        mStickerRecommendView.setOnStickerRecommendListener(stickerItem -> {
            sendRecommendedSticker(stickerItem);
            editText.setText("");
            hideSoftKeyboard();
            hideStickerRecommendPopup();
        });
    }

    /**
     * 显示表情推荐 PopupWindow
     */
    private void showStickerRecommendPopup() {
        if (mStickerRecommendView == null || fragment.getActivity() == null || !fragment.isResumed()) {
            return;
        }

        // 如果已经有 PopupWindow 在显示，先隐藏
        hideStickerRecommendPopup();

        // 计算宽度：每个表情约116dp (100dp + 8dp padding)
        // 最多显示2.5个表情，约248dp
        int stickerCount = mStickerRecommendView.getStickerCount();
        int widthPerSticker = dpToPx(116);  // 每个表情占用的宽度
        int calculatedWidth = stickerCount * widthPerSticker;
        int maxPopupWidth = dpToPx(248);  // 最大宽度
        int popupWidth = Math.min(calculatedWidth, maxPopupWidth);

        // 创建 PopupWindow
        PopupWindow popupWindow = new PopupWindow(
            mStickerRecommendView,  // 内容视图
            popupWidth,  // 动态宽度
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false  // 不获取焦点
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.sticker_recommend_bg));

        // 先测量视图尺寸
        mStickerRecommendView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        mStickerRecommendView.layout(0, 0, mStickerRecommendView.getMeasuredWidth(), mStickerRecommendView.getMeasuredHeight());

        // 设置为可见（在 PopupWindow 显示之前）
        mStickerRecommendView.setVisibility(VISIBLE);

        // 定位到发送按钮上方显示
        if (sendButton != null && sendButton.getVisibility() == View.VISIBLE) {
            // 发送按钮可见时，显示在发送按钮上方
            // yOffset 负值表示向上偏移，计算为：表情高度(136dp) + 按钮高度 + 间距
            //int yOffset = -(dpToPx(136) + sendButton.getHeight() + dpToPx(8));
            int yOffset = -(dpToPx(116) + sendButton.getHeight());
            popupWindow.showAsDropDown(sendButton, 0, yOffset);
        } else if (extImageView != null) {
            // 否则显示在扩展按钮（+号）上方
            int yOffset = -(dpToPx(116) + extImageView.getHeight());
            popupWindow.showAsDropDown(extImageView, 0, yOffset);
        } else {
            return;
        }

        mStickerRecommendPopup = popupWindow;

    }

    /**
     * 隐藏表情推荐 PopupWindow
     */
    private void hideStickerRecommendPopup() {
        if (mStickerRecommendPopup != null && mStickerRecommendPopup.isShowing()) {
            mStickerRecommendPopup.dismiss();
            mStickerRecommendPopup = null;
        }
    }

    private android.widget.PopupWindow mStickerRecommendPopup;

    /**
     * 发送推荐的动态表情
     */
    private void sendRecommendedSticker(StickerItem stickerItem) {
        String stickerPath = StickerManager.getInstance()
            .getStickerBitmapPath(stickerItem.getCategory(), stickerItem.getName());

        String key;
        if (conversation.type == Conversation.ConversationType.SecretChat) {
            key = conversation.target + "_" + stickerPath;
        } else {
            key = stickerPath;
        }

        String remoteUrl = sharedPreferences.getString(key, null);
        messageViewModel.sendStickerMsg(conversation, toUsers(), stickerPath, remoteUrl);
    }

    // ==================== 图片推荐功能 ====================

    /**
     * 初始化图片推荐视图
     */
    private void initImageRecommendView() {
        android.util.Log.d("ConversationInputPanel", "initImageRecommendView: called");
        // 创建推荐视图
        mImageRecommendView = new ImageRecommendView(getContext());

        // 设置回调
        mImageRecommendView.setOnImageRecommendListener(new ImageRecommendView.OnImageRecommendListener() {
            @Override
            public void onImageRecommendSelected(String imagePath) {
                android.util.Log.d("ConversationInputPanel", "ImageRecommend clicked: " + imagePath);
                sendRecommendedImage(imagePath);
                hideImageRecommendPopup();
            }

            @Override
            public void onDismiss() {
                android.util.Log.d("ConversationInputPanel", "ImageRecommend auto-hide triggered");
                hideImageRecommendPopup();
            }
        });
    }

    /**
     * 显示图片推荐 PopupWindow
     *
     * @param imagePath 图片路径
     */
    private void showImageRecommendPopup(String imagePath) {
        android.util.Log.d("ConversationInputPanel", "showImageRecommendPopup: path=" + imagePath + ", view=" + mImageRecommendView + ", activity=" + fragment.getActivity());
        if (mImageRecommendView == null || fragment.getActivity() == null || !fragment.isResumed()) {
            android.util.Log.w("ConversationInputPanel", "showImageRecommendPopup: cannot show, view=" + mImageRecommendView + ", activity=" + fragment.getActivity() + ", resumed=" + fragment.isResumed());
            return;
        }

        // 如果已经有 PopupWindow 在显示，先隐藏
        hideImageRecommendPopup();

        // 设置图片
        mImageRecommendView.showImage(imagePath);

        // 先测量视图尺寸
        mImageRecommendView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        mImageRecommendView.layout(0, 0, mImageRecommendView.getMeasuredWidth(), mImageRecommendView.getMeasuredHeight());

        int popupWidth = mImageRecommendView.getMeasuredWidth();
        int popupHeight = mImageRecommendView.getMeasuredHeight();

        android.util.Log.d("ConversationInputPanel", "popup size: width=" + popupWidth + ", height=" + popupHeight);

        // 创建 PopupWindow
        PopupWindow popupWindow = new PopupWindow(
            mImageRecommendView,
            popupWidth,
            popupHeight,
            false
        );

        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.image_recommend_bg));

        // 设置为可见（在 PopupWindow 显示之前）
        mImageRecommendView.setVisibility(VISIBLE);

        // 使用 showAtLocation 精确定位，处理键盘弹出情况
        if (extImageView != null) {
            int[] location = new int[2];
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            extImageView.getLocationOnScreen(location);

            // 默认位置：按钮上方
            int x = displayMetrics.widthPixels - popupWidth - dpToPx(8);
            int y = location[1] - popupHeight - dpToPx(8);

            // 如果有键盘高度，限制 popup 的最大 Y 坐标，避免显示在右下角
            if (keyboardHeight > 0) {
                int screenHeight = displayMetrics.heightPixels;
                int maxY = screenHeight - keyboardHeight - popupHeight - dpToPx(20);
                y = Math.min(y, maxY);
            }

            // 确保 y 不会超出屏幕顶部
            y = Math.max(y, dpToPx(16));

            android.util.Log.d("ConversationInputPanel", "show image recommend popup at: x=" + x + ", y=" + y + ", keyboardHeight=" + keyboardHeight);
            popupWindow.showAtLocation(extImageView, Gravity.NO_GRAVITY, x, y);
        } else {
            android.util.Log.w("ConversationInputPanel", "extImageView is null, cannot show popup");
            return;
        }

        mImageRecommendPopup = popupWindow;
    }

    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 隐藏图片推荐 PopupWindow
     */
    private void hideImageRecommendPopup() {
        if (mImageRecommendPopup != null && mImageRecommendPopup.isShowing()) {
            mImageRecommendPopup.dismiss();
            mImageRecommendPopup = null;
        }
    }

    /**
     * 发送推荐的图片
     *
     * @param imagePath 图片路径
     */
    private void sendRecommendedImage(String imagePath) {
        java.io.File imageFile = new java.io.File(imagePath);
        if (!imageFile.exists()) {
            return;
        }

        messageViewModel.sendImgMsg(conversation, imageFile);
        hideImageRecommendPopup();
        hideSoftKeyboard();
    }

    /**
     * 隐藏键盘
     */
    private void hideSoftKeyboard() {
        if (activity != null && editText != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            }
        }
    }

    /**
     * dp 转 px
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 检查并显示动态表情推荐
     *
     * @param inputText 用户输入的文本
     */
    private void checkStickerRecommend(String inputText) {
        if (mStickerRecommendView == null) {
            return;
        }

        if (inputText == null || inputText.length() < STICKER_RECOMMEND_MIN_LENGTH) {
            hideStickerRecommendPopup();
            return;
        }

        // 匹配表情
        List<StickerItem> matchedStickers = matchStickers(inputText);

        if (matchedStickers != null && !matchedStickers.isEmpty()) {
            mStickerRecommendView.loadStickersOnly(matchedStickers);
            showStickerRecommendPopup();
        } else {
            hideStickerRecommendPopup();
        }
    }

    /**
     * 匹配动态表情
     *
     * @param keyword 输入的关键词
     * @return 匹配到的表情列表
     */
    private List<StickerItem> matchStickers(String keyword) {
        List<StickerItem> result = new ArrayList<>();

        if (keyword == null || keyword.isEmpty()) {
            android.util.Log.d("StickerRecommend", "matchStickers: keyword is empty");
            return result;
        }

        try {
            // 获取所有表情分类
            List<com.lqr.emoji.StickerCategory> categories = StickerManager.getInstance().getStickerCategories();

            if (categories == null || categories.isEmpty()) {
                android.util.Log.d("StickerRecommend", "matchStickers: no categories found");
                return result;
            }

            android.util.Log.d("StickerRecommend", "matchStickers: checking " + categories.size() + " categories for keyword: " + keyword);

            for (com.lqr.emoji.StickerCategory category : categories) {
                if (category.getStickers() == null) {
                    continue;
                }

                for (StickerItem item : category.getStickers()) {
                    // 匹配文件名（不含扩展名）
                    String fileName = item.getName();
                    if (fileName != null) {
                        // 移除文件扩展名
                        String nameWithoutExt = fileName.contains(".")
                            ? fileName.substring(0, fileName.lastIndexOf('.'))
                            : fileName;

                        // 精确匹配或包含匹配
                        if (nameWithoutExt.equalsIgnoreCase(keyword)
                            || nameWithoutExt.contains(keyword)
//                            || keyword.contains(nameWithoutExt)
                        ) {
                            android.util.Log.d("StickerRecommend", "Matched: " + nameWithoutExt);
                            result.add(item);
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("StickerRecommend", "Error matching stickers", e);
        }

        return result;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hideImageRecommendPopup();
        ImageRecommendManager.getInstance().clearRecommendedImages();
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
