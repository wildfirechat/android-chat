package cn.wildfire.chat.moment;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 单条朋友圈详情在手机端的宿主壳，整页逻辑见 {@link FeedDetailPageFragment}。
 * intent 里带 {@code feedId}。
 */
public class FeedDetailActivity extends MomentShellActivity {

    @Override
    protected Fragment createPage(Intent intent) {
        return FeedDetailPageFragment.fromIntent(intent);
    }
}
