package cn.wildfire.chat.kit.settings.blacklist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;


public class BlacklistListFragment extends Fragment implements BlacklistListAdapter.OnBlacklistItemClickListener, PopupMenu.OnMenuItemClickListener {
    @BindView(R2.id.recyclerView)
    RecyclerView recyclerView;
    private BlacklistViewModel blacklistViewModel;
    private BlacklistListAdapter blacklistListAdapter;

    private String selectedUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.blacklist_list_frament, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshBlacklist();
    }

    private void init() {
        blacklistViewModel = ViewModelProviders.of(getActivity()).get(BlacklistViewModel.class);

        blacklistListAdapter = new BlacklistListAdapter();
        blacklistListAdapter.setOnBlacklistItemClickListener(this);

        recyclerView.setAdapter(blacklistListAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    private void refreshBlacklist() {
        List<String> blacklists = blacklistViewModel.getBlacklists();
        blacklistListAdapter.setBlackedUserIds(blacklists);
        blacklistListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(String userId, View v) {

        PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.blacklist_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        popup.show();
        selectedUserId = userId;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.remove) {
            ChatManager.Instance().setBlackList(selectedUserId, false, new GeneralCallback() {
                @Override
                public void onSuccess() {
                    blacklistListAdapter.getBlackedUserIds().remove(selectedUserId);
                    blacklistListAdapter.notifyDataSetChanged();
                }

                @Override
                public void onFail(int errorCode) {
                    Toast.makeText(getActivity(), "删除失败", Toast.LENGTH_SHORT).show();
                }
            });
            return true;
        }
        return false;
    }
}
