package cn.wildfire.chat.kit.contact.newfriend;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.user.UserInfoActivity;
import cn.wildfirechat.chat.R;

public class SearchUserFragment extends Fragment {
    private String keyword;
    private ContactViewModel contactViewModel;

    @Bind(R.id.noUserRelativeLayout)
    RelativeLayout noUserRelativeLayout;
    @Bind(R.id.searchLinearLayout)
    LinearLayout searchLinearLayout;
    @Bind(R.id.keywordTextView)
    TextView keywordTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(container.getContext()).inflate(R.layout.contact_search_user_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    public void showSearchPromptView(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        this.keyword = keyword;
        searchLinearLayout.setVisibility(View.VISIBLE);
        noUserRelativeLayout.setVisibility(View.GONE);
        keywordTextView.setText(keyword);
    }

    public void hideSearchPromptView() {
        this.keyword = null;
        searchLinearLayout.setVisibility(View.GONE);
        noUserRelativeLayout.setVisibility(View.GONE);
    }

    @OnClick(R.id.searchLinearLayout)
    void search() {
        if (!TextUtils.isEmpty(keyword)) {
            contactViewModel.searchUser(keyword).observe(this, userInfos -> {
                if (userInfos != null && !userInfos.isEmpty()) {
                    // show user info activity
                    Intent intent = new Intent(getActivity(), UserInfoActivity.class);
                    intent.putExtra("userInfo", userInfos.get(0));
                    startActivity(intent);
                    getActivity().finish();
                } else {
                    searchLinearLayout.setVisibility(View.GONE);
                    noUserRelativeLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void init() {
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
    }
}
