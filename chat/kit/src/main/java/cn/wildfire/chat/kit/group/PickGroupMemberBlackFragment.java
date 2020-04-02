package cn.wildfire.chat.kit.group;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.pick.CheckableUserListBlackAdapter;
import cn.wildfire.chat.kit.contact.pick.PickedUserAdapter;
import cn.wildfire.chat.kit.contact.pick.PickedUserBlackAdapter;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.GroupInfo;

public class PickGroupMemberBlackFragment extends PickGroupMemberFragment {

    private static final int SPAN = 5;

    public static PickGroupMemberBlackFragment newInstance(GroupInfo groupInfo) {
        Bundle args = new Bundle();
        args.putParcelable("groupInfo", groupInfo);
        PickGroupMemberBlackFragment fragment = new PickGroupMemberBlackFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getContentLayoutResId() {
        return R.layout.contact_pick_fragment_black;
    }

    @Override
    public UserListAdapter onCreateUserListAdapter() {
        return new CheckableUserListBlackAdapter(this);
    }

    @Override
    protected void configPickedUserRecyclerView() {
        RecyclerView.LayoutManager pickedContactRecyclerViewLayoutManager = new GridLayoutManager(getActivity(), SPAN);
        pickedUserRecyclerView.setLayoutManager(pickedContactRecyclerViewLayoutManager);
        int space = (UIUtils.getDisplayWidth() - SPAN * UIUtils.dip2Px(52) - 2 * UIUtils.dip2Px(16)) / (SPAN - 1);
        pickedUserRecyclerView.addItemDecoration(new Decoration(space));
    }

    @Override
    protected PickedUserAdapter getPickedUserAdapter() {
        return new PickedUserBlackAdapter();
    }

    @Override
    protected void handleHintView(boolean focus) {
        if (focus) {
            hintView.setVisibility(View.GONE);
        } else {
            hintView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void handleEditText() {

    }

    private static class Decoration extends RecyclerView.ItemDecoration {

        private int space;

        public Decoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int column = position % SPAN;
            if (column < SPAN - 1) {
                outRect.right = space;
            } else {
                outRect.right = 0;
            }
            outRect.bottom = UIUtils.dip2Px(10);
        }
    }
}
