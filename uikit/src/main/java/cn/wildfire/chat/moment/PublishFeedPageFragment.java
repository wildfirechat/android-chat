package cn.wildfire.chat.moment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfire.chat.moment.third.widgets.NineGridView;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.FeedContentType;
import cn.wildfirechat.moment.MomentClient;
import cn.wildfirechat.moment.model.Feed;
import cn.wildfirechat.moment.model.FeedEntry;
import cn.wildfirechat.remote.ChatManager;

/**
 * 发表朋友圈：文字 + 九宫格图片/一段视频 + 提醒谁看 + 谁可以看。
 * <p>
 * 逐行搬自 {@link PublishFeedActivity}，那个类现在只是手机端的壳。入口是朋友圈主页标题栏
 * 右上角相机（长按直接进纯文字发表），发表成功后 {@code RESULT_OK} 回给主页触发刷新。
 */
public class PublishFeedPageFragment extends MomentPageFragment implements NineGridView.OnImageClickListener {

    private EditText editText;
    private NineGridView nineGridView;
    private OptionItemView mentionOptionItemView;
    private OptionItemView visibleScopeOptionItemView;

    private FeedMediaContentAdapter nineGridAdapter;

    private String videoPath = null;

    private static final int REQUEST_CODE_PICK_IMAGE = 100;
    private static final int REQUEST_CODE_PICK_TO_MENTION = 101;
    private static final int REQUEST_CODE_VISIBLE_SCOPE = 102;

    private final ArrayList<String> mentionUids = new ArrayList<>();
    private final ArrayList<String> blockUids = new ArrayList<>();
    private final ArrayList<String> toUids = new ArrayList<>();
    private int mode;
    private boolean comporess = true;

    public static PublishFeedPageFragment fromIntent(Intent intent) {
        PublishFeedPageFragment fragment = new PublishFeedPageFragment();
        Bundle args = new Bundle();
        args.putString(PublishFeedActivity.VIDEO_URL, intent.getStringExtra(PublishFeedActivity.VIDEO_URL));
        args.putBoolean(PublishFeedActivity.IMAGE_COMPRESS, intent.getBooleanExtra(PublishFeedActivity.IMAGE_COMPRESS, true));
        args.putStringArrayList(PublishFeedActivity.IMAGE_URLS, intent.getStringArrayListExtra(PublishFeedActivity.IMAGE_URLS));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.moment_activity_publish;
    }

    @Override
    protected void initView(View view) {
        editText = view.findViewById(R.id.publish_input);
        nineGridView = view.findViewById(R.id.preview_image_content);
        mentionOptionItemView = view.findViewById(R.id.mentionOptionItemView);
        visibleScopeOptionItemView = view.findViewById(R.id.visibleScopeOptionItemView);

        mentionOptionItemView.setOnClickListener(v -> mention());
        visibleScopeOptionItemView.setOnClickListener(v -> visibleScope());

        Bundle args = getArguments();
        videoPath = args == null ? null : args.getString(PublishFeedActivity.VIDEO_URL);
        comporess = args == null || args.getBoolean(PublishFeedActivity.IMAGE_COMPRESS, true);
        ArrayList<String> imageUrls = args == null ? null : args.getStringArrayList(PublishFeedActivity.IMAGE_URLS);
        if (!TextUtils.isEmpty(videoPath) || (imageUrls != null && !imageUrls.isEmpty())) {
            nineGridAdapter = new FeedMediaContentAdapter(requireContext(), new RequestOptions().centerCrop(), DrawableTransitionOptions.withCrossFade());
            if (videoPath != null) {
                nineGridAdapter.setMediaUrls(Collections.singletonList(videoPath), true);
            } else {
                nineGridAdapter.setMediaUrls(imageUrls, false);
            }

            nineGridView.setAdapter(nineGridAdapter);
            nineGridView.setOnImageClickListener(this);
        } else {
            setTitleLeftText("发表文字");
            nineGridView.setVisibility(View.GONE);
        }
    }

    void mention() {
        Intent intent = PickContactActivity.buildPickIntent(requireContext(), 0, mentionUids, blockUids);
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_TO_MENTION);
    }

    void visibleScope() {
        Intent intent = new Intent(requireContext(), FeedVisibleScopeActivity.class);
        intent.putExtra(FeedVisibleScopeActivity.PARAM_VISIBLE_MODE, mode);
        if (mode == FeedVisibleScopeActivity.VISIBLE_MODE_PART) {
            intent.putExtra(FeedVisibleScopeActivity.PARAM_UIDS, toUids);
        } else if (mode == FeedVisibleScopeActivity.VISIBLE_MODE_BLOCK) {
            intent.putExtra(FeedVisibleScopeActivity.PARAM_UIDS, blockUids);
        }
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_VISIBLE_SCOPE);
    }

    @Override
    public void onTitleRightClick() {
        publishFeed();
    }

    private void mentionUsers(List<UserInfo> userInfos) {
        mentionUids.clear();

        for (UserInfo userInfo : userInfos) {
            mentionUids.add(userInfo.uid);
        }
        if (userInfos.isEmpty()) {
            mentionOptionItemView.setDesc("");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3 && i < userInfos.size(); i++) {
                sb.append(userInfos.get(i).displayName);
                sb.append("、");
            }
            sb.deleteCharAt(sb.length() - 1);
            if (userInfos.size() > 3) {
                sb.append("等");
            }
            mentionOptionItemView.setDesc(sb.toString());
        }
    }

    private void setFeedVisibleScope(int mode, List<UserInfo> userInfos) {
        this.mode = mode;
        blockUids.clear();
        toUids.clear();
        List<String> uids = new ArrayList<>();
        for (UserInfo userInfo : userInfos) {
            uids.add(userInfo.uid);
        }
        visibleScopeOptionItemView.setTitle("谁可以看");
        switch (mode) {
            case FeedVisibleScopeActivity.VISIBLE_MODE_ALL:
                visibleScopeOptionItemView.setDesc("公开");
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_PART:
                toUids.addAll(uids);
                visibleScopeOptionItemView.setDesc(buildUserNames(userInfos));
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_BLOCK:
                blockUids.addAll(uids);
                visibleScopeOptionItemView.setTitle("谁不可看");
                visibleScopeOptionItemView.setDesc(buildUserNames(userInfos));
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_PRIVATE:
                visibleScopeOptionItemView.setDesc("私密");
                toUids.add(ChatManager.Instance().getUserId());
                break;
            default:
                break;
        }
    }

    private String buildUserNames(List<UserInfo> userInfos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3 && i < userInfos.size(); i++) {
            sb.append(userInfos.get(i).displayName);
            sb.append("、");
        }
        sb.deleteCharAt(sb.length() - 1);
        if (userInfos.size() > 3) {
            sb.append("等");
        }
        return sb.toString();
    }

    @Override
    public void onImageClick(int position, View view) {
        if (position < nineGridAdapter.getImageUrls().size()) {
            // preview image
        } else {
            // add more image
            int count = 9 - nineGridAdapter.getImageUrls().size();
            Intent intent = ImagePicker.picker().showCamera(true).enableMultiMode(count).buildPickIntent(requireContext());
            WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_PICK_IMAGE) {
            if (data != null) {
                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                List<String> imageUrls = nineGridAdapter.getImageUrls();
                for (ImageItem imageItem : images) {
                    imageUrls.add(imageItem.path);
                }

                nineGridAdapter.setMediaUrls(imageUrls, false);
                nineGridView.setAdapter(nineGridAdapter);
            }
        } else if (requestCode == REQUEST_CODE_PICK_TO_MENTION) {
            List<UserInfo> pickedUserInfos = data.getParcelableArrayListExtra(PickContactActivity.RESULT_PICKED_USERS);
            mentionUsers(pickedUserInfos);
        } else if (requestCode == REQUEST_CODE_VISIBLE_SCOPE) {
            int mode = data.getIntExtra(FeedVisibleScopeActivity.RESULT_VISIBLE_MODE, 0);
            List<UserInfo> userInfos = data.getParcelableArrayListExtra(FeedVisibleScopeActivity.RESULT_USERS);
            setFeedVisibleScope(mode, userInfos);
        }
    }

    // TODO 是否发送原图
    private void publishFeed() {

        if (TextUtils.isEmpty(editText.getText().toString()) && (nineGridAdapter == null || (videoPath == null && nineGridAdapter.getImageUrls().isEmpty()))) {
            Toast.makeText(requireContext(), "请输入想发表的内容~", Toast.LENGTH_SHORT).show();
            return;
        }
        Feed feed = new Feed();
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        String text = editText.getText().toString();
        ChatManager.Instance().getWorkHandler().post(() -> {
            List<FeedEntry> feedEntries = new ArrayList<>();
            feed.medias = feedEntries;
            if (nineGridAdapter != null) {
                if (!TextUtils.isEmpty(videoPath) && !videoPath.endsWith(".png")) {
                    feed.type = FeedContentType.Content_Video_Type;
                    FeedEntry feedEntry = new FeedEntry();
                    String videoUrl = MomentClient.uploadMediaSync(videoPath);
                    if (videoUrl == null) {
                        toast("上传视频失败");
                        dialog.dismiss();
                        return;
                    }

                    Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MICRO_KIND);
                    try {
                        String thumbnailFilePath = Config.VIDEO_SAVE_DIR + File.pathSeparator + System.currentTimeMillis() + ".jpg";
                        FileOutputStream outputStream = new FileOutputStream(thumbnailFilePath);
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                        outputStream.close();
                        String thumbnailUrl = MomentClient.uploadMediaSync(thumbnailFilePath);
                        if (thumbnailUrl != null) {
                            feedEntry.mediaUrl = videoUrl;
                            feedEntry.thumbUrl = thumbnailUrl;

                            feedEntry.mediaWidth = thumbnail.getWidth();
                            feedEntry.mediaHeight = thumbnail.getHeight();
                            feedEntries.add(feedEntry);
                        }
                    } catch (Exception e) {
                        toast("上传失败 " + e.getMessage());
                        dialog.dismiss();
                        return;
                    }
                } else if (nineGridAdapter.getImageUrls().size() > 0) {
                    feed.type = FeedContentType.Content_Image_Type;
                    for (String imagePath : nineGridAdapter.getImageUrls()) {
                        if (comporess) {
                            File compressedImgFile = ImageUtils.compressImage(imagePath);
                            imagePath = compressedImgFile == null ? imagePath : compressedImgFile.getPath();
                        }
                        File imageFileThumb = ImageUtils.genThumbImgFile(imagePath);
                        String imageUrl = MomentClient.uploadMediaSync(imagePath);
                        String thumbnailUrl = null;
                        if (imageFileThumb != null) {
                            thumbnailUrl = MomentClient.uploadMediaSync(imageFileThumb.getPath());
                        }

                        if (imageUrl != null) {
                            FeedEntry feedEntry = new FeedEntry();
                            feedEntry.mediaUrl = imageUrl;
                            feedEntry.thumbUrl = thumbnailUrl;

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(imagePath, options);
                            int outHeight = options.outHeight;
                            feedEntry.mediaWidth = options.outWidth;
                            feedEntry.mediaHeight = outHeight;

                            feedEntries.add(feedEntry);
                        } else {
                            toast("上传图片失败");
                            dialog.dismiss();
                            return;
                        }
                    }
                }
            } else {
                feed.type = FeedContentType.Content_Text_Type;
            }

            feed.toUsers = toUids; // 谁可以看
            feed.excludeUsers = blockUids; // 谁不可以看
            feed.mentionedUser = mentionUids; // 提醒谁看
            feed.extra = null;
            feed.text = text;

            MomentClient.getInstance().postFeed(feed, new MomentClient.PostCallback() {
                @Override
                public void onSuccess(long feedUid, long timestamp) {
                    if (getView() == null) {
                        return;
                    }
                    dialog.dismiss();
                    feed.feedId = feedUid;
                    WfcPageCompat.setPageResult(PublishFeedPageFragment.this, Activity.RESULT_OK, null);
                    WfcPageCompat.finishPage(PublishFeedPageFragment.this);
                }

                @Override
                public void onFailure(int errorCode) {
                    if (getView() == null) {
                        return;
                    }
                    Toast.makeText(requireContext(), "post error " + errorCode, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });
    }

    private void toast(String msg) {
        ChatManager.Instance().getMainHandler().post(() -> {
            if (getContext() == null) {
                return;
            }
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });
    }
}
