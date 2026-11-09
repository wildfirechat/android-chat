package cn.wildfire.chat.moment;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 发表朋友圈在手机端的宿主壳，整页逻辑见 {@link PublishFeedPageFragment}。
 * <p>
 * 结果契约：发表成功回 {@code RESULT_OK}（无数据），朋友圈主页据此刷新。
 */
public class PublishFeedActivity extends MomentShellActivity {

    public static final String VIDEO_URL = "video_url";
    public static final String IMAGE_URLS = "image_urls";
    public static final String IMAGE_COMPRESS = "image_compress";

    @Override
    protected Fragment createPage(Intent intent) {
        return PublishFeedPageFragment.fromIntent(intent);
    }
}
