package cn.wildfire.chat.kit.live;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * A VideoView that fills its parent using "center-crop" scaling:
 * the video is scaled so that both dimensions are >= the parent size,
 * preserving the native aspect ratio. Any overflow is clipped by the parent.
 *
 * Call {@link #setVideoSize(int, int)} from the MediaPlayer.OnPreparedListener
 * so that onMeasure can use the real aspect ratio.
 */
public class FullScreenVideoView extends VideoView {

    private int mVideoWidth;
    private int mVideoHeight;

    public FullScreenVideoView(Context context) {
        super(context);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FullScreenVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Call this once the MediaPlayer reports its video dimensions. */
    public void setVideoSize(int width, int height) {
        if (mVideoWidth != width || mVideoHeight != height) {
            mVideoWidth = width;
            mVideoHeight = height;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentW = MeasureSpec.getSize(widthMeasureSpec);
        int parentH = MeasureSpec.getSize(heightMeasureSpec);

        if (mVideoWidth > 0 && mVideoHeight > 0) {
            // Center-crop: scale until BOTH axes fill the parent.
            float scaleX = (float) parentW / mVideoWidth;
            float scaleY = (float) parentH / mVideoHeight;
            float scale = Math.max(scaleX, scaleY);   // "cover"

            int measuredW = Math.round(mVideoWidth  * scale);
            int measuredH = Math.round(mVideoHeight * scale);
            setMeasuredDimension(measuredW, measuredH);
        } else {
            // Dimensions not known yet — just fill the parent.
            setMeasuredDimension(parentW, parentH);
        }
    }
}

