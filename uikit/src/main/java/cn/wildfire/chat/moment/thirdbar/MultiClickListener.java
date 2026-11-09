package cn.wildfire.chat.moment.thirdbar;

import android.os.Handler;
import android.view.View;


/**
 * Created by 大灯泡 on 2016/6/21.
 * <p/>
 * 单双击事件监听
 */
public abstract class MultiClickListener implements View.OnClickListener {
    private static final int DelayedTime = 250;
    private boolean isDouble = false;
    private Handler handler = new Handler();


    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            isDouble = false;
            handler.removeCallbacks(this);
            onSingleClick();
        }
    };

    @Override
    public final void onClick(View v) {
        if (isDouble) {
            isDouble = false;
            handler.removeCallbacks(runnable);
            onDoubleClick();
        } else {
            isDouble = true;
            handler.postDelayed(runnable, DelayedTime);
        }
    }

    public abstract void onSingleClick();

    public abstract void onDoubleClick();

}
