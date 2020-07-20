package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;

import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class ImageExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem(title = "照片")
    public void pickImage(View containerView, Conversation conversation) {
        Intent intent = ImagePicker.picker().showCamera(true).enableMultiMode(9).buildPickIntent(activity);
        startActivityForResult(intent, 100);
        TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_CAMERA);
        messageViewModel.sendMessage(conversation, content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {

                ChatManager.Instance().getWorkHandler().post(() -> {
                    //是否发送原图
                    boolean compress = data.getBooleanExtra(ImagePicker.EXTRA_COMPRESS, true);
                    ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                    for (ImageItem imageItem : images) {
                        File imageFileThumb;
                        File imageFileSource = null;
                        // FIXME: 2018/11/29 压缩, 不是发原图的时候，大图需要进行压缩
                        if (compress) {
                            imageFileSource = ImageUtils.compressImage(imageItem.path);
                        }
                        imageFileSource = imageFileSource == null ? new File(imageItem.path) : imageFileSource;
//                    if (isOrig) {
//                    imageFileSource = new File(imageItem.path);
                        imageFileThumb = ImageUtils.genThumbImgFile(imageItem.path);
                        if (imageFileThumb == null) {
                            Log.e("ImageExt", "gen image thumb fail");
                            return;
                        }
//                    } else {
//                        //压缩图片
//                        // TODO  压缩的有问题
//                        imageFileSource = ImageUtils.genThumbImgFileEx(imageItem.path);
//                        //imageFileThumb = ImageUtils.genThumbImgFile(imageFileSource.getAbsolutePath());
//                        imageFileThumb = imageFileSource;
//                    }
//                            messageViewModel.sendImgMsg(conversation, imageFileThumb, imageFileSource);
                        File finalImageFileSource = imageFileSource;
                        UIUtils.postTaskSafely(() -> messageViewModel.sendImgMsg(conversation, imageFileThumb, finalImageFileSource));

                    }

                });

            }
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_pic;
    }

    @Override
    public String title(Context context) {
        return "照片";
    }
}
