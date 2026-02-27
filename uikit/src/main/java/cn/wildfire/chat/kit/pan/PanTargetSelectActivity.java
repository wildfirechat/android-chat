/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 选择目标位置页面（用于移动/复制）
 * 重构后：底部显示粘贴和取消按钮，点击空间和目录是进入
 */
public class PanTargetSelectActivity extends WfcBaseActivity {
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_target_select;
    }
    
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_COPY = "copy";
    
    private static final String EXTRA_SPACE = "space";
    private static final String EXTRA_FILE = "file";
    private static final String EXTRA_ACTION = "action";
    
    private RecyclerView recyclerView;
    private PanTargetSelectAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private Button pasteButton;
    private Button cancelButton;
    
    private PanService panService;
    private PanSpace sourceSpace;      // 源空间
    private PanFile sourceFile;        // 源文件
    private String action;             // move 或 copy
    
    // 当前导航状态
    private List<Object> breadcrumbStack = new ArrayList<>();  // 导航栈（PanSpace或PanFile）
    private PanSpace currentSpace;     // 当前空间
    private Long currentParentId;      // 当前父目录ID
    
    /**
     * 启动目标选择
     */
    public static void startForResult(Activity activity, PanSpace currentSpace, PanFile file, String action) {
        Intent intent = new Intent(activity, PanTargetSelectActivity.class);
        intent.putExtra(EXTRA_SPACE, currentSpace);
        intent.putExtra(EXTRA_FILE, file);
        intent.putExtra(EXTRA_ACTION, action);
        activity.startActivityForResult(intent, 1001);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sourceSpace = getIntent().getParcelableExtra(EXTRA_SPACE);
        sourceFile = getIntent().getParcelableExtra(EXTRA_FILE);
        action = getIntent().getStringExtra(EXTRA_ACTION);
        
        if (sourceSpace == null || sourceFile == null || action == null) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
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
        updateTitle();
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        pasteButton = findViewById(R.id.pasteButton);
        cancelButton = findViewById(R.id.cancelButton);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanTargetSelectAdapter();
        adapter.setOnItemClickListener(new PanTargetSelectAdapter.OnItemClickListener() {
            @Override
            public void onSpaceClick(PanSpace space) {
                // 进入空间
                enterSpace(space);
            }
            
            @Override
            public void onFolderClick(PanFile folder) {
                // 进入文件夹
                enterFolder(folder);
            }
        });
        recyclerView.setAdapter(adapter);
        
        // 粘贴按钮
        pasteButton.setOnClickListener(v -> doPaste());
        
        // 取消按钮
        cancelButton.setOnClickListener(v -> cancelSelection());
        
        updatePasteButton();
    }
    
    /**
     * 更新标题
     */
    private void updateTitle() {
        if (currentSpace == null) {
            // 空间列表页面
            getSupportActionBar().setTitle(ACTION_MOVE.equals(action) ? R.string.pan_move_to : R.string.pan_copy_to);
        } else {
            // 文件列表页面
            if (!breadcrumbStack.isEmpty() && breadcrumbStack.get(breadcrumbStack.size() - 1) instanceof PanFile) {
                // 在文件夹内
                PanFile folder = (PanFile) breadcrumbStack.get(breadcrumbStack.size() - 1);
                getSupportActionBar().setTitle(folder.getName());
            } else {
                // 在空间根目录
                getSupportActionBar().setTitle(currentSpace.getDisplayName());
            }
        }
    }
    
    /**
     * 更新粘贴按钮状态
     */
    private void updatePasteButton() {
        // 检查是否是原位置（同一空间且同一父目录）
        boolean isSameLocation = false;
        if (currentSpace != null) {
            Long sourceParentId = sourceFile.getParentId();
            boolean sameSpace = currentSpace.getId().equals(sourceSpace.getId());
            boolean sameParent = (currentParentId == null && sourceParentId == null) ||
                                 (currentParentId != null && currentParentId.equals(sourceParentId));
            isSameLocation = sameSpace && sameParent;
        }
        
        // 在原位置时禁用粘贴按钮
        pasteButton.setEnabled(!isSameLocation);
        if (isSameLocation) {
            pasteButton.setAlpha(0.5f);
        } else {
            pasteButton.setAlpha(1.0f);
        }
    }
    
    private void initData() {
        panService = getPanService();
        if (panService == null) {
            Toast.makeText(this, R.string.pan_service_not_configured, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始加载空间列表
        loadSpaces();
    }
    
    /**
     * 加载空间列表（只显示自己的空间和全局空间）
     */
    private void loadSpaces() {
        showLoading(true);
        currentSpace = null;
        currentParentId = null;
        breadcrumbStack.clear();
        
        panService.getSpaces(new PanService.Callback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // 只显示全局公共空间和自己的空间
                    String currentUserId = cn.wildfirechat.remote.ChatManager.Instance().getUserId();
                    List<PanSpace> filteredSpaces = new ArrayList<>();
                    
                    for (PanSpace s : data) {
                        if (s.getSpaceTypeEnum() == PanSpace.SpaceType.GLOBAL_PUBLIC) {
                            // 全局公共空间
                            filteredSpaces.add(s);
                        } else if ((s.getSpaceTypeEnum() == PanSpace.SpaceType.USER_PUBLIC || 
                                    s.getSpaceTypeEnum() == PanSpace.SpaceType.USER_PRIVATE) &&
                                   currentUserId != null && currentUserId.equals(s.getOwnerId())) {
                            // 自己的空间
                            filteredSpaces.add(s);
                        }
                    }
                    
                    if (filteredSpaces.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setSpaces(filteredSpaces);
                    }
                    
                    updateTitle();
                    updatePasteButton();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanTargetSelectActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
    
    /**
     * 进入空间
     */
    private void enterSpace(PanSpace space) {
        currentSpace = space;
        breadcrumbStack.add(space);
        loadFolders(space.getId(), null);
        updateTitle();
        updatePasteButton();
    }
    
    /**
     * 进入文件夹
     */
    private void enterFolder(PanFile folder) {
        breadcrumbStack.add(folder);
        loadFolders(currentSpace.getId(), folder.getId());
        updateTitle();
        updatePasteButton();
    }
    
    /**
     * 加载文件列表（显示所有内容，包括文件夹和文件）
     */
    private void loadFolders(Long spaceId, Long parentId) {
        showLoading(true);
        currentParentId = parentId;
        
        panService.getFiles(spaceId, parentId, new PanService.Callback<List<PanFile>>() {
            @Override
            public void onSuccess(List<PanFile> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    // 显示所有内容（文件夹和文件）
                    List<PanFile> files = data != null ? data : new ArrayList<>();
                    
                    if (files.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setFiles(files, parentId);
                    }
                    
                    updatePasteButton();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanTargetSelectActivity.this, message, Toast.LENGTH_SHORT).show();
                    // 返回到上级
                    goBack();
                });
            }
        });
    }
    
    /**
     * 返回上一级
     */
    private void goBack() {
        if (breadcrumbStack.isEmpty()) {
            finish();
            return;
        }
        
        breadcrumbStack.remove(breadcrumbStack.size() - 1);
        
        if (breadcrumbStack.isEmpty()) {
            // 返回到空间列表
            loadSpaces();
        } else {
            // 返回到上级
            Object parent = breadcrumbStack.get(breadcrumbStack.size() - 1);
            if (parent instanceof PanSpace) {
                // 在空间根目录
                currentSpace = (PanSpace) parent;
                loadFolders(currentSpace.getId(), null);
            } else if (parent instanceof PanFile) {
                // 在文件夹内
                PanFile parentFolder = (PanFile) parent;
                loadFolders(currentSpace.getId(), parentFolder.getId());
            }
        }
        
        updateTitle();
        updatePasteButton();
    }
    
    /**
     * 执行粘贴操作
     */
    private void doPaste() {
        if (currentSpace == null) {
            Toast.makeText(this, "请先选择一个空间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否是同一位置
        Long sourceParentId = sourceFile.getParentId();
        boolean sameSpace = currentSpace.getId().equals(sourceSpace.getId());
        boolean sameParent = (currentParentId == null && sourceParentId == null) ||
                             (currentParentId != null && currentParentId.equals(sourceParentId));
        
        if (sameSpace && sameParent) {
            Toast.makeText(this, R.string.pan_same_location, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ACTION_MOVE.equals(action)) {
            doMove();
        } else {
            doCopy();
        }
    }
    
    /**
     * 执行移动
     */
    private void doMove() {
        showLoading(true);
        panService.moveFile(sourceFile.getId(), currentSpace.getId(), currentParentId, 
            new PanService.Callback<PanFile>() {
                @Override
                public void onSuccess(PanFile data) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(PanTargetSelectActivity.this, R.string.pan_move_success, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onError(int errorCode, String message) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(PanTargetSelectActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
    
    /**
     * 执行复制
     */
    private void doCopy() {
        showLoading(true);
        // 跨空间复制时copy=true，同空间复制时copy=false
        boolean needCopy = !currentSpace.getId().equals(sourceSpace.getId());
        panService.copyFile(sourceFile.getId(), currentSpace.getId(), currentParentId, needCopy,
            new PanService.Callback<PanFile>() {
                @Override
                public void onSuccess(PanFile data) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(PanTargetSelectActivity.this, R.string.pan_copy_success, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
                
                @Override
                public void onError(int errorCode, String message) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(PanTargetSelectActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
    
    /**
     * 取消选择
     */
    private void cancelSelection() {
        setResult(RESULT_CANCELED);
        finish();
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
            // 返回上一级
            if (currentSpace != null) {
                goBack();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        // 返回键也走返回上一级逻辑
        if (currentSpace != null) {
            goBack();
        } else {
            super.onBackPressed();
        }
    }
}
