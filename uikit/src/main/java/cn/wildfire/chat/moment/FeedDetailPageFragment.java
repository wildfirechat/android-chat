package cn.wildfire.chat.moment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.Collections;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.moment.third.beans.FriendCircleBean;
import cn.wildfire.chat.moment.third.widgets.TitleBar;
import cn.wildfirechat.moment.MomentClient;
import cn.wildfirechat.moment.model.Feed;

/**
 * 单条朋友圈详情。入口是朋友圈消息列表里的一条。
 * <p>
 * 逐行搬自 {@link FeedDetailActivity}，那个类现在只是手机端的壳。
 * <p>
 * <strong>顺带修掉一处重复初始化</strong>：改造前 {@code FeedDetailActivity.onCreate} 先
 * {@code super.onCreate(...)}（基类里已经调过一次 {@code initView()}）、紧接着又自己调了一次
 * {@code initView()}，于是 RecyclerView 被挂了两个滚动监听、adapter 建了两遍、
 * {@code getFeed} 也请求了两次。现在初始化只发生在 {@code onViewCreated} 一处。
 */
public class FeedDetailPageFragment extends BaseFeedPageFragment {

    private static final String ARG_FEED_ID = "feedId";

    private long feedId;

    public static FeedDetailPageFragment fromIntent(Intent intent) {
        FeedDetailPageFragment fragment = new FeedDetailPageFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_FEED_ID, intent.getLongExtra(ARG_FEED_ID, 0));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        feedId = args == null ? 0 : args.getLong(ARG_FEED_ID, 0);
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.moment_activity_detail;
    }

    @Override
    protected void initView(View rootView) {
        super.initView(rootView);
        if (feedId == 0) {
            WfcPageCompat.finishPage(this);
            return;
        }

        // TODO show loading
        MomentClient.getInstance().getFeed(feedId, new MomentClient.GetFeedCallback() {
            @Override
            public void onSuccess(Feed feed) {
                if (getView() == null) {
                    return;
                }
                // TODO dismiss loading

                mFriendCircleAdapter.showBottomPadding();
                mFriendCircleAdapter.setFriendCircleBeans(
                    Collections.singletonList(FriendCircleBean.fromFeed(requireContext(), feed)));
            }

            @Override
            public void onFailure(int errorCode) {
                if (getView() == null) {
                    return;
                }
                // TODO dismiss loading
                Toast.makeText(requireContext(), "" + errorCode, Toast.LENGTH_SHORT).show();
                WfcPageCompat.finishPage(FeedDetailPageFragment.this);
            }
        });
    }

    @Override
    protected void initTitleBar() {
        getTitleBar().setLeftText("详情");
        setLeftTextColor(Color.parseColor("#040404"));
        setTitleMode(TitleBar.TitleBarMode.MODE_LEFT);
        setTitleLeftIcon(R.drawable.back_left_black);
    }
}
