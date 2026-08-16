/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfirechat.message.Message;

/**
 * 投票详情页的外壳，页面本体见 {@link PollDetailPageFragment}。
 * <p>
 * 手机端保留「从消息进入显示 X、从列表进入显示返回箭头」的差异：
 * 右栏里宿主是 {@code PanePageFragment}，统一显示返回箭头。
 */
public class PollDetailActivity extends WfcBaseActivity {

    // Intent 参数
    public static final String EXTRA_POLL_ID = "pollId";
    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_MESSAGE = "message";

    /**
     * 从消息进入（投票场景）
     */
    public static Intent buildIntent(Context context, Message message, long pollId, String groupId) {
        Intent intent = new Intent(context, PollDetailActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_POLL_ID, pollId);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }

    /**
     * 从列表进入（可能是管理场景）
     */
    public static Intent buildIntent(Context context, long pollId, String groupId) {
        Intent intent = new Intent(context, PollDetailActivity.class);
        intent.putExtra(EXTRA_POLL_ID, pollId);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        // 配置变化后 FragmentManager 已经把页面恢复出来了，无条件 add 会再叠一层
        if (getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout) != null) {
            return;
        }
        Fragment fragment = PollDetailPageFragment.fromIntent(getIntent());
        if (fragment == null) {
            finish();
            return;
        }
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.containerFrameLayout, fragment)
            .commit();

        // 手机端专属：从消息点击进入显示「关闭」图标，从列表进入显示返回箭头。
        // 右栏由 PanePageFragment 提供统一返回箭头，不在这里处理。
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (getIntent().getParcelableExtra(EXTRA_MESSAGE) != null) {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
            } else {
                getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
            }
        }
    }
}
