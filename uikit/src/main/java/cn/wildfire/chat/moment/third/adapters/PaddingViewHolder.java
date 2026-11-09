package cn.wildfire.chat.moment.third.adapters;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.R;

public class PaddingViewHolder extends RecyclerView.ViewHolder {
    private View paddingView;

    public PaddingViewHolder(@NonNull View itemView) {
        super(itemView);
        paddingView = itemView.findViewById(R.id.padingView);
    }

    public void bind(int height) {
        ViewGroup.LayoutParams layoutParams = paddingView.getLayoutParams();
        layoutParams.height = height;
        paddingView.setLayoutParams(layoutParams);
    }
}
