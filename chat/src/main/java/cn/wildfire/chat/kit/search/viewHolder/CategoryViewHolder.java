package cn.wildfire.chat.kit.search.viewHolder;

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

    public void onBind(String category) {
        categoryTextView.setText(category);
    }
}
