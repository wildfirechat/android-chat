package cn.wildfire.chat.moment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.moment.third.Constants;
import cn.wildfire.chat.moment.third.adapters.FriendCircleAdapter;
import cn.wildfire.chat.moment.third.beans.CommentBean;
import cn.wildfire.chat.moment.third.beans.FriendCircleBean;
import cn.wildfire.chat.moment.third.beans.PraiseBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnDeleteFeedClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnPraiseOrCommentClickListener;
import cn.wildfire.chat.moment.third.others.FriendsCircleAdapterDivideLine;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.moment.third.widgets.CommentOrPraisePopupWindow;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.FeedCommentType;
import cn.wildfirechat.moment.MomentClient;
import cn.wildfirechat.moment.model.Feed;
import cn.wildfirechat.remote.ChatManager;

/**
 * 「一列 feed + 点赞评论」这套交互的基类：朋友圈主页（{@link FeedListPageFragment}）和
 * 单条详情（{@link FeedDetailPageFragment}）共用。
 * <p>
 * 逐行搬自 {@code BaseFeedActivity}，那个类随本次改造删掉。除了宿主换成 Fragment 带来的
 * 机械替换（{@code getSupportFragmentManager} → {@code getChildFragmentManager}、
 * {@code this} 当 Context → {@code requireContext()}、{@code isFinishing()} →
 * {@code getView() == null}），只有两处是真的改了行为，各自在方法上说明：
 * 打开别人的朋友圈走 {@link WfcPageCompat}，返回键从 {@code onBackPressed} 变成
 * {@link #onPageBackPressed()}。
 */
public abstract class BaseFeedPageFragment extends MomentPageFragment implements
    OnPraiseOrCommentClickListener,
    OnCommentItemClickListener,
    OnCommentUserClickListener,
    OnFeedUserClickListener,
    OnCommentItemLongClickListener,
    OnFeedItemLongClickListener, OnDeleteFeedClickListener {

    /**
     * 「看谁的朋友圈」。null 表示自己，与改造前 {@code getIntent().getParcelableExtra("userInfo")}
     * 取不到时的语义一致。
     */
    static final String ARG_USER_INFO = "userInfo";

    protected FriendCircleAdapter mFriendCircleAdapter;
    protected RecyclerView recyclerView;
    protected LinearLayoutManager layoutManager;

    protected CommentFragment commentFragment;

    private CommentOrPraisePopupWindow mCommentOrPraisePopupWindow;

    protected UserInfo user = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            user = args.getParcelable(ARG_USER_INFO);
        }
    }

    @Override
    protected void initView(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                hideCommentFragment();
            }
        });

        layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new FriendsCircleAdapterDivideLine());

        mFriendCircleAdapter = new FriendCircleAdapter(requireContext(), recyclerView);
        mFriendCircleAdapter.setOnCommentItemClickListener(this);
        mFriendCircleAdapter.setOnCommentItemLongClickListener(this);
        mFriendCircleAdapter.setOnFeedItemLongClickListener(this);
        mFriendCircleAdapter.setOnCommentUserClickListener(this);
        mFriendCircleAdapter.setOnDeleteFeedClickListener(this);
        mFriendCircleAdapter.setOnFeedUserClickListener(this);
        mFriendCircleAdapter.setOnPraiseOrCommentClickListener(this::toggleCommentOrPraisePopupWindow);
        recyclerView.setAdapter(mFriendCircleAdapter);

        initTitleBar();
    }

    protected void initTitleBar() {
    }

    protected List<FriendCircleBean> feedsToFriendCircleBeans(List<Feed> feeds) {
        List<FriendCircleBean> friendCircleBeans = new ArrayList<>();
        if (feeds != null) {
            for (Feed feed : feeds) {
                friendCircleBeans.add(FriendCircleBean.fromFeed(requireContext(), feed));
            }
        }
        return friendCircleBeans;
    }

    protected FriendCircleBean feedToFriendCircleBean(Feed feed) {
        return FriendCircleBean.fromFeed(requireContext(), feed);
    }

    @Override
    public void onTitleRightClick() {
    }

    @Override
    public void onPraiseClick(int position) {
        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(position);
        List<PraiseBean> praiseBeans = friendCircleBean.getPraiseBeans();
        PraiseBean praiseBean = new PraiseBean();
        UserInfo userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
        praiseBean.setPraiseUserId(userInfo.uid);
        praiseBean.setPraiseUserName(userInfo.displayName);
        if (praiseBeans == null) {
            List<PraiseBean> beans = new ArrayList<>();
            MomentClient.getInstance().postComment(FeedCommentType.Comment_Thumbup_Type, friendCircleBean.getId(), null, null, 0, null, new MomentClient.PostCallback() {
                @Override
                public void onSuccess(long id, long timestamp) {
                    praiseBean.setId(id);
                    beans.add(praiseBean);
                    friendCircleBean.setPraiseBeans(beans);
                    notifyFeedChanged(position, "star");
                }

                @Override
                public void onFailure(int errorCode) {
                    // do nothing
                }
            });
        } else {
            if (praiseBeans.contains(praiseBean)) {
                int i = praiseBeans.indexOf(praiseBean);
                MomentClient.getInstance().deleteComment(user != null ? user.uid : null, friendCircleBean.getId(), praiseBeans.get(i).getId(), new MomentClient.GeneralCallback() {
                    @Override
                    public void onSuccess() {
                        praiseBeans.remove(praiseBean);
                        friendCircleBean.setPraiseBeans(praiseBeans);
                        notifyFeedChanged(position, "star");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        // do nothing
                    }
                });
            } else {
                MomentClient.getInstance().postComment(FeedCommentType.Comment_Thumbup_Type, friendCircleBean.getId(), null, null, 0, null, new MomentClient.PostCallback() {
                    @Override
                    public void onSuccess(long id, long timestamp) {
                        praiseBean.setId(id);
                        praiseBeans.add(praiseBean);
                        friendCircleBean.setPraiseBeans(praiseBeans);
                        notifyFeedChanged(position, "star");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        // do nothing
                    }
                });
            }
        }
    }

    /**
     * 点赞/评论的网络回调都是异步的，回来时本页可能已经出栈（右栏里宿主 Activity 还活着，
     * 不会像改造前那样连 adapter 一起没掉）。改造前这几处没有 isFinishing 判断，
     * 是因为 Activity 销毁后 adapter 也就无人引用了。
     */
    private void notifyFeedChanged(int position, String payload) {
        if (getView() == null || mFriendCircleAdapter == null) {
            return;
        }
        mFriendCircleAdapter.notifyItemChanged(position + mFriendCircleAdapter.headerCount(),
            Collections.singleton(payload));
    }

    @Override
    public void onCommentClick(View view, int position) {
        View itemView = layoutManager.findViewByPosition(position + mFriendCircleAdapter.headerCount());

        int scrollBy = getCommentPanelTop() - itemView.getBottom();
        recyclerView.scrollBy(0, -scrollBy);

        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(position);

        showCommentFragment(friendCircleBean.getId(), 0);
    }

    @Override
    public void onCommentItemClick(View commentItemView, int feedPosition, int commentPosition) {
        if (commentFragment != null) {
            hideCommentFragment();
            return;
        }

        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(feedPosition);
        List<CommentBean> commentBeans = friendCircleBean.getCommentBeans();
        CommentBean commentBean = commentBeans.get(commentPosition);

        if (commentBean.getChildUserId().equals(ChatManager.Instance().getUserId())) {
            PopupMenu popup = new PopupMenu(requireContext(), commentItemView);
            popup.getMenuInflater().inflate(R.menu.moment_comment_item_delete_popup_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.delete) {
                    deleteComment(feedPosition, commentPosition);
                }

                return true;
            });
            popup.show();
        } else {
            int[] location = new int[2];
            commentItemView.getLocationInWindow(location);
            int bottom = location[1] + commentItemView.getHeight();

            int scrollBy = getCommentPanelTop() - bottom;
            recyclerView.scrollBy(0, -scrollBy);
            showCommentFragment(friendCircleBean.getId(), commentBean.getId());
        }
    }

    @Override
    public void onCommentItemLongClick(View commentItemView, int feedPosition, int commentPosition) {
        int menuId = R.menu.moment_comment_item_popup_menu;

        CommentBean commentBean = mFriendCircleAdapter.getFriendCircleBeans().get(feedPosition).getCommentBeans().get(commentPosition);
        if (commentBean.getChildUserId().equals(ChatManager.Instance().getUserId())) {
            //me
            menuId = R.menu.moment_comment_item_delete_popup_menu;
        }
        PopupMenu popup = new PopupMenu(requireContext(), commentItemView);
        popup.getMenuInflater().inflate(menuId, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.copy) {
                Toast.makeText(requireContext(), "copy", Toast.LENGTH_SHORT).show();
            } else if (itemId == R.id.collection) {
            } else if (itemId == R.id.delete) {
                deleteComment(feedPosition, commentPosition);
            }
            return true;
        });
        popup.show(); //showing popup menu
    }

    @Override
    public void onDeleteFeedClick(int feedPosition) {
        new MaterialDialog.Builder(requireContext())
            .content("删除该朋友圈")
            .positiveText("删除")
            .positiveColor(Color.RED)
            .negativeText("取消")
            .onPositive((dialog, which) -> deleteFeed(feedPosition))
            .build()
            .show();
    }

    @Override
    public void onFeedItemLongClick(View feedItemView, int feedPosition) {
        int menuId = R.menu.moment_feed_item_popup_menu;
        PopupMenu popup = new PopupMenu(requireContext(), feedItemView);
        popup.getMenuInflater().inflate(menuId, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.copy) {
                copy(feedPosition);
            } else if (itemId == R.id.collection) {
            } else if (itemId == R.id.delete) {
                deleteFeed(feedPosition);
            }
            return true;
        });
        popup.show(); //showing popup menu
    }

    private void copy(int feedPosition) {
        ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }

        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(feedPosition);

        String content = friendCircleBean.getContent();
        if (!TextUtils.isEmpty(content)) {
            ClipData clipData = ClipData.newPlainText("feedContent", content);
            clipboardManager.setPrimaryClip(clipData);
        }
    }

    /**
     * 评论输入条挂在本页布局里的 {@code commentLayoutContainer} 上，所以用
     * {@code getChildFragmentManager()} —— 改造前是 Activity 的 FragmentManager。
     * 右栏里同一时刻可能有好几页朋友圈（自己的 → 别人的），各自的输入条必须归各自管。
     */
    private void showCommentFragment(long feedId, long commentId) {
        if (commentFragment != null) {
            return;
        }
        commentFragment = new CommentFragment();
        Bundle bundle = new Bundle();
        bundle.putLong("feedId", feedId);
        bundle.putLong("commentId", commentId);
        commentFragment.setArguments(bundle);
        getChildFragmentManager().beginTransaction().
            replace(R.id.commentLayoutContainer, commentFragment, "comment")
            .show(commentFragment)
            .commit();
    }

    private void hideCommentFragment() {
        if (commentFragment == null) {
            return;
        }

        commentFragment.hideKeyboard();
        getChildFragmentManager().beginTransaction().remove(commentFragment).commit();
        commentFragment = null;
    }

    /**
     * 返回键：先收起评论输入条。改造前是覆写 {@code Activity.onBackPressed()}，
     * 现在通过 {@code WfcPage} 表达，手机端的壳和右栏都会先问本页一次。
     */
    @Override
    public boolean onPageBackPressed() {
        if (commentFragment != null) {
            hideCommentFragment();
            return true;
        }
        return false;
    }

    public void commentFeed(long feedId, String comment) {
        int position = mFriendCircleAdapter.getFeedPosition(feedId);
        MomentClient.getInstance().postComment(FeedCommentType.Comment_Comment_Type, feedId, comment, null, 0, null, new MomentClient.PostCallback() {
            @Override
            public void onSuccess(long id, long timestamp) {
                if (getView() == null) {
                    return;
                }
                FriendCircleBean friendCircleBean = mFriendCircleAdapter.getFriendCircleBean(feedId);
                CommentBean commentBean = new CommentBean();
                commentBean.setCommentContent(comment);
                UserInfo userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
                commentBean.setChildUserId(userInfo.uid);
                commentBean.setChildUserName(userInfo.displayName);
                commentBean.setCommentType(Constants.CommentType.COMMENT_TYPE_SINGLE);
                List<CommentBean> commentBeans = friendCircleBean.getCommentBeans();
                if (commentBeans == null) {
                    commentBeans = new ArrayList<>();
                }
                commentBean.setId(id);
                commentBeans.add(commentBean);
                friendCircleBean.setCommentBeans(commentBeans);
                notifyFeedChanged(position, "comment");
            }

            @Override
            public void onFailure(int errorCode) {
                // do nothing
            }
        });
        hideCommentFragment();
    }

    public void replyComment(long feedId, long commentId, String comment) {
        int position = mFriendCircleAdapter.getFeedPosition(feedId);
        CommentBean parentCommentBean = mFriendCircleAdapter.getCommentBean(feedId, commentId);
        MomentClient.getInstance().postComment(FeedCommentType.Comment_Comment_Type, feedId, comment, parentCommentBean.getChildUserId(), commentId, null, new MomentClient.PostCallback() {
            @Override
            public void onSuccess(long id, long timestamp) {
                if (getView() == null) {
                    return;
                }
                FriendCircleBean friendCircleBean = mFriendCircleAdapter.getFriendCircleBean(feedId);
                CommentBean commentBean = new CommentBean();
                commentBean.setCommentContent(comment);
                UserInfo userInfo = ChatManager.Instance().getUserInfo(ChatManager.Instance().getUserId(), false);
                commentBean.setChildUserId(userInfo.uid);
                commentBean.setChildUserName(userInfo.displayName);
                commentBean.setParentUserId(parentCommentBean.getChildUserId());
                commentBean.setParentUserName(parentCommentBean.getChildUserName());
                commentBean.setCommentType(Constants.CommentType.COMMENT_TYPE_REPLY);
                List<CommentBean> commentBeans = friendCircleBean.getCommentBeans();
                if (commentBeans == null) {
                    commentBeans = new ArrayList<>();
                }
                commentBean.setId(id);
                commentBeans.add(commentBean);
                friendCircleBean.setCommentBeans(commentBeans);
                notifyFeedChanged(position, "comment");
            }

            @Override
            public void onFailure(int errorCode) {
                // do nothing
            }
        });
        hideCommentFragment();
    }

    private int getKeyboardPortraitHeight() {
        int defaultCustomKeyboardSize = getResources().getDimensionPixelSize(R.dimen.default_custom_keyboard_size);
        int minCustomKeyboardSize = getResources().getDimensionPixelSize(R.dimen.min_custom_keyboard_size);
        int keyboardHeight = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("keyboard_height_portrait", defaultCustomKeyboardSize);
        return Math.max(keyboardHeight, minCustomKeyboardSize);
    }

    /**
     * 评论输入条顶边的屏幕坐标，用来把被评论的那条 feed 滚到键盘上方。
     * 首选取 {@link CommentFragment} 上次实测记下来的值；没有时按「屏幕高 - 键盘高 - 40dp」估一个。
     */
    private int getCommentPanelTop() {
        int commentPanelTop = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("commentPanelTop", 0);
        if (commentPanelTop > 0) {
            return commentPanelTop;
        }

        WindowManager manager = requireActivity().getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int height = outMetrics.heightPixels;
        return height - (getKeyboardPortraitHeight() + Utils.dp2px(40));
    }

    protected void deleteFeed(int feedPosition) {
        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(feedPosition);
        MomentClient.getInstance().deleteFeed(user != null ? user.uid : null, friendCircleBean.getId(), new MomentClient.GeneralCallback() {
            @Override
            public void onSuccess() {
                if (getView() == null) {
                    return;
                }
                friendCircleBeans.remove(feedPosition);
                mFriendCircleAdapter.notifyItemRemoved(feedPosition + mFriendCircleAdapter.headerCount());
            }

            @Override
            public void onFailure(int errorCode) {
                // do nothing
            }
        });
    }

    protected void deleteComment(int feedPosition, int commentPosition) {
        List<FriendCircleBean> friendCircleBeans = mFriendCircleAdapter.getFriendCircleBeans();
        FriendCircleBean friendCircleBean = friendCircleBeans.get(feedPosition);
        List<CommentBean> commentBeans = friendCircleBean.getCommentBeans();
        CommentBean commentBean = commentBeans.get(commentPosition);
        MomentClient.getInstance().deleteComment(user != null ? user.uid : null, friendCircleBean.getId(), commentBean.getId(), new MomentClient.GeneralCallback() {
            @Override
            public void onSuccess() {
                commentBeans.remove(commentBean);
                friendCircleBean.setCommentBeans(commentBeans);
                notifyFeedChanged(feedPosition, "comment");
            }

            @Override
            public void onFailure(int errorCode) {
                // do nothing
            }
        });
    }

    /**
     * 点某人的头像/名字 → 打开他的朋友圈。
     * <p>
     * 走 {@link WfcPageCompat}：平板上压到本页所在的那条右栏栈上，返回能退回来。
     * 手机端逐字节等价于改造前的 {@code startActivity}。
     */
    @Override
    public void onCommentUserClick(String userId) {
        Intent intent = new Intent(requireContext(), FeedListActivity.class);
        intent.putExtra(ARG_USER_INFO, ChatManager.Instance().getUserInfo(userId, false));
        WfcPageCompat.startPage(this, intent);
    }

    @Override
    public void onFeedUserClick(String userId) {
        onCommentUserClick(userId);
    }

    public void toggleCommentOrPraisePopupWindow(View anchorView, FriendCircleBean friendCircleBean, int position) {
        if (mCommentOrPraisePopupWindow == null) {
            mCommentOrPraisePopupWindow = new CommentOrPraisePopupWindow(requireContext());
        }
        mCommentOrPraisePopupWindow
            .setOnPraiseOrCommentClickListener(this)
            .setCurrentPosition(position);
        if (mCommentOrPraisePopupWindow.isShowing()) {
            mCommentOrPraisePopupWindow.dismiss();
        } else {
            hideCommentFragment();
            mCommentOrPraisePopupWindow.showPopupWindow(anchorView);
        }
    }

    @Override
    public void onDestroyView() {
        // 右栏里宿主 Activity 长期活着，本页出栈只销毁视图。弹窗挂在窗口上，不主动收会漏。
        if (mCommentOrPraisePopupWindow != null) {
            if (mCommentOrPraisePopupWindow.isShowing()) {
                mCommentOrPraisePopupWindow.dismiss();
            }
            mCommentOrPraisePopupWindow = null;
        }
        commentFragment = null;
        mFriendCircleAdapter = null;
        recyclerView = null;
        layoutManager = null;
        super.onDestroyView();
    }
}
