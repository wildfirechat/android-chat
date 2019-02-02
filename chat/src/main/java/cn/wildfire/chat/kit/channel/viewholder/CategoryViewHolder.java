package cn.wildfire.chat.kit.channel.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;

public class CategoryViewHolder extends RecyclerView.ViewHolder {
    @Bind(R.id.categoryTextView)
    TextView categoryTextView;

    public CategoryViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void bind(String category) {
        categoryTextView.setText(category);
    }
}
