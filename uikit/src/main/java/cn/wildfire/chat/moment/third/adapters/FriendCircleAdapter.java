package cn.wildfire.chat.moment.third.adapters;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.moment.third.Constants;
import cn.wildfire.chat.moment.third.beans.CommentBean;
import cn.wildfire.chat.moment.third.beans.FriendCircleBean;
import cn.wildfire.chat.moment.third.beans.UserBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnDeleteFeedClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnTogglePraiseOrCommentPopupWindowListener;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfire.chat.moment.third.widgets.NineGridView;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.model.Profile;
import cn.wildfirechat.remote.ChatManager;

/**
 * @author KCrason
 * @date 2018/4/27
 */
public class FriendCircleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private List<FriendCircleBean> mFriendCircleBeans;

    private RequestOptions mRequestOptions;
    private RequestOptions mAvatarRequestOptions;

    private int mAvatarSize;

    private DrawableTransitionOptions mDrawableTransitionOptions;

    private OnTogglePraiseOrCommentPopupWindowListener onTogglePraiseOrCommentPopupWindowListener;
    private OnCommentItemClickListener onCommentItemClickListener;
    private OnCommentItemLongClickListener onCommentItemLongClickListener;
    private OnFeedItemLongClickListener onFeedItemLongClickListener;
    private OnCommentUserClickListener onCommentUserClickListener;
    private OnDeleteFeedClickListener onDeleteFeedClickListener;
    private OnFeedUserClickListener onFeedUserClickListener;

    private LinearLayoutManager mLayoutManager;

    private RecyclerView mRecyclerView;

    private HostViewHolder headerViewHolder;
    private Profile profile;
    private UserInfo userInfo;

    private int lastItemType = LAST_ITEM_TYPE_NORMAL;
    private static final int LAST_ITEM_TYPE_NORMAL = 0;
    private static final int LAST_ITEM_TYPE_LOADING = 1;
    private static final int LAST_ITEM_TYPE_VISIBLE_SCOPE = 2;
    private static final int LAST_ITEM_TYPE_BOTTOM_PADDING = 3;

    public FriendCircleAdapter(Context context, RecyclerView recyclerView) {
        this.mContext = context;
        mRecyclerView = recyclerView;
        this.mLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.mAvatarSize = Utils.dp2px(44f);
        this.mLayoutInflater = LayoutInflater.from(context);
        this.mRequestOptions = new RequestOptions().centerCrop();
        this.mAvatarRequestOptions = new RequestOptions()
                .placeholder(R.mipmap.avatar_def)
                .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(13)));
        this.mDrawableTransitionOptions = DrawableTransitionOptions.withCrossFade();
    }

    public void setHeaderViewHolder(HostViewHolder headerViewHolder) {
        this.headerViewHolder = headerViewHolder;
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

    public void setOnDeleteFeedClickListener(OnDeleteFeedClickListener onDeleteFeedClickListener) {
        this.onDeleteFeedClickListener = onDeleteFeedClickListener;
    }

    public void setOnFeedUserClickListener(OnFeedUserClickListener onFeedUserClickListener) {
        this.onFeedUserClickListener = onFeedUserClickListener;
    }

    public void setOnPraiseOrCommentClickListener(OnTogglePraiseOrCommentPopupWindowListener onTogglePraiseOrCommentPopupWindowListener) {
        this.onTogglePraiseOrCommentPopupWindowListener = onTogglePraiseOrCommentPopupWindowListener;
    }

    public void setFriendCircleBeans(List<FriendCircleBean> friendCircleBeans) {
        this.mFriendCircleBeans = friendCircleBeans;
        notifyDataSetChanged();
    }

    public void updateFriendCircleBean(int index, FriendCircleBean friendCircleBean) {
        if (this.mFriendCircleBeans == null || this.mFriendCircleBeans.isEmpty() || index >= this.mFriendCircleBeans.size()) {
            return;
        }
        this.mFriendCircleBeans.set(index, friendCircleBean);
        notifyItemChanged(headerCount() + index);
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
        notifyItemChanged(0);
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
        notifyItemChanged(0);
    }

    public List<FriendCircleBean> getFriendCircleBeans() {
        return mFriendCircleBeans;
    }

    public int getFeedPosition(long feedId) {
        if (mFriendCircleBeans == null) {
            return -1;
        }
        for (int i = 0; i < mFriendCircleBeans.size(); i++) {
            if (mFriendCircleBeans.get(i).getId() == feedId) {
                return i;
            }
        }
        return -1;
    }

    public FriendCircleBean getFriendCircleBean(long feedId) {
        if (mFriendCircleBeans == null) {
            return null;
        }
        for (int i = 0; i < mFriendCircleBeans.size(); i++) {
            if (mFriendCircleBeans.get(i).getId() == feedId) {
                return mFriendCircleBeans.get(i);
            }
        }
        return null;
    }

    public CommentBean getCommentBean(long feedId, long commendId) {
        FriendCircleBean friendCircleBean = getFriendCircleBean(feedId);
        if (friendCircleBean == null) {
            return null;
        }
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

    public void showVisibleScopeItem() {
        if (lastItemType == LAST_ITEM_TYPE_VISIBLE_SCOPE) {
            return;
        }
        if (lastItemType == LAST_ITEM_TYPE_LOADING) {
            lastItemType = LAST_ITEM_TYPE_VISIBLE_SCOPE;
            notifyItemChanged(getItemCount() - 1);
        } else {
            int position = getItemCount();
            lastItemType = LAST_ITEM_TYPE_VISIBLE_SCOPE;
            notifyItemInserted(position);
        }
    }

    public void showBottomPadding() {
        if (lastItemType == LAST_ITEM_TYPE_BOTTOM_PADDING) {
            return;
        }
        int position = getItemCount();
        lastItemType = LAST_ITEM_TYPE_BOTTOM_PADDING;
        notifyItemInserted(position);
    }

    public void addFriendCircleBeans(List<FriendCircleBean> friendCircleBeans) {
        if (friendCircleBeans != null) {
            if (mFriendCircleBeans == null) {
                mFriendCircleBeans = new ArrayList<>();
            }
            this.mFriendCircleBeans.addAll(friendCircleBeans);
            notifyItemRangeInserted(mFriendCircleBeans.size(), friendCircleBeans.size());
        }
    }

    public void updateUserInfo(List<UserInfo> userInfos) {
        if (mFriendCircleBeans == null) {
            return;
        }
        for (int i = 0; i < mFriendCircleBeans.size(); i++) {
            FriendCircleBean friendCircleBean = mFriendCircleBeans.get(i);
            UserBean userBean = friendCircleBean.getUserBean();
            for (int j = 0; j < userInfos.size(); j++) {
                UserInfo info = userInfos.get(j);
                if (userBean.getUserId().equals(info.uid)) {
                    friendCircleBean.setUserBean(UserBean.fromUserInfo(info));
                    notifyItemChanged(i);
                    break;
                }
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;
        if (viewType == Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_HEADER) {
            return headerViewHolder;
        } else if (viewType == Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_ONLY_WORD) {
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
        } else if (viewType == R.layout.moment_feed_item_bottom_padding) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View itemView = layoutInflater.inflate(R.layout.moment_feed_item_bottom_padding, parent, false);
            holder = new BottomPaddingViewHolder(itemView);

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
            position -= headerCount();
            if (mFriendCircleBeans != null && position < mFriendCircleBeans.size()) {
                FriendCircleBean friendCircleBean = mFriendCircleBeans.get(position);
                ((BaseFriendCircleViewHolder) holder).makeUserBaseData((BaseFriendCircleViewHolder) holder, mContext, friendCircleBean, position, onlyUpdatePraiseOrComment, onlyUpdatePraiseOrComment, onFeedItemLongClickListener, onFeedUserClickListener, onCommentUserClickListener, onDeleteFeedClickListener, onTogglePraiseOrCommentPopupWindowListener);
                if (holder instanceof OnlyWordViewHolder) {
                    OnlyWordViewHolder onlyWordViewHolder = (OnlyWordViewHolder) holder;
                } else if (holder instanceof WordAndUrlViewHolder) {
                    WordAndUrlViewHolder wordAndUrlViewHolder = (WordAndUrlViewHolder) holder;
                    wordAndUrlViewHolder.layoutUrl.setOnClickListener(v -> Toast.makeText(mContext, "You Click Layout Url", Toast.LENGTH_SHORT).show());
                } else if (holder instanceof WordAndImagesViewHolder && friendCircleBean.getMediaEntries() != null) {
                    WordAndImagesViewHolder wordAndImagesViewHolder = (WordAndImagesViewHolder) holder;
                    wordAndImagesViewHolder.nineGridView.setOnImageClickListener((position1, view) -> {

                        WeakReference<NineGridView> rvRef = new WeakReference<>(wordAndImagesViewHolder.nineGridView);

                        MMPreviewActivity.SourceRectProvider provider = entry -> {
                            NineGridView nineGridView = rvRef.get();
                            if (nineGridView == null || entry == null) return null;

                            NineImageAdapter adapter = (NineImageAdapter) wordAndImagesViewHolder.nineGridView.getmAdapter();
                            for (int i = 0; i < adapter.getCount(); i++) {
                                if (TextUtils.equals(adapter.getItem(i).getMediaUrl(), entry.getMediaUrl())) {
                                    View thumb = nineGridView.getImageViews().get(i);
                                    if (thumb != null && thumb.isShown()) {
                                        int[] loc = new int[2];
                                        thumb.getLocationOnScreen(loc);
                                        return new Rect(loc[0], loc[1],
                                                loc[0] + thumb.getWidth(), loc[1] + thumb.getHeight());
                                    }
                                }
                            }
                            return null;
                        };
                        MMPreviewActivity.previewMedia(mContext, friendCircleBean.getMediaEntries(), position1, false, (ImageView) view, provider);
                    });
                    if (friendCircleBean.getMediaEntries().size() == 1) {
                        MediaEntry mediaEntry = friendCircleBean.getMediaEntries().get(0);
                        wordAndImagesViewHolder.nineGridView.setSingleImageSize(mediaEntry.getWidth(), mediaEntry.getHeight());
                    }
                    wordAndImagesViewHolder.nineGridView.setAdapter(new NineImageAdapter(mContext, wordAndImagesViewHolder.nineGridView, mRequestOptions,
                            mDrawableTransitionOptions, friendCircleBean.getMediaEntries()));
                }
            }
        } else if (holder instanceof HostViewHolder) {
            // header
            ((HostViewHolder) holder).bind(mContext, userInfo, profile);
        } else if (holder instanceof VisibleScopeViewHolder) {
            String selfUid = ChatManager.Instance().getUserId();
            // 朋友圈首页
            if (userInfo == null || selfUid.equals(userInfo.uid)) {
                ((VisibleScopeViewHolder) holder).visibleScopeLinearLayout.setVisibility(View.INVISIBLE);
            } else {
                if (ChatManager.Instance().isMyFriend(userInfo.uid)) {
                    if (profile.visibleScope == Profile.VisibleScope.VisibleScope_NoLimit) {
                        ((VisibleScopeViewHolder) holder).visibleScopeLinearLayout.setVisibility(View.INVISIBLE);
                    } else {
                        String desc;
                        if (profile.visibleScope == Profile.VisibleScope.VisibleScope_3Days) {
                            desc = "只展示最近3天的朋友圈";
                        } else if (profile.visibleScope == Profile.VisibleScope.VisibleScope_1Month) {
                            desc = "只展示最近 一个月的朋友圈";
                            //VisibleScope_6Months = 3;
                        } else {
                            desc = "只展示最近半年的朋友圈";
                        }
                        ((VisibleScopeViewHolder) holder).visibleScopeLinearLayout.setVisibility(View.VISIBLE);
                        ((VisibleScopeViewHolder) holder).visibleScopeTextView.setText(desc);
                    }
                } else {
                    if (profile.strangerVisibleCount == 0) {
                        ((VisibleScopeViewHolder) holder).visibleScopeLinearLayout.setVisibility(View.INVISIBLE);
                    } else {
                        String desc;
                        if (profile.strangerVisibleCount == Profile.VisibleScope.VisibleScope_3Days) {
                            desc = "只展示最近3天的朋友圈";
                        } else if (profile.strangerVisibleCount == Profile.VisibleScope.VisibleScope_1Month) {
                            desc = "只展示最近 一个月的朋友圈";
                            //VisibleScope_6Months = 3;
                        } else {
                            desc = "只展示最近半年的朋友圈";
                        }
                        ((VisibleScopeViewHolder) holder).visibleScopeLinearLayout.setVisibility(View.VISIBLE);
                        ((VisibleScopeViewHolder) holder).visibleScopeTextView.setText(desc);
                    }
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (headerCount() > 0 && position == 0) {
            return Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_HEADER;
        } else if (mFriendCircleBeans != null && position < headerCount() + feedCount()) {
            return mFriendCircleBeans.get(position - headerCount()).getViewType();
        } else {
            int type = 0;
            switch (lastItemType) {
                case LAST_ITEM_TYPE_LOADING:
                    type = R.layout.moment_feed_item_loading;
                    break;
                case LAST_ITEM_TYPE_VISIBLE_SCOPE:
                    type = R.layout.moment_feed_item_visible_scope;
                    break;
                case LAST_ITEM_TYPE_BOTTOM_PADDING:
                    type = R.layout.moment_feed_item_bottom_padding;
                    break;
                default:
                    break;
            }
            return type;
        }
    }

    public int headerCount() {
        return headerViewHolder == null ? 0 : 1;
    }

    private int footerCount() {
        return lastItemType != LAST_ITEM_TYPE_NORMAL ? 1 : 0;
    }

    private int feedCount() {
        return mFriendCircleBeans == null ? 0 : mFriendCircleBeans.size();
    }

    @Override
    public int getItemCount() {
        return headerCount() + feedCount() + footerCount();
    }
}
