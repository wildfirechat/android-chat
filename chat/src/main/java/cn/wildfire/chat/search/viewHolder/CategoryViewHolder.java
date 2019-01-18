package cn.wildfire.chat.search.viewHolder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import cn.wildfirechat.chat.R;

import butterknife.Bind;
import butterknife.ButterKnife;

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
