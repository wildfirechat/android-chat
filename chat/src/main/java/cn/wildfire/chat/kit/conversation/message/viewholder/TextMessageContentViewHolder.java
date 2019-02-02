package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.text.style.ImageSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.lqr.emoji.MoonUtils;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.kit.annotation.SendLayoutRes;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.TextMessageContent;

@MessageContentType(TextMessageContent.class)
@SendLayoutRes(resId = R.layout.conversation_item_text_send)
@ReceiveLayoutRes(resId = R.layout.conversation_item_text_receive)
@EnableContextMenu
public class TextMessageContentViewHolder extends NormalMessageContentViewHolder {
    @Bind(R.id.contentTextView)
    TextView contentTextView;

    public TextMessageContentViewHolder(FragmentActivity activity, RecyclerView.Adapter adapter, View itemView) {
        super(activity, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        MoonUtils.identifyFaceExpression(context, contentTextView, ((TextMessageContent) message.message.content).getContent(), ImageSpan.ALIGN_BOTTOM);
    }

    @OnClick(R.id.contentTextView)
    public void onClickTest(View view) {
        Toast.makeText(context, "onTextMessage click: " + ((TextMessageContent) message.message.content).getContent(), Toast.LENGTH_SHORT).show();
    }
}
