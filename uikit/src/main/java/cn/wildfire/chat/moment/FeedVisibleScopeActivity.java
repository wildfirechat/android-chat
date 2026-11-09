package cn.wildfire.chat.moment;

import android.content.Intent;

import androidx.fragment.app.Fragment;

/**
 * 「谁可以看」在手机端的宿主壳，整页逻辑见 {@link FeedVisibleScopePageFragment}。
 * <p>
 * 参数与结果的键名留在这里：它是这一页对外的契约，发表页两端都按这些常量读写。
 */
public class FeedVisibleScopeActivity extends MomentShellActivity {

    public static final String PARAM_VISIBLE_MODE = "mode";
    public static final String PARAM_UIDS = "users";

    public static final String RESULT_VISIBLE_MODE = "mode";
    public static final String RESULT_USERS = "users";

    public static final int VISIBLE_MODE_ALL = 0;
    public static final int VISIBLE_MODE_PRIVATE = 1;
    public static final int VISIBLE_MODE_PART = 2;
    public static final int VISIBLE_MODE_BLOCK = 3;

    @Override
    protected Fragment createPage(Intent intent) {
        return FeedVisibleScopePageFragment.fromIntent(intent);
    }
}
