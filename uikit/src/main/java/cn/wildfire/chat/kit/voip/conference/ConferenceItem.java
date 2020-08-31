package cn.wildfire.chat.kit.voip.conference;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import cn.wildfire.chat.kit.R;


public class ConferenceItem extends FrameLayout {
    private ImageView portraitImageView;
    private TextView statusTextView;

    public ConferenceItem(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public ConferenceItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ConferenceItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ConferenceItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //noinspection SuspiciousNameCombination
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    private void init(Context context, AttributeSet attrs) {
        View view = inflate(context, R.layout.av_multi_call_item, this);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        statusTextView = view.findViewById(R.id.statusTextView);
    }

    public ImageView getPortraitImageView() {
        return portraitImageView;
    }

    public TextView getStatusTextView() {
        return statusTextView;
    }

    // TODO video
}
