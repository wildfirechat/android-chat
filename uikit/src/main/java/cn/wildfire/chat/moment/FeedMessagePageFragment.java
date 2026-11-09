package cn.wildfire.chat.moment;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.moment.MomentClient;
import cn.wildfirechat.remote.ChatManager;

/**
 * 朋友圈消息（谁赞了你、谁评论了你）。入口是朋友圈主页头图上那条「您有 N 条未读消息」。
 * <p>
 * 逐行搬自 {@link FeedMessageActivity}，那个类现在只是手机端的壳。
 */
public class FeedMessagePageFragment extends MomentPageFragment
    implements OnFeedUserClickListener,
    FeedMessagAdapter.OnFeedMessageLongClickListener,
    FeedMessagAdapter.OnFeedMessageClickListener,
    FeedMessagAdapter.OnLoadMoreMessageClickListener {

    private FeedMessagAdapter feedMessagAdapter;

    public static FeedMessagePageFragment fromIntent(Intent intent) {
        return new FeedMessagePageFragment();
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.moment_activity_message;
    }

    @Override
    protected void initView(View view) {
        setTitleLeftText("朋友圈消息");
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        feedMessagAdapter = new FeedMessagAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(feedMessagAdapter);
        feedMessagAdapter.setOnFeedMessageClickListener(this);
        feedMessagAdapter.setOnFeedMessageLongClickListener(this);
        feedMessagAdapter.setOnFeedUserClickListener(this);
        feedMessagAdapter.setOnLoadMoreMessageClickListener(this);

        List<Message> messages = MomentClient.getInstance().getFeedMessages(0, true);

        feedMessagAdapter.setFeedMessages(messages);
        feedMessagAdapter.notifyDataSetChanged();
        MomentClient.getInstance().clearUnreadStatus();
    }

    @Override
    public void onTitleRightClick() {
        // TODO
    }

    @Override
    public void onFeedMessageClick(long feedId) {
        openFeedDetail(feedId);
    }

    @Override
    public void onFeedCommentMessageClick(long feedId, long commentId) {
        openFeedDetail(feedId);
    }

    private void openFeedDetail(long feedId) {
        Intent intent = new Intent(requireContext(), FeedDetailActivity.class);
        intent.putExtra("feedId", feedId);
        WfcPageCompat.startPage(this, intent);
    }

    @Override
    public void onFeedMessageLongClick(long feedId) {
        // TODO show feed detail
    }

    @Override
    public void onFeedCommentMessageLongClick(long feedId, long commentId) {

    }

    @Override
    public void onFeedUserClick(String userId) {
        Intent intent = new Intent(requireContext(), FeedListActivity.class);
        intent.putExtra(BaseFeedPageFragment.ARG_USER_INFO, ChatManager.Instance().getUserInfo(userId, false));
        WfcPageCompat.startPage(this, intent);
    }

    @Override
    public void onLoadMoreFeedMessageClick() {
        List<Message> feedMessages = feedMessagAdapter.getFeedMessages();
        long msgId = 0;
        int insertPosition = 0;
        if (feedMessages != null && !feedMessages.isEmpty()) {
            msgId = feedMessages.get(feedMessages.size() - 1).messageId;
            insertPosition = feedMessages.size();
        }
        feedMessages = MomentClient.getInstance().getFeedMessages(msgId, false);
        if (feedMessages.size() == 0) {
            Toast.makeText(requireContext(), "没有更多消息", Toast.LENGTH_SHORT).show();
            return;
        }
        feedMessagAdapter.addFeedMessages(feedMessages);
        feedMessagAdapter.notifyItemRangeInserted(insertPosition, feedMessages.size());
    }
}
