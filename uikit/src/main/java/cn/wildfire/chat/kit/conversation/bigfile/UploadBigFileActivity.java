package cn.wildfire.chat.kit.conversation.bigfile;

import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetUploadUrlCallback;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadBigFileActivity extends WfcBaseActivity implements View.OnClickListener {
    @BindView(R2.id.fileExtImageView)
    ImageView fileExtImageView;
    @BindView(R2.id.fileNameTextView)
    TextView fileNameTextView;
    @BindView(R2.id.fileSizeTextView)
    TextView fileSizeTextView;
    @BindView(R2.id.fileStatusTextView)
    TextView fileStatusTextView;
    @BindView(R2.id.fileUploadProgressBar)
    ProgressBar fileUploadProgressBar;
    @BindView(R2.id.actionButton)
    Button actionButton;

    String filePath;
    String remoteUrl;
    int state; //0 未上传；1 上传中；2 已上传；3 已取消；4 上传失败；5 已发送
    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    private Call mCall;
    private Conversation conversation;

    @Override
    protected int contentLayout() {
        return R.layout.activity_upload_big_file;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        filePath = getIntent().getStringExtra("filePath");
        conversation = getIntent().getParcelableExtra("conversation");
        fileExtImageView.setImageResource(FileUtils.getFileTypeImageResId(filePath));
        File file = new File(filePath);
        fileNameTextView.setText(file.getName());
        fileSizeTextView.setText(FileUtils.getReadableFileSize((int) file.length()));
        actionButton.setOnClickListener(this);
        updateState(0);
    }

    private void updateState(int newState) {
        if(state == 3 && newState == 4) {
            return;
        }

        state = newState;
        fileStatusTextView.setVisibility(View.VISIBLE);
        fileStatusTextView.setTextColor(Color.BLACK);
        fileUploadProgressBar.setVisibility(View.INVISIBLE);
        if(state == 0) {
            fileStatusTextView.setText("请点击上传");
            actionButton.setText("上传");
            actionButton.setBackgroundColor(Color.GRAY);
        } else if(state == 1) {
            fileStatusTextView.setVisibility(View.INVISIBLE);
            fileUploadProgressBar.setVisibility(View.VISIBLE);
            actionButton.setText("取消");
            actionButton.setBackgroundColor(Color.RED);
        } else if(state == 2) {
            fileStatusTextView.setText("上传成功，请点击发送");
            actionButton.setText("发送消息");
            actionButton.setBackgroundColor(Color.GREEN);
        } else if(state == 3) {
            fileStatusTextView.setText("已取消上传");
            actionButton.setText("上传");
            actionButton.setBackgroundColor(Color.GRAY);
        } else if(state == 4) {
            fileStatusTextView.setText("上传失败");
            actionButton.setText("重传");
            actionButton.setBackgroundColor(Color.GRAY);
        } else if(state == 5) {
            actionButton.setText("已发送");
            actionButton.setEnabled(false);
        }
    }

    private void updateProgress(int newProgress) {
        fileUploadProgressBar.setProgress(newProgress);
    }

    private void uploadFile() {
        ChatManager.Instance().getUploadUrl(new File(filePath).getName(), MessageContentMediaType.FILE, null, new GetUploadUrlCallback() {
            @Override
            public void onSuccess(String uploadUrl, String remoteUrl, String backUploadupUrl, int serverType) {
                if(serverType == 1) {
                    String[] ss = uploadUrl.split("\\?");
                    uploadQiniu(ss[0], ss[1], ss[2], filePath, remoteUrl);
                    return;
                }
                MediaType type=MediaType.parse("application/octet-stream");
                File file = new File(filePath);
                RequestBody fileBody = new UploadFileRequestBody(RequestBody.create(type,file), (progress)->{
                    runOnUiThread(()->updateProgress(progress));
                });


                Request request = new Request.Builder().url(uploadUrl).put(fileBody).build();
                mCall = okHttpClient.newCall(request);
                mCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(()-> updateState(4));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if(response.code() != 200) {
                            runOnUiThread(()-> updateState(4));
                        } else {
                            UploadBigFileActivity.this.remoteUrl = remoteUrl;
                            runOnUiThread(()-> updateState(2));
                        }
                    }
                });
            }

            @Override
            public void onFail(int errorCode) {
                runOnUiThread(()-> updateState(4));
            }
        });
    }
    private void uploadQiniu(String url, String token, String key, String filePath, String remoteUrl) {
        File file = new File(filePath);
        MediaType type=MediaType.parse("application/octet-stream");
        RequestBody fileBody = new UploadFileRequestBody(RequestBody.create(type,file), (progress)->{
            runOnUiThread(()->updateProgress(progress));
        });

        final MultipartBody.Builder mb = new MultipartBody.Builder();
        mb.addFormDataPart("key", key);
        mb.addFormDataPart("token", token);
        mb.addFormDataPart("file", "fileName", fileBody);
        mb.setType(MediaType.parse("multipart/form-data"));
        RequestBody body = mb.build();
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);

        mCall = okHttpClient.newCall(requestBuilder.build());

        mCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(()-> updateState(4));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.code() != 200) {
                    runOnUiThread(()-> updateState(4));
                } else {
                    UploadBigFileActivity.this.remoteUrl = remoteUrl;

                    runOnUiThread(()-> updateState(2));
                }
            }
        });
    }
    @Override
    public void onClick(View view) {
        if(state == 0 || state == 3 || state == 4) {
            updateState(1);
            uploadFile();
        } else if(state == 1) {
            if(!mCall.isCanceled()) {
                mCall.cancel();
            }
            updateState(3);
        } else if(state == 2) {
            FileMessageContent content = new FileMessageContent(filePath);
            content.remoteUrl = remoteUrl;
            ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
            updateState(5);
        }
    }
}
