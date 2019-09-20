package cn.wildfire.chat.kit.search;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public interface SearchableMudle<T, V extends ResultItemViewHolder> {
    V onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    void onBind(V holder, T t);

    void onClick(Fragment fragment, V holder, View view, T t);

    int priority();

    String category();

    List<T> search(String keyword);
}
