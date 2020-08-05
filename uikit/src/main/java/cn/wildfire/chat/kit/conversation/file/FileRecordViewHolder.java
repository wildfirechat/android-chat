package cn.wildfire.chat.kit.conversation.file;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.model.FileRecord;

class FileRecordViewHolder extends RecyclerView.ViewHolder {
    @BindView(R2.id.fileNameTextView)
    TextView fileNameTextView;
    @BindView(R2.id.fileSizeTextView)
    TextView fileSizeTextView;

    public FileRecordViewHolder(@NonNull View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void onBind(FileRecord fileRecord) {
        fileNameTextView.setText(fileRecord.name);
        fileSizeTextView.setText(FileUtils.getReadableFileSize(fileRecord.size));

    }
}
