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
 * 网盘文件选择器（用于从网盘选择文件发送）
 * 参考 iOS WFCUPanFilePickerViewController
 * 流程：先显示3个空间 -> 点击空间进入文件列表 -> 选择文件
 */
public class PanFilePickerActivity extends WfcBaseActivity {
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_file_picker;
    }
    
    private RecyclerView recyclerView;
    private PanFilePickerAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private Button confirmButton;
    
    private PanService panService;
    private List<PanSpace> spaces = new ArrayList<>();
    private List<PanFile> files = new ArrayList<>();
    private List<PanFile> selectedFiles = new ArrayList<>();
    
    private PanSpace currentSpace;  // 当前选中的空间
    private Long currentParentId = null;  // 当前文件夹ID
    private List<Object> breadcrumbStack = new ArrayList<>(); // 导航栈（PanSpace或PanFile）
    
    public static void startForResult(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, PanFilePickerActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initView();
        initData();
    }
    
    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        updateNavigationBar();
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        confirmButton = findViewById(R.id.confirmButton);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanFilePickerAdapter();
        adapter.setOnItemClickListener(new PanFilePickerAdapter.OnItemClickListener() {
            @Override
            public void onSpaceClick(PanSpace space) {
                // 进入空间
                enterSpace(space);
            }
            
            @Override
            public void onFileClick(PanFile file) {
                if (file.isFolder()) {
                    // 进入文件夹
                    enterFolder(file);
                } else {
                    // 选择/取消选择文件
                    toggleFileSelection(file);
                }
            }
            
            @Override
            public void onFileCheckChanged(PanFile file, boolean isChecked) {
                if (isChecked) {
                    if (!isFileSelected(file)) {
                        selectedFiles.add(file);
                    }
                } else {
                    selectedFiles.removeIf(f -> f.getId().equals(file.getId()));
                }
                updateConfirmButton();
            }
        });
        recyclerView.setAdapter(adapter);
        
        confirmButton.setOnClickListener(v -> confirmSelection());
        updateConfirmButton();
    }
    
    /**
     * 更新导航栏
     */
    private void updateNavigationBar() {
        if (currentSpace == null) {
            // 空间列表页面
            getSupportActionBar().setTitle(R.string.pan_title);
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
    
    private void initData() {
        panService = getPanService();
        if (panService == null) {
            Toast.makeText(this, R.string.pan_service_not_configured, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 初始加载空间列表
        loadMySpaces();
    }
    
    /**
     * 加载我的空间列表（3个空间）
     */
    private void loadMySpaces() {
        showLoading(true);
        currentSpace = null;
        currentParentId = null;
        breadcrumbStack.clear();
        
        panService.getSpaces(new PanService.Callback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    
                    // 筛选只显示公共空间和个人空间（和iOS一致）
                    String currentUserId = cn.wildfirechat.remote.ChatManager.Instance().getUserId();
                    List<PanSpace> publicSpaces = new ArrayList<>();  // 公共空间
                    List<PanSpace> mySpaces = new ArrayList<>();      // 我的空间
                    
                    for (PanSpace space : data) {
                        PanSpace.SpaceType spaceType = space.getSpaceTypeEnum();
                        // 公共空间（全局公共空间）
                        if (spaceType == PanSpace.SpaceType.GLOBAL_PUBLIC) {
                            publicSpaces.add(space);
                        }
                        // 我的空间（公共+私有）
                        else if ((spaceType == PanSpace.SpaceType.USER_PUBLIC || 
                                  spaceType == PanSpace.SpaceType.USER_PRIVATE) && 
                                  currentUserId != null && currentUserId.equals(space.getOwnerId())) {
                            mySpaces.add(space);
                        }
                    }
                    
                    // 公共空间排在最上面，然后是我的空间（和iOS一致）
                    spaces = new ArrayList<>();
                    spaces.addAll(publicSpaces);
                    spaces.addAll(mySpaces);
                    
                    files.clear();
                    
                    if (spaces.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setSpaces(spaces);
                    }
                    
                    updateNavigationBar();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanFilePickerActivity.this, message, Toast.LENGTH_SHORT).show();
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
        loadFilesInSpace(space, null);
        updateNavigationBar();
    }
    
    /**
     * 进入文件夹
     */
    private void enterFolder(PanFile folder) {
        breadcrumbStack.add(folder);
        loadFilesInSpace(currentSpace, folder.getId());
        updateNavigationBar();
    }
    
    /**
     * 加载空间内的文件
     */
    private void loadFilesInSpace(PanSpace space, Long parentId) {
        showLoading(true);
        currentParentId = parentId;
        
        panService.getFiles(space.getId(), parentId, new PanService.Callback<List<PanFile>>() {
            @Override
            public void onSuccess(List<PanFile> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    files = data != null ? data : new ArrayList<>();
                    
                    if (files.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setFiles(files, selectedFiles);
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanFilePickerActivity.this, message, Toast.LENGTH_SHORT).show();
                    // 返回上级
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
            currentSpace = null;
            files.clear();
            loadMySpaces();
        } else {
            // 返回到上级
            Object parent = breadcrumbStack.get(breadcrumbStack.size() - 1);
            if (parent instanceof PanSpace) {
                // 在空间根目录
                currentSpace = (PanSpace) parent;
                loadFilesInSpace(currentSpace, null);
            } else if (parent instanceof PanFile) {
                // 在文件夹内
                PanFile parentFolder = (PanFile) parent;
                loadFilesInSpace(currentSpace, parentFolder.getId());
            }
        }
        updateNavigationBar();
    }
    
    private boolean isFileSelected(PanFile file) {
        for (PanFile selected : selectedFiles) {
            if (selected.getId().equals(file.getId())) {
                return true;
            }
        }
        return false;
    }
    
    private void toggleFileSelection(PanFile file) {
        boolean isSelected = isFileSelected(file);
        if (isSelected) {
            selectedFiles.removeIf(f -> f.getId().equals(file.getId()));
        } else {
            // 最多选择9个文件（与iOS一致）
            if (selectedFiles.size() >= 9) {
                Toast.makeText(this, "最多只能选择9个文件", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedFiles.add(file);
        }
        adapter.updateSelection(selectedFiles);
        updateConfirmButton();
    }
    
    private void updateConfirmButton() {
        confirmButton.setEnabled(!selectedFiles.isEmpty());
        confirmButton.setText(getString(R.string.pan_select_file_confirm, selectedFiles.size()));
    }
    
    private void confirmSelection() {
        Intent result = new Intent();
        result.putParcelableArrayListExtra("selectedFiles", new ArrayList<>(selectedFiles));
        setResult(RESULT_OK, result);
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
            if (currentSpace != null) {
                // 在文件列表页面，返回上一级
                goBack();
                return true;
            }
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
