/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.pan.api.PanService;
import cn.wildfire.chat.kit.pan.model.CreateFileRequest;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfirechat.message.FileMessageContent;

/**
 * 保存到网盘页面
 */
public class PanSaveActivity extends WfcBaseActivity {
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_save;
    }
    
    private static final String EXTRA_FILE_CONTENT = "fileContent";
    
    private RecyclerView recyclerView;
    private PanSaveSpaceAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    
    private PanService panService;
    private FileMessageContent fileContent;
    
    /**
     * 启动保存到网盘
     */
    public static void start(Activity activity, FileMessageContent content) {
        Intent intent = new Intent(activity, PanSaveActivity.class);
        intent.putExtra(EXTRA_FILE_CONTENT, content);
        activity.startActivity(intent);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        fileContent = getIntent().getParcelableExtra(EXTRA_FILE_CONTENT);
        if (fileContent == null) {
            Toast.makeText(this, "文件信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initView();
        initData();
    }
    
    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.pan_save_to);
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanSaveSpaceAdapter();
        adapter.setOnSpaceClickListener(space -> {
            saveToSpace(space);
        });
        recyclerView.setAdapter(adapter);
    }
    
    private void initData() {
        panService = getPanService();
        if (panService == null) {
            Toast.makeText(this, R.string.pan_service_not_configured, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        loadSpaces();
    }
    
    private void loadSpaces() {
        showLoading(true);
        
        panService.getSpaces(new PanService.Callback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (data == null || data.isEmpty()) {
                        showEmpty(true);
                        return;
                    }
                    
                    // 只筛选出我的两个空间（公共+私有），排除全局公共空间
                    String currentUserId = cn.wildfirechat.remote.ChatManager.Instance().getUserId();
                    List<PanSpace> mySpaces = new ArrayList<>();
                    
                    for (PanSpace space : data) {
                        // 只保留用户自己的空间（公共和私有）
                        if ((space.getSpaceTypeEnum() == PanSpace.SpaceType.USER_PUBLIC || 
                             space.getSpaceTypeEnum() == PanSpace.SpaceType.USER_PRIVATE) &&
                            currentUserId != null && currentUserId.equals(space.getOwnerId())) {
                            mySpaces.add(space);
                        }
                    }
                    
                    if (mySpaces.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setSpaces(mySpaces);
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanSaveActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    private void saveToSpace(PanSpace space) {
        // 检查文件URL是否有效
        if (fileContent.remoteUrl == null || fileContent.remoteUrl.isEmpty()) {
            Toast.makeText(this, "文件链接无效，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showLoading(true);
        
        CreateFileRequest request = new CreateFileRequest();
        request.setSpaceId(space.getId());
        request.setParentId(null); // 保存到根目录
        request.setName(fileContent.getName());
        request.setSize((long) fileContent.getSize());
        request.setMimeType(getMimeTypeFromFileName(fileContent.getName()));
        // MD5 为空，服务器端会处理
        request.setMd5("");
        request.setStorageUrl(fileContent.remoteUrl);
        request.setCopy(true); // 从文件消息保存，需要复制到Pan bucket
        
        panService.createFile(request, new PanService.Callback<cn.wildfire.chat.kit.pan.model.PanFile>() {
            @Override
            public void onSuccess(cn.wildfire.chat.kit.pan.model.PanFile data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanSaveActivity.this, R.string.pan_save_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanSaveActivity.this, 
                        getString(R.string.pan_save_failed) + ": " + message, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private String getMimeTypeFromFileName(String fileName) {
        if (fileName == null) return "application/octet-stream";
        
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
            return "application/vnd.ms-powerpoint";
        } else if (lowerName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerName.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerName.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        return "application/octet-stream";
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showEmpty(boolean show) {
        emptyTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    protected PanService getPanService() {
        try {
            Class<?> clazz = Class.forName("cn.wildfire.chat.app.pan.PanServiceProvider");
            return (PanService) clazz.getMethod("getPanService").invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
