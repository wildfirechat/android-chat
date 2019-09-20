package cn.wildfire.chat.kit.audio;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;

public class AudioRecorder implements AudioManager.OnAudioFocusChangeListener {
    private Context context;
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;
    private AudioFocusRequest audioFocusRequest;

    public AudioRecorder(Context context) {
        this.context = context;
    }

    /**
     * @param outputAudioFile
     */
    public void startRecord(String outputAudioFile) {
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(this, new Handler())
                    .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
        }

        try {
            this.audioManager.setMode(0);
            this.mediaRecorder = new MediaRecorder();

            try {
                int bps = 7950;
                this.mediaRecorder.setAudioSamplingRate(8000);
                this.mediaRecorder.setAudioEncodingBitRate(bps);
            } catch (Resources.NotFoundException var3) {
                var3.printStackTrace();
            }

            this.mediaRecorder.setAudioChannels(1);
            this.mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            this.mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            this.mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            this.mediaRecorder.setOutputFile(outputAudioFile);
            this.mediaRecorder.prepare();
            this.mediaRecorder.start();
        } catch (Exception var4) {
            var4.printStackTrace();
        }
    }

    public void stopRecord() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(this);
            }
            mediaRecorder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            stopRecord();
        }
    }

    public int getMaxAmplitude() {
        try {
            return mediaRecorder.getMaxAmplitude();
        } catch (Exception e) {
            // do nothing
        }
        return 0;
    }

    public interface OnRecordError {
        void onError(String msg);
    }
}
