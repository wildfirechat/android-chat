package cn.wildfire.chat.kit.search.viewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.GroupSearchResult;

public class GroupViewHolder extends ResultItemViewHolder<GroupSearchResult> {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.descTextView)
    TextView descTextView;

    public GroupViewHolder(Fragment fragment, View itemView) {
        super(fragment, itemView);
        ButterKnife.bind(this, itemView);
    }


    @Override
    public void onBind(String keyword, GroupSearchResult groupSearchResult) {
        nameTextView.setText(groupSearchResult.groupInfo.name);
        Glide.with(fragment).load(groupSearchResult.groupInfo.portrait).into(portraitImageView);

        String desc = "";
        switch (groupSearchResult.marchedType) {
            case 0:
                desc = "群名称包含: " + keyword;
                break;
            case 1:
                desc = "群成员包含: " + keyword;
                break;
            case 2:
                desc = "群名称和群成员都包含: " + keyword;
                break;
            default:
                break;
        }
        descTextView.setText(desc);
    }
}
