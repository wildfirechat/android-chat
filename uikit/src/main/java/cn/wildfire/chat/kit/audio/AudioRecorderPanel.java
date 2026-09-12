/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Layout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.asr.AsrManager;
import cn.wildfire.chat.kit.widget.SimpleTextWatcher;
import cn.wildfirechat.remote.ChatManager;

/**
 * 按住说话，交互参考微信：
 * <ul>
 *     <li>按住按钮开始录音，松开发送语音</li>
 *     <li>手指滑到左上方的“取消”后松开，不发送</li>
 *     <li>开启转文字时，手指滑到右上方的“转文字”，边说边显示识别出的文字；松开后可以编辑文字再发送，也可以发送原语音</li>
 * </ul>
 * 录音采集 16kHz PCM，发送语音时再编码成 AMR；转文字识别的是从开始录音算起的全部音频。
 */
public class AudioRecorderPanel implements View.OnTouchListener {
    private static final String TAG = "AudioRecorderPanel";

    /**
     * 用户取消时 {@link OnRecordListener#onRecordFail(String)} 的 reason
     */
    public static final String REASON_USER_CANCELED = "user canceled";

    private static final int PCM_BYTES_PER_SECOND = 16000 * 2;
    private static final int PCM_FRAME_BYTES = 960;

    private static final int STAGE_IDLE = 0;
    // 按住录音中
    private static final int STAGE_RECORDING = 1;
    // 在“转文字”上松手后，编辑识别出的文字
    private static final int STAGE_EDITING = 2;
    // 浮层正在退出
    private static final int STAGE_DISMISSING = 3;

    // 气泡形态：松开发送、取消、转文字、编辑文字、没有识别到文字、说话时间太短
    private static final int BUBBLE_SEND = 0;
    private static final int BUBBLE_CANCEL = 1;
    private static final int BUBBLE_TEXT = 2;
    private static final int BUBBLE_EDIT = 3;
    private static final int BUBBLE_NO_TEXT = 4;
    private static final int BUBBLE_TOO_SHORT = 5;

    private static final int BUBBLE_COLOR_RED = 0xFFFA5151;

    private int maxDuration = Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * 1000;
    private int minDuration = 1 * 1000;
    private int countDown = 10 * 1000;

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Vibrator vibrator;
    private final SoundPool soundPool;
    private int sendAudioMessageSuccessSoundId;
    private View rootView;
    private Button button;
    private OnRecordListener recordListener;
    private boolean speechToTextEnabled;
    // 气泡使用 app 主色调，气泡上的文字和声波根据主色调的深浅用白色或深色
    private final int bubbleColor;
    private final int bubbleContentColor;

    private int stage = STAGE_IDLE;
    // 每次录音加 1，用来丢弃上一次录音延迟到达的回调
    private int recordSession;
    private PcmAudioRecorder recorder;
    private ByteArrayOutputStream pcmBuffer;
    private byte[] recordedPcm;
    private long startTime;
    private long recordDuration;

    private AsrManager asrManager;
    private boolean asrStarted;
    private boolean asrFinished;
    private boolean asrFailed;

    // 浮层
    private PopupWindow popupWindow;
    private FrameLayout popupRootLayout;
    private VoiceInputBackground background;
    private float backgroundProgress;
    private ValueAnimator backgroundAnimator;
    private ValueAnimator panelAnimator;
    private VoiceRecordBottomView bottomView;
    private TextView countDownTextView;
    private VoiceBubbleLayout bubbleLayout;
    private VoiceWaveView waveView;
    private EditText textEditText;
    private TextView hintTextView;
    private ViewGroup editActionsLayout;
    private View sendVoiceLayout;
    private View sendVoiceImageView;
    private TextView sendTextButton;
    private final int[] popupLocation = new int[2];
    // 浮层完成布局、各控件的位置已经计算好
    private boolean layoutReady;
    private float stageLeft;
    private float stageWidth;
    private float bubbleBottomMargin;
    private float editActionsBottomMargin;
    private int bubbleState = BUBBLE_SEND;
    private final BubbleFrame bubbleFrame = new BubbleFrame();
    private ValueAnimator bubbleAnimator;
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();
    private final Runnable dismissRunnable = this::dismissPopup;

    public AudioRecorderPanel(Context context) {
        this.context = context;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        this.bubbleColor = ContextCompat.getColor(context, R.color.colorPrimary);
        this.bubbleContentColor = ColorUtils.calculateLuminance(bubbleColor) > 0.3 ? 0xFF191919 : Color.WHITE;
    }

    /**
     * @param maxDuration 最长录音时间，单位：秒
     */
    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration * 1000;
    }

    /**
     * @param minDuration 最短录音时间，单位：秒
     */
    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration * 1000;
    }

    /**
     * @param countDown 录音剩余多少秒时开始倒计时，单位：秒
     */
    public void setCountDown(int countDown) {
        this.countDown = countDown * 1000;
    }

    /**
     * 是否可以滑动到“转文字”。需要配置实时语音识别服务 {@link Config#ASR_STREAM_SERVER_URL}，转文字的结果通过
     * {@link OnRecordListener#onSendText(String)} 回调
     */
    public void setSpeechToTextEnabled(boolean enabled) {
        this.speechToTextEnabled = enabled;
    }

    /**
     * 将{@link AudioRecorderPanel}附加到button上面
     *
     * @param rootView 录音界面显示的rootView
     * @param button   长按触发录音的按钮
     */
    public void attach(View rootView, Button button) {
        this.rootView = rootView;
        this.button = button;
        this.button.setText(R.string.hold_to_talk);
        this.button.setOnTouchListener(this);

        this.sendAudioMessageSuccessSoundId = this.soundPool.load(context, R.raw.audido_msg_send_success, 0);
    }

    public void deattch() {
        dismissNow();
        rootView = null;
        button = null;
        this.soundPool.unload(sendAudioMessageSuccessSoundId);
    }

    public void setRecordListener(OnRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    public boolean isShowingRecorder() {
        return popupWindow != null && popupWindow.isShowing();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (button == null) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (stage == STAGE_DISMISSING) {
                    // 上一次录音的浮层还在退出，直接关闭
                    dismissNow();
                }
                if (stage == STAGE_IDLE) {
                    setButtonPressed(true);
                    startRecord();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (stage == STAGE_RECORDING) {
                    updateZone(event.getRawX(), event.getRawY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setButtonPressed(false);
                if (stage == STAGE_RECORDING) {
                    stopRecord(bottomView.getZone());
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void setButtonPressed(boolean pressed) {
        button.setBackgroundResource(pressed ? R.drawable.shape_session_btn_voice_pressed : R.drawable.shape_session_btn_voice_normal);
        float scale = pressed ? 0.95f : 1f;
        button.animate().scaleX(scale).scaleY(scale).setDuration(100).start();
    }

    private void startRecord() {
        int session = ++recordSession;
        PcmAudioRecorder audioRecorder = new PcmAudioRecorder(context);
        boolean success = audioRecorder.startRecording(new PcmAudioRecorder.OnAudioDataCallback() {
            @Override
            public void onAudioData(byte[] pcmData) {
                // 在录音线程回调，pcmData 会被复用
                byte[] data = pcmData.clone();
                float level = computeLevel(data);
                handler.post(() -> handleAudioData(session, data, level));
            }

            @Override
            public void onError(String message) {
                handler.post(() -> handleRecorderError(session, message));
            }
        });
        if (!success) {
            // 启动失败时已经回调过 onError，这次录音作废
            recordSession++;
            if (recordListener != null) {
                recordListener.onRecordFail(context.getString(R.string.audio_record_init_error));
            }
            return;
        }

        recorder = audioRecorder;
        pcmBuffer = new ByteArrayOutputStream(PCM_BYTES_PER_SECOND * 5);
        recordedPcm = null;
        startTime = SystemClock.elapsedRealtime();
        stage = STAGE_RECORDING;
        asrStarted = false;
        asrFinished = false;
        asrFailed = false;
        showPopup();
        if (recordListener != null) {
            recordListener.onRecordStateChanged(RecordState.START);
        }
        handler.postDelayed(tickRunnable, 100);
        vibrate(40);
    }

    private void handleAudioData(int session, @NonNull byte[] data, float level) {
        if (session != recordSession || pcmBuffer == null) {
            return;
        }
        pcmBuffer.write(data, 0, data.length);
        if (stage == STAGE_RECORDING) {
            waveView.setLevel(level);
        }
        if (asrManager != null) {
            asrManager.feedAudioData(data);
        }
    }

    private void handleRecorderError(int session, @NonNull String message) {
        if (session != recordSession || stage != STAGE_RECORDING) {
            return;
        }
        Log.e(TAG, "录音失败: " + message);
        if (pcmBuffer != null && pcmBuffer.size() > 0) {
            // 例如来电抢走了音频焦点，按手指当前的位置结束录音，保留已经录到的声音
            stopRecord(bottomView.getZone());
        } else {
            if (recordListener != null) {
                recordListener.onRecordFail(message);
            }
            dismissPopup();
        }
    }

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            if (stage != STAGE_RECORDING) {
                return;
            }
            long elapsed = SystemClock.elapsedRealtime() - startTime;
            if (elapsed >= maxDuration) {
                stopRecord(bottomView.getZone());
                return;
            }
            if (elapsed > maxDuration - countDown) {
                showCountDown((int) Math.ceil((maxDuration - elapsed) / 1000.0));
            }
            handler.postDelayed(this, 100);
        }
    };

    /**
     * 结束录音
     *
     * @param zone 松手时手指所在的目标
     */
    private void stopRecord(int zone) {
        if (stage != STAGE_RECORDING) {
            return;
        }
        handler.removeCallbacks(tickRunnable);
        recordDuration = SystemClock.elapsedRealtime() - startTime;
        stage = zone == VoiceRecordBottomView.ZONE_TEXT ? STAGE_EDITING : STAGE_DISMISSING;
        if (recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }
        // 录音线程停止前投递的音频还在消息队列中，处理完之后再继续
        int session = recordSession;
        handler.post(() -> {
            if (session == recordSession) {
                onRecordStopped(zone);
            }
        });
    }

    private void onRecordStopped(int zone) {
        recordedPcm = pcmBuffer != null ? pcmBuffer.toByteArray() : new byte[0];
        pcmBuffer = null;
        if (zone == VoiceRecordBottomView.ZONE_TEXT) {
            enterEditing();
            return;
        }
        cancelSpeechToText();
        if (zone == VoiceRecordBottomView.ZONE_CANCEL) {
            if (recordListener != null) {
                recordListener.onRecordFail(REASON_USER_CANCELED);
            }
            dismissPopup();
        } else if (recordDuration < minDuration) {
            showTooShortTip();
        } else {
            sendVoice();
            dismissPopup();
        }
    }

    /**
     * 把录到的音频编码成 AMR 后回调 onRecordSuccess
     */
    private void sendVoice() {
        byte[] pcm = recordedPcm;
        OnRecordListener listener = recordListener;
        recordedPcm = null;
        if (pcm == null || pcm.length == 0 || listener == null) {
            return;
        }
        playSendSound();
        int duration = Math.max(1, Math.round((float) pcm.length / PCM_BYTES_PER_SECOND));
        int gain = Config.ENABLE_AUDIO_MESSAGE_AMPLIFICATION ? Math.max(1, Config.AUDIO_MESSAGE_AMPLIFICATION_FACTOR) : 1;
        String audioFile = genAudioFile();
        ChatManager.Instance().getWorkHandler().post(() -> {
            boolean success = PcmAmrEncoder.encode(pcm, pcm.length, audioFile, gain);
            handler.post(() -> {
                if (success) {
                    listener.onRecordSuccess(audioFile, duration);
                } else {
                    listener.onRecordFail(context.getString(R.string.audio_record_stop_error));
                }
            });
        });
    }

    private void startSpeechToText() {
        if (asrStarted) {
            return;
        }
        asrStarted = true;
        AsrManager manager = new AsrManager(context);
        asrManager = manager;
        manager.startRecognitionWithAudioFeed(new AsrManager.RecognitionCallback() {
            @Override
            public void onPartialResult(@NonNull String text) {
                if (asrManager == manager) {
                    onSpeechText(text, false);
                }
            }

            @Override
            public void onFinalResult(@NonNull String text) {
                if (asrManager == manager) {
                    onSpeechText(text, true);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (asrManager == manager) {
                    onSpeechError(message);
                }
            }

            @Override
            public void onHotwordDetected(@NonNull String hotword, @NonNull String text) {
                if (asrManager == manager) {
                    onSpeechText(text, true);
                }
            }
        });
        // 转文字从开始录音时算起，先补上已经录到的音频
        if (asrManager == manager && pcmBuffer != null) {
            byte[] recorded = pcmBuffer.toByteArray();
            for (int offset = 0; offset < recorded.length; offset += PCM_FRAME_BYTES) {
                manager.feedAudioData(Arrays.copyOfRange(recorded, offset, Math.min(recorded.length, offset + PCM_FRAME_BYTES)));
            }
        }
    }

    private void onSpeechText(@NonNull String text, boolean isFinal) {
        if (isFinal) {
            asrFinished = true;
            asrManager = null;
        }
        setBubbleText(text);
        if (isFinal && stage == STAGE_EDITING) {
            onRecognitionDoneInEditing();
        }
    }

    private void onSpeechError(@NonNull String message) {
        Log.w(TAG, "转文字失败: " + message);
        // 已经识别出的文字保留，仍然可以编辑后发送
        asrFailed = textEditText == null || textEditText.length() == 0;
        asrFinished = true;
        asrManager = null;
        updateTextHint();
        if (stage == STAGE_EDITING) {
            onRecognitionDoneInEditing();
        }
    }

    private void cancelSpeechToText() {
        if (asrManager != null) {
            AsrManager manager = asrManager;
            asrManager = null;
            manager.cancelRecognition();
        }
    }

    /**
     * 停止录音和识别，丢弃录到的音频
     */
    private void releaseRecording() {
        stage = STAGE_IDLE;
        recordSession++;
        handler.removeCallbacks(tickRunnable);
        handler.removeCallbacks(dismissRunnable);
        if (recorder != null) {
            recorder.stopRecording();
            recorder = null;
        }
        cancelSpeechToText();
        pcmBuffer = null;
        recordedPcm = null;
    }

    private void vibrate(long milliseconds) {
        if (vibrator != null && vibrator.hasVibrator() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void playSendSound() {
        soundPool.play(sendAudioMessageSuccessSoundId, 0.1f, 0.1f, 0, 0, 1);
    }

    private String genAudioFile() {
        File dir = new File(context.getFilesDir(), "audio");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir, System.currentTimeMillis() + "");
        return file.getAbsolutePath();
    }

    /**
     * 计算一段 PCM 的音量，0~1
     */
    private static float computeLevel(@NonNull byte[] pcm) {
        int samples = pcm.length / 2;
        if (samples == 0) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < samples; i++) {
            short sample = (short) ((pcm[2 * i] & 0xff) | (pcm[2 * i + 1] << 8));
            sum += sample * sample;
        }
        double db = 20 * Math.log10(Math.max(Math.sqrt(sum / samples), 1) / 32768);
        if (Config.ENABLE_AUDIO_MESSAGE_AMPLIFICATION) {
            db += 20 * Math.log10(Math.max(1, Config.AUDIO_MESSAGE_AMPLIFICATION_FACTOR));
        }
        // -50dB 以下视为安静，-15dB 以上视为最大音量
        return (float) Math.max(0, Math.min(1, (db + 50) / 35));
    }

    public interface OnRecordListener {
        void onRecordSuccess(String audioFile, int duration);

        void onRecordFail(String reason);

        void onRecordStateChanged(RecordState state);

        /**
         * 语音转成文字后，用户确认发送文字
         *
         * @param text 要发送的文字
         */
        default void onSendText(String text) {
        }
    }

    public enum RecordState {
        // 开始录音
        START,
        // 录音中
        RECORDING,
        // 用户准备取消
        TO_CANCEL,
        // 最长录音时间快到
        TO_TIMEOUT,
    }

    // region 浮层界面

    private ObjectAnimator shakeAnimator;

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            popupRootLayout.getViewTreeObserver().removeOnPreDrawListener(this);
            if (stage == STAGE_IDLE || rootView == null || button == null) {
                return true;
            }
            onPopupLaidOut();
            // 控件位置变了，跳过这一帧，下一帧按新位置绘制
            return false;
        }
    };

    private void showPopup() {
        if (popupWindow == null) {
            createPopup();
        }
        resetPopupViews();
        layoutReady = false;
        popupWindow.setFocusable(false);
        popupWindow.showAtLocation(rootView, Gravity.TOP | Gravity.START, 0, 0);
        // 浮层完成布局、拿到尺寸后再计算各控件的位置，在这之前所有控件都是透明的
        popupRootLayout.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    }

    private void createPopup() {
        popupRootLayout = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.voice_input_popup, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 浮层本身按深色设计，不让系统的强制深色模式反转浮层的颜色
            popupRootLayout.setForceDarkAllowed(false);
        }
        background = new VoiceInputBackground(dp(130));
        popupRootLayout.setBackground(background);
        bottomView = popupRootLayout.findViewById(R.id.voiceRecordBottomView);
        countDownTextView = popupRootLayout.findViewById(R.id.voiceCountDownTextView);
        bubbleLayout = popupRootLayout.findViewById(R.id.voiceBubbleLayout);
        waveView = popupRootLayout.findViewById(R.id.voiceWaveView);
        textEditText = popupRootLayout.findViewById(R.id.voiceTextEditText);
        hintTextView = popupRootLayout.findViewById(R.id.voiceHintTextView);
        editActionsLayout = popupRootLayout.findViewById(R.id.voiceEditActionsLayout);
        sendVoiceLayout = popupRootLayout.findViewById(R.id.voiceSendVoiceLayout);
        sendVoiceImageView = popupRootLayout.findViewById(R.id.voiceSendVoiceImageView);
        sendTextButton = popupRootLayout.findViewById(R.id.voiceSendTextButton);
        textEditText.setTextColor(bubbleContentColor);
        textEditText.setHintTextColor(ColorUtils.setAlphaComponent(bubbleContentColor, 0x99));
        textEditText.setHighlightColor(ColorUtils.setAlphaComponent(bubbleContentColor, 0x4D));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            GradientDrawable cursor = new GradientDrawable();
            cursor.setColor(bubbleContentColor);
            cursor.setSize(dp(2), 0);
            textEditText.setTextCursorDrawable(cursor);
        }

        popupRootLayout.findViewById(R.id.voiceCancelImageView).setOnClickListener(v -> onEditCancelClick());
        sendVoiceImageView.setOnClickListener(v -> onSendVoiceClick());
        sendTextButton.setOnClickListener(v -> onSendTextClick());
        textEditText.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (stage == STAGE_EDITING && asrFinished && bubbleState == BUBBLE_EDIT) {
                    updateEditActions();
                    animateBubbleTo(BUBBLE_EDIT);
                }
            }
        });
        // 编辑文字时弹出软键盘，把气泡和按钮顶到软键盘上方
        ViewCompat.setOnApplyWindowInsetsListener(popupRootLayout, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int padding = stage == STAGE_EDITING && imeBottom > 0 ? Math.max(0, imeBottom - Math.round(editActionsBottomMargin) + dp(12)) : 0;
            if (v.getPaddingBottom() != padding) {
                v.setPadding(0, 0, 0, padding);
                if (stage == STAGE_EDITING && layoutReady) {
                    // 深灰背景跟着气泡移动
                    if (panelAnimator != null) {
                        panelAnimator.cancel();
                    }
                    background.setPanelTop(getEditPanelTop());
                }
            }
            return insets;
        });

        popupWindow = new PopupWindow(popupRootLayout, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(false);
        // 让浮层延伸到状态栏
        popupWindow.setClippingEnabled(false);
        popupWindow.setAnimationStyle(0);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        popupWindow.setOnDismissListener(this::onPopupDismissed);
    }

    private void resetPopupViews() {
        cancelPopupAnimations();
        backgroundProgress = 0;
        background.setProgress(0);
        popupRootLayout.setPadding(0, 0, 0, 0);
        bottomView.reset();
        bottomView.setSpeechToTextEnabled(speechToTextEnabled);
        countDownTextView.setVisibility(View.INVISIBLE);
        bubbleLayout.setAlpha(0);
        bubbleLayout.setScaleX(1);
        bubbleLayout.setScaleY(1);
        bubbleLayout.setTranslationX(0);
        bubbleLayout.setTranslationY(0);
        hintTextView.setAlpha(0);
        waveView.setLoading(false);
        waveView.setLevel(0);
        textEditText.setFocusable(false);
        textEditText.setFocusableInTouchMode(false);
        textEditText.setText("");
        textEditText.setHint(null);
        editActionsLayout.setAlpha(1);
        editActionsLayout.setVisibility(View.INVISIBLE);
        bubbleState = BUBBLE_SEND;
    }

    private void cancelPopupAnimations() {
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        if (panelAnimator != null) {
            panelAnimator.cancel();
        }
        if (bubbleAnimator != null) {
            bubbleAnimator.cancel();
        }
        if (shakeAnimator != null) {
            shakeAnimator.cancel();
        }
        bubbleLayout.animate().cancel();
        countDownTextView.animate().cancel();
        editActionsLayout.animate().cancel();
        for (int i = 0; i < editActionsLayout.getChildCount(); i++) {
            editActionsLayout.getChildAt(i).animate().cancel();
        }
    }

    private void onPopupLaidOut() {
        int width = popupRootLayout.getWidth();
        int height = popupRootLayout.getHeight();
        popupRootLayout.getLocationOnScreen(popupLocation);
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        // 操作区只覆盖会话界面，双栏时不会延伸到左边
        stageLeft = Math.max(0, location[0] - popupLocation[0]);
        stageWidth = Math.min(width - stageLeft, rootView.getWidth());
        if (stageWidth <= 0) {
            stageLeft = 0;
            stageWidth = width;
        }
        // 底部弧形区域要盖住按住说话的按钮，手指按下时就在“松开 发送”区域内
        button.getLocationOnScreen(location);
        float buttonTop = location[1] - popupLocation[1];
        float arcTop = Math.min(height - dp(110), buttonTop - dp(16));
        bottomView.setStage(stageLeft, stageLeft + stageWidth, arcTop);
        // 录音时深灰背景从弧形按钮处开始，编辑文字时再升到气泡下方
        background.setStage(stageLeft, stageLeft + stageWidth);
        background.setPanelTop(bottomView.getPillTop() + dp(18));

        // 气泡的尖角固定在按钮上方，内容变多时向上长高
        bubbleBottomMargin = height - Math.max(dp(160), bottomView.getPillTop() - dp(141));
        editActionsBottomMargin = Math.max(dp(16), height - arcTop - dp(20));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) countDownTextView.getLayoutParams();
        lp.leftMargin = Math.round(stageLeft);
        lp.width = Math.round(stageWidth);
        lp.bottomMargin = Math.round(bubbleBottomMargin - dp(44));
        countDownTextView.setLayoutParams(lp);

        lp = (FrameLayout.LayoutParams) editActionsLayout.getLayoutParams();
        lp.leftMargin = Math.round(stageLeft);
        lp.width = Math.round(stageWidth);
        lp.bottomMargin = Math.round(editActionsBottomMargin);
        editActionsLayout.setLayoutParams(lp);

        layoutReady = true;
        BubbleFrame frame = new BubbleFrame();
        computeBubbleFrame(bubbleState, frame);
        applyBubbleFrame(frame, frame, 1);
        playEnterAnimation();
    }

    private void playEnterAnimation() {
        animateBackground(1, 200, null);
        bottomView.show();
        bubbleLayout.setAlpha(0);
        bubbleLayout.setScaleX(0.6f);
        bubbleLayout.setScaleY(0.6f);
        bubbleLayout.setTranslationY(dp(24));
        bubbleLayout.animate()
            .alpha(1)
            .scaleX(1)
            .scaleY(1)
            .translationY(0)
            .setStartDelay(40)
            .setDuration(340)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();
    }

    private void updateZone(float rawX, float rawY) {
        if (!layoutReady) {
            return;
        }
        int zone = bottomView.zoneAt(rawX - popupLocation[0], rawY - popupLocation[1]);
        if (zone == bottomView.getZone()) {
            return;
        }
        bottomView.setZone(zone);
        bottomView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (zone == VoiceRecordBottomView.ZONE_CANCEL) {
            if (recordListener != null) {
                recordListener.onRecordStateChanged(RecordState.TO_CANCEL);
            }
            animateBubbleTo(BUBBLE_CANCEL);
        } else if (zone == VoiceRecordBottomView.ZONE_TEXT) {
            startSpeechToText();
            animateBubbleTo(BUBBLE_TEXT);
        } else {
            animateBubbleTo(BUBBLE_SEND);
        }
    }

    /**
     * 气泡从当前的位置、大小、颜色平滑过渡到目标形态；内容变化时用同一个形态再调用一次，气泡会平滑地改变高度
     */
    private void animateBubbleTo(int state) {
        boolean changed = bubbleState != state;
        bubbleState = state;
        if (!layoutReady) {
            return;
        }
        BubbleFrame from = bubbleFrame.copy();
        BubbleFrame to = new BubbleFrame();
        computeBubbleFrame(state, to);
        if (bubbleAnimator != null) {
            bubbleAnimator.cancel();
        }
        bubbleAnimator = ValueAnimator.ofFloat(0, 1);
        bubbleAnimator.setDuration(changed ? 320 : 180);
        bubbleAnimator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        bubbleAnimator.addUpdateListener(animation -> applyBubbleFrame(from, to, (float) animation.getAnimatedValue()));
        bubbleAnimator.start();
    }

    private void computeBubbleFrame(int state, @NonNull BubbleFrame frame) {
        float tailHeight = bubbleLayout.getTailHeight();
        float maxWidth = stageWidth - dp(32);
        float sendWidth = Math.min(maxWidth, Math.max(dp(160), stageWidth * 0.475f));
        float tailTargetX;
        frame.waveColor = bubbleContentColor;
        frame.textAlpha = 0;
        frame.hintAlpha = 0;
        frame.textBottomMargin = dp(20);
        switch (state) {
            case BUBBLE_CANCEL:
                // 缩成红色的小方块，移到“取消”上方
                frame.width = dp(78);
                frame.height = dp(78) + tailHeight;
                tailTargetX = bottomView.getCancelCenterX();
                frame.left = Math.max(stageLeft + dp(16), tailTargetX - frame.width / 2);
                frame.color = BUBBLE_COLOR_RED;
                frame.waveColor = Color.WHITE;
                frame.waveWidth = dp(34);
                frame.waveHeight = dp(16);
                frame.waveCenterX = frame.width / 2;
                frame.waveCenterY = (frame.height - tailHeight) / 2;
                frame.waveAlpha = 1;
                break;
            case BUBBLE_TEXT:
            case BUBBLE_EDIT:
            case BUBBLE_NO_TEXT:
                // 展开成整行宽度显示文字，声波缩小到右下角，识别完成后消失；没有识别到文字时变成红色的提示
                boolean noText = state == BUBBLE_NO_TEXT;
                boolean showWave = state == BUBBLE_TEXT || (state == BUBBLE_EDIT && !asrFinished);
                frame.left = stageLeft + dp(16);
                frame.width = maxWidth;
                frame.textBottomMargin = showWave ? dp(20) : 0;
                frame.height = measureBubbleHeight(frame.width, frame.textBottomMargin);
                frame.color = noText ? BUBBLE_COLOR_RED : bubbleColor;
                frame.waveColor = noText ? Color.WHITE : bubbleContentColor;
                frame.waveWidth = dp(34);
                frame.waveHeight = dp(16);
                frame.waveCenterX = frame.width - dp(20) - frame.waveWidth / 2;
                frame.waveCenterY = frame.height - tailHeight - dp(18);
                frame.waveAlpha = showWave ? 1 : 0;
                frame.textAlpha = noText ? 0 : 1;
                frame.hintAlpha = noText ? 1 : 0;
                // 和微信一样，整行宽度的气泡尖角固定在同一个位置，转文字、编辑、没有识别到文字之间切换时不动
                tailTargetX = stageLeft + stageWidth * 0.755f;
                break;
            case BUBBLE_TOO_SHORT:
                // 保持松开发送时的样子，声波换成提示，提示放不下时加宽
                frame.width = Math.min(maxWidth, Math.max(sendWidth, measureHintWidth(maxWidth)));
                frame.height = dp(78) + tailHeight;
                frame.left = stageLeft + (stageWidth - frame.width) / 2;
                frame.color = bubbleColor;
                frame.waveWidth = sendWidth * 0.46f;
                frame.waveHeight = dp(20);
                frame.waveCenterX = frame.width / 2;
                frame.waveCenterY = (frame.height - tailHeight) / 2;
                frame.waveAlpha = 0;
                frame.hintAlpha = 1;
                tailTargetX = frame.left + frame.width / 2;
                break;
            default:
                frame.width = sendWidth;
                frame.height = dp(78) + tailHeight;
                frame.left = stageLeft + (stageWidth - frame.width) / 2;
                frame.color = bubbleColor;
                frame.waveWidth = frame.width * 0.46f;
                frame.waveHeight = dp(20);
                frame.waveCenterX = frame.width / 2;
                frame.waveCenterY = (frame.height - tailHeight) / 2;
                frame.waveAlpha = 1;
                tailTargetX = frame.left + frame.width / 2;
                break;
        }
        frame.tailX = tailTargetX - frame.left;
    }

    private void applyBubbleFrame(@NonNull BubbleFrame from, @NonNull BubbleFrame to, float fraction) {
        BubbleFrame frame = bubbleFrame;
        frame.left = lerp(from.left, to.left, fraction);
        frame.width = lerp(from.width, to.width, fraction);
        frame.height = lerp(from.height, to.height, fraction);
        frame.tailX = lerp(from.tailX, to.tailX, fraction);
        frame.color = (int) argbEvaluator.evaluate(fraction, from.color, to.color);
        frame.waveColor = (int) argbEvaluator.evaluate(fraction, from.waveColor, to.waveColor);
        frame.waveWidth = lerp(from.waveWidth, to.waveWidth, fraction);
        frame.waveHeight = lerp(from.waveHeight, to.waveHeight, fraction);
        frame.waveCenterX = lerp(from.waveCenterX, to.waveCenterX, fraction);
        frame.waveCenterY = lerp(from.waveCenterY, to.waveCenterY, fraction);
        frame.waveAlpha = lerp(from.waveAlpha, to.waveAlpha, fraction);
        frame.textAlpha = lerp(from.textAlpha, to.textAlpha, fraction);
        frame.hintAlpha = lerp(from.hintAlpha, to.hintAlpha, fraction);
        frame.textBottomMargin = lerp(from.textBottomMargin, to.textBottomMargin, fraction);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) bubbleLayout.getLayoutParams();
        lp.leftMargin = Math.round(frame.left);
        lp.width = Math.round(frame.width);
        lp.height = Math.round(frame.height);
        lp.bottomMargin = Math.round(bubbleBottomMargin);
        bubbleLayout.setLayoutParams(lp);
        bubbleLayout.setPivotX(frame.width / 2);
        bubbleLayout.setPivotY(frame.height);
        bubbleLayout.setBubbleColor(frame.color);
        bubbleLayout.setTailX(frame.tailX);

        FrameLayout.LayoutParams waveLp = (FrameLayout.LayoutParams) waveView.getLayoutParams();
        waveLp.width = Math.round(frame.waveWidth);
        waveLp.height = Math.round(frame.waveHeight);
        waveView.setLayoutParams(waveLp);
        waveView.setTranslationX(frame.waveCenterX - frame.waveWidth / 2 - bubbleLayout.getPaddingLeft());
        waveView.setTranslationY(frame.waveCenterY - frame.waveHeight / 2 - bubbleLayout.getPaddingTop());
        waveView.setBarColor(frame.waveColor);
        waveView.setAlpha(frame.waveAlpha);

        FrameLayout.LayoutParams textLp = (FrameLayout.LayoutParams) textEditText.getLayoutParams();
        textLp.bottomMargin = Math.round(frame.textBottomMargin);
        textEditText.setLayoutParams(textLp);
        textEditText.setAlpha(frame.textAlpha);
        // 提示淡入时轻微上浮
        hintTextView.setAlpha(frame.hintAlpha);
        hintTextView.setTranslationY((1 - frame.hintAlpha) * dp(6));
    }

    /**
     * 气泡按指定宽度显示当前文字时的高度
     */
    private float measureBubbleHeight(float width, float textBottomMargin) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) textEditText.getLayoutParams();
        int bottomMargin = lp.bottomMargin;
        lp.bottomMargin = Math.round(textBottomMargin);
        bubbleLayout.measure(View.MeasureSpec.makeMeasureSpec(Math.round(width), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        lp.bottomMargin = bottomMargin;
        return bubbleLayout.getMeasuredHeight();
    }

    /**
     * 气泡完整显示提示所需的宽度
     */
    private float measureHintWidth(float maxWidth) {
        int horizontalPadding = bubbleLayout.getPaddingLeft() + bubbleLayout.getPaddingRight();
        hintTextView.measure(View.MeasureSpec.makeMeasureSpec(Math.max(0, Math.round(maxWidth) - horizontalPadding), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return hintTextView.getMeasuredWidth() + horizontalPadding;
    }

    private void setBubbleText(@NonNull String text) {
        if (textEditText == null) {
            return;
        }
        if (!TextUtils.equals(textEditText.getText(), text)) {
            textEditText.setText(text);
            textEditText.setSelection(text.length());
            if (bubbleState == BUBBLE_TEXT || bubbleState == BUBBLE_EDIT) {
                animateBubbleTo(bubbleState);
                textEditText.post(this::scrollTextToEnd);
            }
        }
        updateTextHint();
    }

    /**
     * 文字超过最大行数时，滚动到最新识别出的文字
     */
    private void scrollTextToEnd() {
        Layout layout = textEditText.getLayout();
        if (layout == null) {
            return;
        }
        int visibleHeight = textEditText.getHeight() - textEditText.getTotalPaddingTop() - textEditText.getTotalPaddingBottom();
        textEditText.scrollTo(0, Math.max(0, layout.getHeight() - visibleHeight));
    }

    private void updateTextHint() {
        if (textEditText == null) {
            return;
        }
        // 按住转文字时出错，在气泡里提示；松手后没有文字时换成红色的提示气泡
        if (asrFailed && stage == STAGE_RECORDING) {
            textEditText.setHint(R.string.voice_input_recognize_failed);
        } else {
            textEditText.setHint(null);
        }
    }

    private void showTooShortTip() {
        showHint(R.string.voice_short, bubbleContentColor);
        animateBubbleTo(BUBBLE_TOO_SHORT);
        shakeAnimator = ObjectAnimator.ofFloat(bubbleLayout, View.TRANSLATION_X, 0, -dp(10), dp(10), -dp(7), dp(7), -dp(3), dp(3), 0);
        shakeAnimator.setDuration(420);
        shakeAnimator.setStartDelay(100);
        shakeAnimator.start();
        handler.postDelayed(dismissRunnable, 1000);
    }

    /**
     * 设置气泡里的提示，图标和文字同色
     */
    private void showHint(int resId, int color) {
        hintTextView.setText(resId);
        hintTextView.setTextColor(color);
        TextViewCompat.setCompoundDrawableTintList(hintTextView, ColorStateList.valueOf(color));
    }

    private void showCountDown(int seconds) {
        countDownTextView.setText(context.getString(R.string.voice_input_count_down, seconds));
        if (countDownTextView.getVisibility() != View.VISIBLE) {
            countDownTextView.setAlpha(0);
            countDownTextView.setVisibility(View.VISIBLE);
            countDownTextView.animate().alpha(1).setDuration(200).start();
            if (recordListener != null) {
                recordListener.onRecordStateChanged(RecordState.TO_TIMEOUT);
            }
        }
    }

    /**
     * 在“转文字”上松手：底部按钮落下，换成取消、发送原语音、发送；识别结果全部返回后可以编辑文字
     */
    private void enterEditing() {
        countDownTextView.animate().alpha(0).setDuration(150).start();
        bottomView.hide(null);
        animatePanelTop(getEditPanelTop(), 360);
        // 浮层获取焦点后才能编辑文字
        popupWindow.setFocusable(true);
        popupWindow.update();
        updateTextHint();
        updateEditActions();
        showEditActions();
        if (asrManager != null) {
            // 录音已经停止，声波换成等待动画，剩余识别结果返回后回调 onFinalResult
            waveView.setLoading(true);
            animateBubbleTo(BUBBLE_EDIT);
            asrManager.stopRecognition();
        } else {
            // 识别已经结束，或者出错了
            asrFinished = true;
            onRecognitionDoneInEditing();
        }
    }

    private void onRecognitionDoneInEditing() {
        waveView.setLoading(false);
        updateTextHint();
        updateEditActions();
        if (textEditText.length() == 0) {
            // 没有识别到文字：红色提示，只能取消或者发送原语音
            showHint(asrFailed ? R.string.voice_input_recognize_failed : R.string.voice_input_no_text, Color.WHITE);
            animateBubbleTo(BUBBLE_NO_TEXT);
            return;
        }
        textEditText.setFocusableInTouchMode(true);
        textEditText.requestFocus();
        textEditText.setSelection(textEditText.length());
        animateBubbleTo(BUBBLE_EDIT);
    }

    private boolean isVoiceAvailable() {
        return recordedPcm != null && recordedPcm.length > 0 && recordDuration >= minDuration;
    }

    private void updateEditActions() {
        sendTextButton.setEnabled(asrFinished && textEditText.getText().toString().trim().length() > 0);
        sendVoiceImageView.setEnabled(isVoiceAvailable());
    }

    private void showEditActions() {
        editActionsLayout.setAlpha(1);
        editActionsLayout.setVisibility(View.VISIBLE);
        for (int i = 0; i < editActionsLayout.getChildCount(); i++) {
            View child = editActionsLayout.getChildAt(i);
            float alpha = child == sendVoiceLayout && !isVoiceAvailable() ? 0.4f : 1;
            child.setAlpha(0);
            child.setTranslationY(dp(28));
            // 等弧形按钮大部分落下后再升起，两组按钮不在同一时间重叠
            child.animate()
                .alpha(alpha)
                .translationY(0)
                .setStartDelay(140 + i * 40L)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator(2f))
                .start();
        }
    }

    private void onEditCancelClick() {
        if (stage != STAGE_EDITING) {
            return;
        }
        cancelSpeechToText();
        if (recordListener != null) {
            recordListener.onRecordFail(REASON_USER_CANCELED);
        }
        dismissPopup();
    }

    private void onSendVoiceClick() {
        if (stage != STAGE_EDITING || !isVoiceAvailable()) {
            return;
        }
        cancelSpeechToText();
        sendVoice();
        dismissPopup();
    }

    private void onSendTextClick() {
        if (stage != STAGE_EDITING) {
            return;
        }
        String text = textEditText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        if (recordListener != null) {
            recordListener.onSendText(text);
        }
        dismissPopup();
    }

    /**
     * 播放退出动画后关闭浮层
     */
    private void dismissPopup() {
        handler.removeCallbacks(dismissRunnable);
        if (popupWindow == null || !popupWindow.isShowing()) {
            releaseRecording();
            return;
        }
        stage = STAGE_DISMISSING;
        hideKeyboard();
        if (bubbleAnimator != null) {
            bubbleAnimator.cancel();
        }
        bottomView.hide(null);
        bubbleLayout.animate()
            .alpha(0)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .translationY(0)
            .setStartDelay(0)
            .setDuration(180)
            .setInterpolator(new AccelerateInterpolator())
            .start();
        countDownTextView.animate().alpha(0).setDuration(150).start();
        editActionsLayout.animate().alpha(0).setDuration(150).start();
        animateBackground(0, 240, this::dismissNow);
    }

    /**
     * 立即关闭浮层，停止录音和识别，不回调
     */
    private void dismissNow() {
        handler.removeCallbacks(dismissRunnable);
        if (popupRootLayout != null) {
            popupRootLayout.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
            cancelPopupAnimations();
        }
        // 先置为空闲，关闭浮层时不再当作用户取消
        releaseRecording();
        layoutReady = false;
        if (popupWindow != null && popupWindow.isShowing()) {
            hideKeyboard();
            popupWindow.dismiss();
        }
    }

    private void onPopupDismissed() {
        if (stage == STAGE_IDLE) {
            return;
        }
        // 编辑文字时按返回键关闭浮层，等同于取消
        if (stage == STAGE_EDITING && recordListener != null) {
            recordListener.onRecordFail(REASON_USER_CANCELED);
        }
        popupRootLayout.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        cancelPopupAnimations();
        releaseRecording();
        layoutReady = false;
    }

    private void hideKeyboard() {
        if (textEditText == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(textEditText.getWindowToken(), 0);
        }
    }

    private void animateBackground(float progress, long duration, @Nullable Runnable endAction) {
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        backgroundAnimator = ValueAnimator.ofFloat(backgroundProgress, progress);
        backgroundAnimator.setDuration(duration);
        backgroundAnimator.addUpdateListener(animation -> {
            backgroundProgress = (float) animation.getAnimatedValue();
            background.setProgress(backgroundProgress);
        });
        backgroundAnimator.addListener(new EndListener(endAction));
        backgroundAnimator.start();
    }

    /**
     * 深灰背景的上边缘平滑移动到指定位置
     */
    private void animatePanelTop(float panelTop, long duration) {
        if (panelAnimator != null) {
            panelAnimator.cancel();
        }
        panelAnimator = ValueAnimator.ofFloat(background.getPanelTop(), panelTop);
        panelAnimator.setDuration(duration);
        panelAnimator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        panelAnimator.addUpdateListener(animation -> background.setPanelTop((float) animation.getAnimatedValue()));
        panelAnimator.start();
    }

    /**
     * 编辑文字时深灰背景完全不透明处，在气泡下边缘稍上方
     */
    private float getEditPanelTop() {
        return popupRootLayout.getHeight() - popupRootLayout.getPaddingBottom() - bubbleBottomMargin - bubbleLayout.getTailHeight() - dp(5);
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static float lerp(float from, float to, float fraction) {
        return from + (to - from) * fraction;
    }

    /**
     * 气泡某一形态的位置、大小和内容，坐标相对于浮层，声波和尖角的坐标相对于气泡
     */
    private static class BubbleFrame {
        float left;
        float width;
        float height;
        float tailX;
        int color;
        int waveColor;
        float waveWidth;
        float waveHeight;
        float waveCenterX;
        float waveCenterY;
        float waveAlpha;
        float textAlpha;
        float hintAlpha;
        float textBottomMargin;

        @NonNull
        BubbleFrame copy() {
            BubbleFrame frame = new BubbleFrame();
            frame.left = left;
            frame.width = width;
            frame.height = height;
            frame.tailX = tailX;
            frame.color = color;
            frame.waveColor = waveColor;
            frame.waveWidth = waveWidth;
            frame.waveHeight = waveHeight;
            frame.waveCenterX = waveCenterX;
            frame.waveCenterY = waveCenterY;
            frame.waveAlpha = waveAlpha;
            frame.textAlpha = textAlpha;
            frame.hintAlpha = hintAlpha;
            frame.textBottomMargin = textBottomMargin;
            return frame;
        }
    }

    /**
     * 动画正常结束（没有被取消）时执行
     */
    private static class EndListener extends AnimatorListenerAdapter {
        private final Runnable endAction;
        private boolean canceled;

        EndListener(@Nullable Runnable endAction) {
            this.endAction = endAction;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            canceled = true;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!canceled && endAction != null) {
                endAction.run();
            }
        }
    }

    // endregion
}
