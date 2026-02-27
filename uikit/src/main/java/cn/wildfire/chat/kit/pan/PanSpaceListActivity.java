/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.content.Context;
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
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 网盘空间列表页面
 */
public class PanSpaceListActivity extends WfcBaseActivity {
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_space_list;
    }
    
    private static final String EXTRA_TARGET_USER_ID = "targetUserId";
    private static final String EXTRA_TARGET_USER_NAME = "targetUserName";
    
    private RecyclerView recyclerView;
    private PanSpaceListAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    
    private PanService panService;
    private String targetUserId;
    private String targetUserName;
    
    /**
     * 打开自己的网盘
     */
    public static void start(Context context) {
        Intent intent = new Intent(context, PanSpaceListActivity.class);
        context.startActivity(intent);
    }
    
    /**
     * 查看好友的公共空间
     * @param context 上下文
     * @param userId 好友用户ID
     * @param userName 好友昵称（用于显示标题）
     */
    public static void startForUserPublicSpace(Context context, String userId, String userName) {
        Intent intent = new Intent(context, PanSpaceListActivity.class);
        intent.putExtra(EXTRA_TARGET_USER_ID, userId);
        intent.putExtra(EXTRA_TARGET_USER_NAME, userName);
        context.startActivity(intent);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 读取参数
        targetUserId = getIntent().getStringExtra(EXTRA_TARGET_USER_ID);
        targetUserName = getIntent().getStringExtra(EXTRA_TARGET_USER_NAME);
        
        initView();
        initData();
    }
    
    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // 根据是否有 targetUserId 设置标题
        if (targetUserId != null) {
            // 查看其他用户的公共空间
            if (targetUserName != null && !targetUserName.isEmpty()) {
                getSupportActionBar().setTitle(targetUserName + "的公共空间");
            } else {
                getSupportActionBar().setTitle("他/她的公共空间");
            }
        } else {
            // 查看自己的网盘
            getSupportActionBar().setTitle(R.string.pan_title);
        }
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanSpaceListAdapter();
        adapter.setOnSpaceClickListener(new PanSpaceListAdapter.OnSpaceClickListener() {
            @Override
            public void onSpaceClick(PanSpace space, String displayName) {
                // 进入文件列表，传入自定义显示名称
                PanFileListActivity.start(PanSpaceListActivity.this, space, displayName);
            }
        });
        // 如果是查看其他用户的空间，设置自定义名称
        if (targetUserId != null && targetUserName != null) {
            adapter.setUserPublicSpaceName(targetUserName + "的公共空间");
        }
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
        
        if (targetUserId != null) {
            // 查看指定用户的公共空间
            loadUserPublicSpace(targetUserId);
            return;
        }
        
        panService.getSpaces(new PanService.Callback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (data == null || data.isEmpty()) {
                        showEmpty(true);
                    } else {
                        // 过滤空间：只显示全局公共空间和自己的空间
                        String currentUserId = cn.wildfirechat.remote.ChatManager.Instance().getUserId();
                        List<PanSpace> filteredSpaces = new ArrayList<>();
                        
                        for (PanSpace space : data) {
                            PanSpace.SpaceType spaceType = space.getSpaceTypeEnum();
                            if (spaceType == PanSpace.SpaceType.GLOBAL_PUBLIC) {
                                // 全局公共空间 - 显示
                                filteredSpaces.add(space);
                            } else if (spaceType == PanSpace.SpaceType.USER_PUBLIC || 
                                       spaceType == PanSpace.SpaceType.USER_PRIVATE) {
                                // 用户空间 - 只显示自己的
                                if (currentUserId != null && currentUserId.equals(space.getOwnerId())) {
                                    filteredSpaces.add(space);
                                }
                            }
                        }
                        
                        if (filteredSpaces.isEmpty()) {
                            showEmpty(true);
                        } else {
                            showEmpty(false);
                            adapter.setSpaces(filteredSpaces);
                        }
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    android.util.Log.e("PanSpaceListActivity", "loadSpaces error: " + errorCode + ", " + message);
                    Toast.makeText(PanSpaceListActivity.this, 
                        getString(R.string.pan_load_failed) + ": " + message, 
                        Toast.LENGTH_LONG).show();
                    // 显示错误但不退出，让用户可以重试
                    showEmpty(true);
                    emptyTextView.setText(getString(R.string.pan_load_failed) + "\n" + message);
                });
            }
        });
    }
    
    private void loadUserPublicSpace(String userId) {
        android.util.Log.d("PanSpaceListActivity", "Loading user public space for: " + userId);
        panService.getUserPublicSpace(userId, new PanService.Callback<PanSpace>() {
            @Override
            public void onSuccess(PanSpace space) {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (space == null) {
                        showEmpty(true);
                        emptyTextView.setText("该用户没有公共空间");
                    } else {
                        showEmpty(false);
                        List<PanSpace> spaces = new ArrayList<>();
                        spaces.add(space);
                        adapter.setSpaces(spaces);
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    android.util.Log.e("PanSpaceListActivity", "loadUserPublicSpace error: " + errorCode + ", " + message);
                    Toast.makeText(PanSpaceListActivity.this, 
                        getString(R.string.pan_load_failed) + ": " + message, 
                        Toast.LENGTH_LONG).show();
                    showEmpty(true);
                    emptyTextView.setText(getString(R.string.pan_load_failed) + "\n" + message);
                });
            }
        });
    }
    
    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
    
    private void showEmpty(boolean show) {
        emptyTextView.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    /**
     * 获取网盘服务，子类可重写
     */
    protected PanService getPanService() {
        // 通过AppServiceProvider获取
        try {
            Class<?> clazz = Class.forName("cn.wildfire.chat.app.pan.PanServiceProvider");
            android.util.Log.d("PanSpaceListActivity", "Found PanServiceProvider class");
            PanService service = (PanService) clazz.getMethod("getPanService").invoke(null);
            android.util.Log.d("PanSpaceListActivity", "Got PanService: " + (service != null ? "not null" : "null"));
            return service;
        } catch (Exception e) {
            android.util.Log.e("PanSpaceListActivity", "Failed to get PanService", e);
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
