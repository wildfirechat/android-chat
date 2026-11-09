package cn.wildfire.chat.moment.third.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfire.chat.moment.third.Constants;
import cn.wildfire.chat.moment.third.beans.CommentBean;
import cn.wildfire.chat.moment.third.beans.FriendCircleBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnDeleteFeedClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnTogglePraiseOrCommentPopupWindowListener;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.moment.third.widgets.CommentOrPraisePopupWindow;

/**
 * @author KCrason
 * @date 2018/4/27
 */
public class FeedDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private FriendCircleBean friendCircleBean;

    private RequestOptions mRequestOptions;

    private int mAvatarSize;

    private DrawableTransitionOptions mDrawableTransitionOptions;

    private CommentOrPraisePopupWindow mCommentOrPraisePopupWindow;

    private OnTogglePraiseOrCommentPopupWindowListener onTogglePraiseOrCommentPopupWindowListener;
    private OnCommentItemClickListener onCommentItemClickListener;
    private OnCommentItemLongClickListener onCommentItemLongClickListener;
    private OnFeedItemLongClickListener onFeedItemLongClickListener;
    private OnCommentUserClickListener onCommentUserClickListener;
    private OnDeleteFeedClickListener onDeleteFeedClickListener;
    private OnFeedUserClickListener onFeedUserClickListener;

    private LinearLayoutManager mLayoutManager;

    private RecyclerView mRecyclerView;

    private int lastItemType = LAST_ITEM_TYPE_NORMAL;
    private static final int LAST_ITEM_TYPE_NORMAL = 0;
    private static final int LAST_ITEM_TYPE_LOADING = 1;

    public FeedDetailAdapter(Context context, RecyclerView recyclerView) {
        this.mContext = context;
        mRecyclerView = recyclerView;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mAvatarSize = Utils.dp2px(44f);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mRequestOptions = new RequestOptions().centerCrop();
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
    }

    public void setOnCommentItemClickListener(OnCommentItemClickListener onCommentItemClickListener) {
        this.onCommentItemClickListener = onCommentItemClickListener;
    }

    public void setOnCommentItemLongClickListener(OnCommentItemLongClickListener onCommentItemLongClickListener) {
        this.onCommentItemLongClickListener = onCommentItemLongClickListener;
    }

    public void setOnFeedItemLongClickListener(OnFeedItemLongClickListener onFeedItemLongClickListener) {
        this.onFeedItemLongClickListener = onFeedItemLongClickListener;
    }

    public void setOnCommentUserClickListener(OnCommentUserClickListener onCommentUserClickListener) {
        this.onCommentUserClickListener = onCommentUserClickListener;
    }

    public void setOnFeedUserClickListener(OnFeedUserClickListener onFeedUserClickListener) {
        this.onFeedUserClickListener = onFeedUserClickListener;
    }

    public void setFriendCircleBean(FriendCircleBean friendCircleBean) {
        this.friendCircleBean = friendCircleBean;
    }

    public CommentBean getCommentBean(long feedId, long commendId) {
        List<CommentBean> commentBeans = friendCircleBean.getCommentBeans();
        if (commentBeans == null) {
            return null;
        }
        for (CommentBean bean : commentBeans) {
            if (bean.getId() == commendId) {
                return bean;
            }
        }
        return null;
    }

    public void showLoadingOldFeedItem() {
        if (lastItemType != LAST_ITEM_TYPE_NORMAL) {
            return;
        }
        int count = getItemCount();
        lastItemType = LAST_ITEM_TYPE_LOADING;
        notifyItemInserted(count);
    }

    public void hideLoadingOldFeedItem() {
        if (lastItemType != LAST_ITEM_TYPE_LOADING) {
            return;
        }
        int count = getItemCount();
        lastItemType = LAST_ITEM_TYPE_NORMAL;
        notifyItemRemoved(count);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        if (viewType == Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_ONLY_WORD) {
            holder = new OnlyWordViewHolder(mLayoutInflater.inflate(R.layout.moment_item_recycler_firend_circle_only_word, parent, false));
        } else if (viewType == Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_WORD_AND_URL) {
            holder = new WordAndUrlViewHolder(mLayoutInflater.inflate(R.layout.moment_item_recycler_firend_circle_word_and_url, parent, false));
        } else if (viewType == Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_WORD_AND_IMAGES) {
            holder = new WordAndImagesViewHolder(mLayoutInflater.inflate(R.layout.moment_item_recycler_firend_circle_word_and_images, parent, false));
        } else if (viewType == R.layout.moment_feed_item_loading) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View itemView = layoutInflater.inflate(R.layout.moment_feed_item_loading, parent, false);
            holder = new LoadingViewHolder(itemView);
        } else if (viewType == R.layout.moment_feed_item_visible_scope) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View itemView = layoutInflater.inflate(R.layout.moment_feed_item_visible_scope, parent, false);
            holder = new VisibleScopeViewHolder(itemView);
        }
        if (holder instanceof BaseFriendCircleViewHolder) {
            ((BaseFriendCircleViewHolder) holder).verticalCommentWidget.setOnCommentItemClickListener(onCommentItemClickListener);
            ((BaseFriendCircleViewHolder) holder).verticalCommentWidget.setOnCommentItemLongClickListener(onCommentItemLongClickListener);
            ((BaseFriendCircleViewHolder) holder).verticalCommentWidget.setOnCommentUserClickListener(onCommentUserClickListener);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder(holder, position, false);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            onBindViewHolder(holder, position, true);
        }
    }

    private void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean onlyUpdatePraiseOrComment) {
        if (holder instanceof BaseFriendCircleViewHolder) {
            // header
            position--;
            ((BaseFriendCircleViewHolder) holder).makeUserBaseData((BaseFriendCircleViewHolder) holder, mContext, friendCircleBean, position, onlyUpdatePraiseOrComment, onlyUpdatePraiseOrComment, onFeedItemLongClickListener, onFeedUserClickListener, onCommentUserClickListener, onDeleteFeedClickListener, onTogglePraiseOrCommentPopupWindowListener);
            if (holder instanceof OnlyWordViewHolder) {
                OnlyWordViewHolder onlyWordViewHolder = (OnlyWordViewHolder) holder;
            } else if (holder instanceof WordAndUrlViewHolder) {
                WordAndUrlViewHolder wordAndUrlViewHolder = (WordAndUrlViewHolder) holder;
                wordAndUrlViewHolder.layoutUrl.setOnClickListener(v -> Toast.makeText(mContext, "You Click Layout Url", Toast.LENGTH_SHORT).show());
            } else if (holder instanceof WordAndImagesViewHolder) {
                WordAndImagesViewHolder wordAndImagesViewHolder = (WordAndImagesViewHolder) holder;
                wordAndImagesViewHolder.nineGridView.setOnImageClickListener((position1, view) ->
                    MMPreviewActivity.previewMedia(mContext, friendCircleBean.getMediaEntries(), position1, false, (ImageView)view, null));

                if (friendCircleBean.getMediaEntries().size() == 1) {
                    MediaEntry mediaEntry = friendCircleBean.getMediaEntries().get(0);
                    wordAndImagesViewHolder.nineGridView.setSingleImageSize(mediaEntry.getWidth(), mediaEntry.getHeight());
                }
                wordAndImagesViewHolder.nineGridView.setAdapter(new NineImageAdapter(mContext, wordAndImagesViewHolder.nineGridView, mRequestOptions,
                    mDrawableTransitionOptions, friendCircleBean.getMediaEntries()));
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return friendCircleBean.getViewType();
        } else {
            return R.layout.moment_feed_item_loading;
        }
    }

    @Override
    public int getItemCount() {
        int footerCount = lastItemType == LAST_ITEM_TYPE_LOADING ? 1 : 0;

        return 1 + footerCount;
    }


}
