package cn.wildfire.chat.kit.voip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.GlideApp;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;

import static cn.wildfire.chat.kit.voip.MultiCallParticipantAdapter.ParticipantViewHolder;

public class MultiCallParticipantAdapter extends RecyclerView.Adapter<ParticipantViewHolder> {
    private List<UserInfo> participants;

    public List<UserInfo> getParticipants() {
        return participants;
    }

    public void setParticipants(List<UserInfo> participants) {
        this.participants = participants;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.av_multi_incoming_item, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        UserInfo userInfo = participants.get(position);
        ImageView imageView = holder.portraitImageView;
        GlideApp.with(imageView).load(userInfo.portrait).into(imageView);
        TextView textView = holder.nameTextView;
        textView.setText(userInfo.displayName);
    }

    @Override
    public int getItemCount() {
        return participants == null ? 0 : participants.size();
    }


    static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private ImageView portraitImageView;
        private TextView nameTextView;

        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
        }
    }
}
