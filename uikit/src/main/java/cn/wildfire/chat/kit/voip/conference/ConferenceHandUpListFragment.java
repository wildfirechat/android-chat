/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.livebus.LiveDataBus;
import cn.wildfire.chat.kit.voip.FullScreenBottomSheetDialogFragment;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceHandUpListFragment extends FullScreenBottomSheetDialogFragment {
    private RecyclerView recyclerView;
    private HandUpListAdapter adapter;
    private List<String> handUpList;

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_hand_up_list;
    }

    @Override
    protected String title() {
        return "发言申请";
    }

    @Override
    protected String confirmText() {
        return null;
    }

    @Override
    protected void afterCreateDialogView(View view) {
        super.afterCreateDialogView(view);
        initView(view);
    }

    private void initView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        adapter = new HandUpListAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));

        view.findViewById(R.id.putAllDownButton).setOnClickListener(v -> {
            ConferenceManager.getManager().putAllHandDown();
            dismiss();
        });

        loadAndShowHandUpList();

        LiveDataBus.subscribe("kConferenceCommandStateChanged", this, new Observer<Object>() {
            @Override
            public void onChanged(Object o) {
                loadAndShowHandUpList();
            }
        });

    }

    private void loadAndShowHandUpList() {
        this.handUpList = ConferenceManager.getManager().getHandUpMembers();
    }

    class HandUpListAdapter extends RecyclerView.Adapter<ApplyUnmuteViewHolder> {

        @NonNull
        @Override
        public ApplyUnmuteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            View view = layoutInflater.inflate(R.layout.av_conference_hand_up_list_item, parent, false);
            return new ApplyUnmuteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ApplyUnmuteViewHolder holder, int position) {
            holder.onBing(handUpList.get(position));
        }

        @Override
        public int getItemCount() {
            return handUpList == null ? 0 : handUpList.size();
        }
    }

    class ApplyUnmuteViewHolder extends RecyclerView.ViewHolder {
        private ImageView portraitImageView;
        private TextView nameTextView;

        public ApplyUnmuteViewHolder(@NonNull View itemView) {
            super(itemView);
            this.portraitImageView = itemView.findViewById(R.id.portraitImageView);
            this.nameTextView = itemView.findViewById(R.id.nameTextView);
            itemView.findViewById(R.id.putDownTextView).setOnClickListener(v -> {
                int position = getAdapterPosition();
                String userId = handUpList.get(position);
                ConferenceManager.getManager().putMemberHandDown(userId);
                dismiss();
            });
        }

        public void onBing(String userId) {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
            String displayName = ChatManager.Instance().getUserDisplayName(userInfo);
            nameTextView.setText(displayName);
            Glide.with(this.itemView).load(userInfo.portrait).placeholder(R.mipmap.avatar_def)
                .apply(RequestOptions.bitmapTransform(new RoundedCorners(10)))
                .into(portraitImageView);
        }
    }
}
