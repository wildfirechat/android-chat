/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.io.File;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;

public class AudioRecorderPanel implements View.OnTouchListener {
    private int maxDuration = Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * 1000;
    private int minDuration = 1 * 1000;
    private int countDown = 10 * 1000;
    private boolean isRecording;
    private long startTime;
    private boolean isToCancel;
    private boolean isCountDown;
    private String currentAudioFile;

    private Context context;
    private View rootView;
    private Button button;
    private AudioRecorder recorder;
    private OnRecordListener recordListener;
    private Handler handler;

    private TextView countDownTextView;
    private TextView stateTextView;
    private ImageView stateImageView;
    private PopupWindow recordingWindow;

    public AudioRecorderPanel(Context context) {
        this.context = context;
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
     * 将{@link AudioRecorderPanel}附加到button上面
     *
     * @param rootView 录音界面显示的rootView
     * @param button   长按触发录音的按钮
     */
    public void attach(View rootView, Button button) {
        this.rootView = rootView;
        this.button = button;
        this.button.setText("按住 说话");
        this.button.setOnTouchListener(this);
    }

    public void deattch() {
        rootView = null;
        button = null;
    }

    public void setRecordListener(OnRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (button == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_pressed);
                startRecord();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isCancelled(v, event)) {
                    if (recordListener != null) {
                        recordListener.onRecordStateChanged(RecordState.TO_CANCEL);
                    }
                    showCancelTip();
                    isToCancel = true;
                } else {
                    hideCancelTip();
                    isToCancel = false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                button.setBackgroundResource(R.drawable.shape_session_btn_voice_normal);
                if (isToCancel) {
                    cancelRecord();
                } else if (isRecording) {
                    stopRecord();
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void startRecord() {
        // FIXME: 2018/10/10 权限是否有权限，没权限提示错误，并返回
        isRecording = true;
        if (recorder == null) {
            recorder = new AudioRecorder(context);
            handler = new Handler();
        } else {
            handler.removeCallbacks(this::hideRecording);
        }
        currentAudioFile = genAudioFile();
        recorder.startRecord(currentAudioFile);
        if (recordListener != null) {
            recordListener.onRecordStateChanged(RecordState.START);
        }

        startTime = System.currentTimeMillis();
        showRecording();
        tick();
    }

    public boolean isShowingRecorder() {
        return recordingWindow != null;
    }

    private void stopRecord() {
        if (!isRecording) {
            return;
        }
        if (recorder != null) {
            recorder.stopRecord();
        }
        if (recordListener != null) {
            long duration = System.currentTimeMillis() - startTime;
            if (duration > minDuration) {
                recordListener.onRecordSuccess(currentAudioFile, (int) duration / 1000);
                hideRecording();
            } else {
                showTooShortTip();
                handler.postDelayed(this::hideRecording, 1000);
            }
        } else {
            hideRecording();
        }

        isToCancel = false;
        isRecording = false;

        recorder = null;
        handler = null;
    }

    private void cancelRecord() {
        if (recorder != null) {
            recorder.stopRecord();
        }
        if (recordListener != null) {
            recordListener.onRecordFail("user canceled");
        }
        hideRecording();

        isToCancel = false;
        isRecording = false;
    }

    private void showRecording() {
        if (recordingWindow == null) {
            View view = View.inflate(context, R.layout.audio_popup_wi_vo, null);
            stateImageView = view.findViewById(R.id.rc_audio_state_image);
            stateTextView = view.findViewById(R.id.rc_audio_state_text);
            countDownTextView = view.findViewById(R.id.rc_audio_timer);
            recordingWindow = new PopupWindow(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            recordingWindow.setFocusable(false);
            recordingWindow.setOutsideTouchable(false);
            recordingWindow.setTouchable(true);
        }

        recordingWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0);

        if (isCountDown) {
            countDownTextView.setVisibility(View.VISIBLE);
            stateImageView.setVisibility(View.GONE);
        } else {
            stateImageView.setVisibility(View.VISIBLE);
            stateImageView.setImageResource(R.mipmap.ic_volume_1);
            countDownTextView.setVisibility(View.GONE);
        }
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText(R.string.voice_rec);
        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
    }

    private void hideRecording() {
        if (recordingWindow == null) {
            return;
        }
        recordingWindow.dismiss();
        recordingWindow = null;
        stateImageView = null;
        stateTextView = null;
        countDownTextView = null;
        isCountDown = false;
        isToCancel = false;
    }

    private void showTooShortTip() {
        stateImageView.setImageResource(R.mipmap.ic_volume_wraning);
        stateTextView.setText(R.string.voice_short);
    }

    private void showCancelTip() {
        if (recordingWindow == null) {
            return;
        }
        countDownTextView.setVisibility(View.GONE);
        stateImageView.setVisibility(View.VISIBLE);
        stateImageView.setImageResource(R.mipmap.ic_volume_cancel);
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText(R.string.voice_cancel);
        stateTextView.setBackgroundResource(R.drawable.corner_voice_style);
    }

    private void hideCancelTip() {
        if (!isToCancel) {
            return;
        }
        showRecording();
    }

    /**
     * @param seconds
     */
    private void showCountDown(int seconds) {
        if (isToCancel) {
            return;
        }
        stateImageView.setVisibility(View.GONE);
        stateTextView.setVisibility(View.VISIBLE);
        stateTextView.setText(R.string.voice_rec);
        stateTextView.setBackgroundResource(R.drawable.bg_voice_popup);
        countDownTextView.setText(String.format("%s", seconds));
        countDownTextView.setVisibility(View.VISIBLE);
    }

    private void timeout() {
        stopRecord();
    }

    private void tick() {
        if (isRecording) {
            long now = System.currentTimeMillis();
            if (now - startTime > maxDuration) {
                timeout();
                return;
            } else if (now - startTime > (maxDuration - countDown)) {
                isCountDown = true;
                int tmp = (int) ((maxDuration - (now - startTime)) / 1000);
                tmp = tmp > 1 ? tmp : 1;
                showCountDown(tmp);
                if (recordListener != null) {
                    recordListener.onRecordStateChanged(RecordState.TO_TIMEOUT);
                }
            } else {
                updateVolume();
            }
            handler.postDelayed(this::tick, 100);
        }
    }

    private void updateVolume() {
        if (isToCancel || recorder == null) {
            return;
        }
        // refer to https://www.cnblogs.com/lqminn/archive/2012/10/10/2717904.html
        int db = 8 * recorder.getMaxAmplitude() / 32768;
        switch (db) {
            case 0:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_1);
                break;
            case 1:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_2);
                break;
            case 2:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_3);
                break;
            case 3:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_4);
                break;
            case 4:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_5);
                break;
            case 5:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_6);
                break;
            case 6:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_7);
                break;
            default:
                this.stateImageView.setImageResource(R.mipmap.ic_volume_8);
        }

    }

    private String genAudioFile() {
        File dir = new File(context.getFilesDir(), "audio");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File(dir, System.currentTimeMillis() + "");
        return file.getAbsolutePath();
    }

    private boolean isCancelled(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        if (event.getRawX() < location[0] || event.getRawX() > location[0] + view.getWidth()
            || event.getRawY() < location[1] - 40) {
            return true;
        }

        return false;
    }

    public interface OnRecordListener {
        void onRecordSuccess(String audioFile, int duration);

        void onRecordFail(String reason);

        void onRecordStateChanged(RecordState state);
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
}
