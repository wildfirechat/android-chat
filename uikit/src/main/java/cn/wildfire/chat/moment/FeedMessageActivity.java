package cn.wildfire.chat.moment;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 朋友圈消息列表在手机端的宿主壳，整页逻辑见 {@link FeedMessagePageFragment}。无参数。
 */
public class FeedMessageActivity extends MomentShellActivity {

    @Override
    protected Fragment createPage(Intent intent) {
        return FeedMessagePageFragment.fromIntent(intent);
    }
}
