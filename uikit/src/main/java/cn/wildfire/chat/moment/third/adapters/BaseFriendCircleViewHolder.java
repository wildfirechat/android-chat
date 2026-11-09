package cn.wildfire.chat.moment.third.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.moment.third.beans.FriendCircleBean;
import cn.wildfire.chat.moment.third.beans.OtherInfoBean;
import cn.wildfire.chat.moment.third.beans.UserBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnDeleteFeedClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedItemLongClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.moment.third.interfaces.OnTogglePraiseOrCommentPopupWindowListener;
import cn.wildfire.chat.moment.third.span.TextMovementMethod;
import cn.wildfire.chat.moment.third.utils.SpanUtils;
import cn.wildfire.chat.moment.third.widgets.VerticalCommentWidget;
import cn.wildfirechat.remote.ChatManager;

class BaseFriendCircleViewHolder extends RecyclerView.ViewHolder {

    public VerticalCommentWidget verticalCommentWidget;
    public TextView txtUserName;
    public View viewLine;
    public TextView txtPraiseContent;
    public ImageView imgAvatar;
    public TextView txtSource;
    public TextView txtPublishTime;
    public ImageView deleteIcon;
    public ImageView imgPraiseOrComment;
    public TextView txtLocation;
    public TextView txtContent;
    public TextView txtState;
    public View divideLine;
    public LinearLayout layoutPraiseAndComment;

    private Context mContext;
    private RequestOptions mRequestOptions = new RequestOptions()
        .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(10)));

    public BaseFriendCircleViewHolder(View itemView) {
        super(itemView);
        verticalCommentWidget = itemView.findViewById(R.id.vertical_comment_widget);
        txtUserName = itemView.findViewById(R.id.txt_user_name);
        txtPraiseContent = itemView.findViewById(R.id.praise_content);
        viewLine = itemView.findViewById(R.id.view_line);
        imgAvatar = itemView.findViewById(R.id.img_avatar);
        txtSource = itemView.findViewById(R.id.txt_source);
        txtPublishTime = itemView.findViewById(R.id.txt_publish_time);
        deleteIcon = itemView.findViewById(R.id.img_del);
        imgPraiseOrComment = itemView.findViewById(R.id.img_click_praise_or_comment);
        txtLocation = itemView.findViewById(R.id.txt_location);
        txtContent = itemView.findViewById(R.id.txt_content);
        txtState = itemView.findViewById(R.id.txt_state);
        layoutPraiseAndComment = itemView.findViewById(R.id.layout_praise_and_comment);
        divideLine = itemView.findViewById(R.id.view_divide_line);
        txtPraiseContent.setMovementMethod(new TextMovementMethod());
    }

    public void makeUserBaseData(BaseFriendCircleViewHolder holder,
                                 Context context,
                                 FriendCircleBean friendCircleBean,
                                 int position,
                                 boolean onlyPraise,
                                 boolean onlyComment,
                                 OnFeedItemLongClickListener onFeedItemLongClickListener,
                                 OnFeedUserClickListener onFeedUserClickListener,
                                 OnCommentUserClickListener onCommentUserClickListener,
                                 OnDeleteFeedClickListener onDeleteFeedClickListener,
                                 OnTogglePraiseOrCommentPopupWindowListener onTogglePraiseOrCommentPopupWindowListener) {
        this.mContext = context;
        if (!onlyPraise && !onlyComment) {
            if (!TextUtils.isEmpty(friendCircleBean.getContentSpan())) {
                holder.txtContent.setVisibility(View.VISIBLE);
                holder.txtContent.setText(friendCircleBean.getContentSpan());
                setContentShowState(holder, friendCircleBean);
                holder.txtContent.setOnLongClickListener(v -> {
                    if (onFeedItemLongClickListener != null) {
                        onFeedItemLongClickListener.onFeedItemLongClick(v, position);
                    }
                    return true;
                });
            } else {
                holder.txtContent.setVisibility(View.GONE);
            }

            UserBean userBean = friendCircleBean.getUserBean();
            if (userBean != null) {
                holder.txtUserName.setText(userBean.getUserName());
                holder.txtUserName.setOnClickListener(v -> {
                    if (onFeedUserClickListener != null) {
                        onFeedUserClickListener.onFeedUserClick(userBean.getUserId());
                    }
                });
                holder.imgAvatar.setOnClickListener(v -> {
                    if (onFeedUserClickListener != null) {
                        onFeedUserClickListener.onFeedUserClick(userBean.getUserId());
                    }
                });
                Glide.with(mContext).load(userBean.getUserAvatarUrl())
                    .apply(mRequestOptions)
                    .into(holder.imgAvatar);

                if (userBean.getUserId().equals(ChatManager.Instance().getUserId())) {
                    deleteIcon.setVisibility(View.VISIBLE);
                    deleteIcon.setOnClickListener(v -> {
                        if (onDeleteFeedClickListener != null) {
                            onDeleteFeedClickListener.onDeleteFeedClick(position);
                        }
                    });
                } else {
                    deleteIcon.setVisibility(View.GONE);
                }
            } else {
                deleteIcon.setVisibility(View.GONE);
            }

            OtherInfoBean otherInfoBean = friendCircleBean.getOtherInfoBean();

            if (otherInfoBean != null) {
                holder.txtSource.setText(otherInfoBean.getSource());
                holder.txtPublishTime.setText(otherInfoBean.getTime());
            }
            holder.txtLocation.setOnClickListener(v -> Toast.makeText(mContext, "You Click Location", Toast.LENGTH_SHORT).show());

            holder.imgPraiseOrComment.setOnClickListener(v -> {
                if (onTogglePraiseOrCommentPopupWindowListener != null) {
                    onTogglePraiseOrCommentPopupWindowListener.togglePraiseOrCommentPopupWindow(holder.imgPraiseOrComment, friendCircleBean, position);
                }
            });
        }

        if (friendCircleBean.isShowPraise() || friendCircleBean.isShowComment()) {
            holder.layoutPraiseAndComment.setVisibility(View.VISIBLE);
            if (friendCircleBean.isShowComment() && friendCircleBean.isShowPraise()) {
                holder.viewLine.setVisibility(View.VISIBLE);
            } else {
                holder.viewLine.setVisibility(View.GONE);
            }
            if (friendCircleBean.isShowPraise()) {
                friendCircleBean.setPraiseSpan(SpanUtils.makePraiseSpan(mContext, friendCircleBean.getPraiseBeans(), onCommentUserClickListener));
                holder.txtPraiseContent.setVisibility(View.VISIBLE);
                holder.txtPraiseContent.setText(friendCircleBean.getPraiseSpan());
            } else {
                holder.txtPraiseContent.setVisibility(View.GONE);
            }
            if (friendCircleBean.isShowComment()) {
                holder.verticalCommentWidget.setVisibility(View.VISIBLE);
                holder.verticalCommentWidget.addComments(position, friendCircleBean.getCommentBeans(), false);
            } else {
                holder.verticalCommentWidget.setVisibility(View.GONE);
            }
        } else {
            holder.layoutPraiseAndComment.setVisibility(View.GONE);
        }

    }

    private void setContentShowState(BaseFriendCircleViewHolder holder, FriendCircleBean friendCircleBean) {
        if (friendCircleBean.isShowCheckAll()) {
            holder.txtState.setVisibility(View.VISIBLE);
            setTextState(holder, friendCircleBean.isExpanded());
            holder.txtState.setOnClickListener(v -> {
                if (friendCircleBean.isExpanded()) {
                    friendCircleBean.setExpanded(false);
                } else {
                    friendCircleBean.setExpanded(true);
                }
                setTextState(holder, friendCircleBean.isExpanded());
            });
        } else {
            holder.txtState.setVisibility(View.GONE);
            holder.txtContent.setMaxLines(Integer.MAX_VALUE);
        }
    }

    private void setTextState(BaseFriendCircleViewHolder holder, boolean isExpand) {
        if (isExpand) {
            holder.txtContent.setMaxLines(Integer.MAX_VALUE);
            holder.txtState.setText("收起");
        } else {
            holder.txtContent.setMaxLines(4);
            holder.txtState.setText("全文");
        }
    }

}
