package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.lqr.emoji.MoonUtils;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.widget.LinkClickListener;
import cn.wildfire.chat.kit.widget.LinkTextViewMovementMethod;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.message.PTextMessageContent;
import cn.wildfirechat.message.TextMessageContent;


@MessageContentType(value = {
    TextMessageContent.class,
    PTextMessageContent.class

})
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    @BindView(R2.id.contentTextView)
    TextView contentTextView;

    public TextMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        String content = ((TextMessageContent) message.message.content).getContent();
        if (content.startsWith("<") && content.endsWith(">")) {
            contentTextView.setText(Html.fromHtml(content));
        } else {
            MoonUtils.identifyFaceExpression(fragment.getContext(), contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
        }
        contentTextView.setMovementMethod(new LinkTextViewMovementMethod(new LinkClickListener() {
            @Override
            public boolean onLinkClick(String link) {
                WfcWebViewActivity.loadUrl(fragment.getContext(), "", link);
                return true;
            }
        }));
    }

    @OnClick(R2.id.contentTextView)
    public void onClickTest(View view) {
//        Toast.makeText(fragment.getContext(), "onTextMessage click: " + ((TextMessageContent) message.message.content).getContent(), Toast.LENGTH_SHORT).show();
        String content = ((TextMessageContent) message.message.content).getContent();
        WfcWebViewActivity.loadHtmlContent(fragment.getActivity(), "消息内容", content);
    }


    @MessageContextMenuItem(tag = MessageContextMenuItemTags.TAG_CLIP, title = "复制", confirm = false, priority = 12)
    public void clip(View itemView, UiMessage message) {
        ClipboardManager clipboardManager = (ClipboardManager) fragment.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            return;
        }
        TextMessageContent content = (TextMessageContent) message.message.content;
        ClipData clipData = ClipData.newPlainText("messageContent", content.getContent());
        clipboardManager.setPrimaryClip(clipData);
    }
}
