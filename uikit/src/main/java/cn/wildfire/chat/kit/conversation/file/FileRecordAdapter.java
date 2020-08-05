package cn.wildfire.chat.kit.conversation.file;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.FileRecord;

class FileRecordAdapter extends RecyclerView.Adapter<FileRecordViewHolder> {
    private List<FileRecord> fileRecords;

    public void setFileRecords(List<FileRecord> fileRecords) {
        this.fileRecords = fileRecords;
    }

    public List<FileRecord> getFileRecords() {
        return fileRecords;
    }

    public void addFileRecords(List<FileRecord> fileRecords) {
        if (fileRecords == null || fileRecords.isEmpty()) {
            return;
        }
        this.fileRecords = this.fileRecords == null ? new ArrayList<>() : this.fileRecords;
        this.fileRecords.addAll(fileRecords);
    }

    @NonNull
    @Override
    public FileRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_record_item, parent, false);
        return new FileRecordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull FileRecordViewHolder holder, int position) {
        holder.onBind(fileRecords.get(position));
    }

    @Override
    public int getItemCount() {
        return fileRecords == null ? 0 : fileRecords.size();
    }
}
