package cn.wildfire.chat.kit.contact.newfriend;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Bind;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.FriendRequest;

public class FriendRequestListFragment extends Fragment {
    @Bind(R.id.noNewFriendLinearLayout)
    LinearLayout noNewFriendLinearLayout;
    @Bind(R.id.newFriendListLinearLayout)
    LinearLayout newFriendLinearLayout;
    @Bind(R.id.friendRequestListRecyclerView)
    RecyclerView recyclerView;

    private ContactViewModel contactViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contact_new_friend_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        List<FriendRequest> requests = contactViewModel.getFriendRequest();
        if (requests != null && requests.size() > 0) {
            noNewFriendLinearLayout.setVisibility(View.GONE);
            newFriendLinearLayout.setVisibility(View.VISIBLE);

            FriendRequestListAdapter adapter = new FriendRequestListAdapter(FriendRequestListFragment.this);
            adapter.setFriendRequests(requests);
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
            recyclerView.setAdapter(adapter);
        } else {
            noNewFriendLinearLayout.setVisibility(View.VISIBLE);
            newFriendLinearLayout.setVisibility(View.GONE);
        }
    }
}
