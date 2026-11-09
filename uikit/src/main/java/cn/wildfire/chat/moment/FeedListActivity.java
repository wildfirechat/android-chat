package cn.wildfire.chat.moment;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 朋友圈主页在手机端的宿主壳，整页逻辑见 {@link FeedListPageFragment}。
 * <p>
 * 平板上这个 Activity 不会被启动：{@code PaneRegistry} 里登记了它 → 直接在右栏造
 * {@link FeedListPageFragment}，见 {@link MomentPaneRegistry}。
 * <p>
 * 入口有隐式（{@code WfcIntent.ACTION_MOMENT}，见 uikit 的 AndroidManifest）和显式两种，
 * 两条路都带可选的 {@code userInfo}：为空看自己的，非空看那个人的。
 */
public class FeedListActivity extends MomentShellActivity {

    @Override
    protected Fragment createPage(Intent intent) {
        return FeedListPageFragment.fromIntent(intent);
    }
}
