/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.pan.api.PanService;
import cn.wildfire.chat.kit.pan.model.CreateFileRequest;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.remote.ChatManager;

/**
 * 网盘文件列表页面
 */
public class PanFileListActivity extends WfcBaseActivity {
    
    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_file_list;
    }
    
    private static final String EXTRA_SPACE = "space";
    private static final String EXTRA_PARENT_ID = "parentId";
    private static final String EXTRA_FOLDER_NAME = "folderName";
    private static final String EXTRA_SPACE_DISPLAY_NAME = "spaceDisplayName";
    private static final int REQUEST_CODE_FILE_PICKER = 1001;
    
    private RecyclerView recyclerView;
    private PanFileListAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    
    private PanService panService;
    private PanSpace space;
    private Long parentId;
    private String folderName;
    private String spaceDisplayName;  // 自定义空间显示名称
    private List<PanFile> currentFiles = new ArrayList<>();
    private boolean hasWritePermission = false;
    
    /**
     * 启动文件列表（根目录）
     */
    public static void start(Context context, PanSpace space) {
        start(context, space, null, null, null);
    }
    
    /**
     * 启动文件列表（根目录，带自定义显示名称）
     */
    public static void start(Context context, PanSpace space, String spaceDisplayName) {
        start(context, space, null, null, spaceDisplayName);
    }
    
    /**
     * 启动文件列表（子目录）
     */
    public static void start(Context context, PanSpace space, Long parentId, String folderName) {
        start(context, space, parentId, folderName, null);
    }
    
    /**
     * 启动文件列表（完整参数）
     */
    public static void start(Context context, PanSpace space, Long parentId, String folderName, String spaceDisplayName) {
        Intent intent = new Intent(context, PanFileListActivity.class);
        intent.putExtra(EXTRA_SPACE, space);
        if (parentId != null) {
            intent.putExtra(EXTRA_PARENT_ID, parentId);
        }
        if (folderName != null) {
            intent.putExtra(EXTRA_FOLDER_NAME, folderName);
        }
        if (spaceDisplayName != null) {
            intent.putExtra(EXTRA_SPACE_DISPLAY_NAME, spaceDisplayName);
        }
        context.startActivity(intent);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        space = getIntent().getParcelableExtra(EXTRA_SPACE);
        parentId = getIntent().getLongExtra(EXTRA_PARENT_ID, 0);
        if (parentId == 0) parentId = null;
        folderName = getIntent().getStringExtra(EXTRA_FOLDER_NAME);
        // 获取自定义空间显示名称（用于查看他人空间时）
        spaceDisplayName = getIntent().getStringExtra(EXTRA_SPACE_DISPLAY_NAME);
        
        if (space == null) {
            Toast.makeText(this, "空间信息错误", Toast.LENGTH_SHORT).show();
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
        // 设置标题：优先使用文件夹名，其次是自定义空间名，最后是空间默认显示名
        if (folderName != null) {
            getSupportActionBar().setTitle(folderName);
        } else if (spaceDisplayName != null) {
            getSupportActionBar().setTitle(spaceDisplayName);
        } else {
            getSupportActionBar().setTitle(space.getDisplayName());
        }
        
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanFileListAdapter();
        adapter.setOnFileClickListener(new PanFileListAdapter.OnFileClickListener() {
            @Override
            public void onFileClick(PanFile file) {
                if (file.isFolder()) {
                    // 进入子目录
                    PanFileListActivity.start(PanFileListActivity.this, space, file.getId(), file.getName());
                } else {
                    // 预览/下载文件
                    previewFile(file);
                }
            }
            
            @Override
            public void onFileLongClick(PanFile file) {
                showFileOptions(file);
            }
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
        
        // 检查写入权限
        checkWritePermission();
        loadFiles();
    }
    
    private void checkWritePermission() {
        panService.checkSpaceWritePermission(space.getId(), new PanService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean hasPermission) {
                hasWritePermission = hasPermission != null && hasPermission;
            }
            
            @Override
            public void onError(int errorCode, String message) {
                hasWritePermission = false;
            }
        });
    }
    
    private void loadFiles() {
        showLoading(true);
        
        panService.getFiles(space.getId(), parentId, new PanService.Callback<List<PanFile>>() {
            @Override
            public void onSuccess(List<PanFile> data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentFiles = data != null ? data : new ArrayList<>();
                    if (currentFiles.isEmpty()) {
                        showEmpty(true);
                    } else {
                        showEmpty(false);
                        adapter.setFiles(currentFiles);
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(PanFileListActivity.this, 
                        getString(R.string.pan_load_failed) + ": " + message, 
                        Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                });
            }
        });
    }
    
    private void previewFile(PanFile file) {
        // 获取文件下载URL并打开
        panService.getFileUrl(file.getId(), new PanService.Callback<String>() {
            @Override
            public void onSuccess(String url) {
                runOnUiThread(() -> {
                    if (url != null && !url.isEmpty()) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(PanFileListActivity.this, 
                                "无法打开文件", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        "获取文件链接失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showFileOptions(PanFile file) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        
        if (hasWritePermission) {
            // 有写权限：显示完整菜单
            
            // 分享 - 只有文件可以分享
            if (!file.isFolder()) {
                options.add(getString(R.string.pan_share));
                actions.add(() -> shareFile(file));
            }
            
            // 重命名
            options.add(getString(R.string.pan_rename));
            actions.add(() -> renameFile(file));
            
            // 移动
            options.add(getString(R.string.pan_move));
            actions.add(() -> moveFile(file));
            
            // 复制
            options.add(getString(R.string.pan_copy));
            actions.add(() -> copyFile(file));
            
            // 删除
            options.add(getString(R.string.pan_delete));
            actions.add(() -> deleteFile(file));
            
        } else {
            // 无写权限：只显示转存
            options.add(getString(R.string.pan_duplicate));
            actions.add(() -> duplicateFile(file));
        }
        
        String[] optionsArray = options.toArray(new String[0]);
        new AlertDialog.Builder(this)
            .setTitle(file.getName())
            .setItems(optionsArray, (dialog, which) -> {
                if (which >= 0 && which < actions.size()) {
                    actions.get(which).run();
                }
            })
            .show();
    }
    
    /**
     * 分享文件到聊天
     */
    private void shareFile(PanFile file) {
        // 创建 FileMessageContent
        FileMessageContent content = new FileMessageContent();
        content.setName(file.getName());
        content.setSize(file.getSize() != null ? file.getSize().intValue() : 0);
        content.remoteUrl = file.getStorageUrl();
        
        // 创建 Message 对象
        cn.wildfirechat.message.Message message = new cn.wildfirechat.message.Message();
        message.content = content;
        
        // 跳转到转发页面
        try {
            Class<?> forwardClass = Class.forName("cn.wildfire.chat.kit.conversation.forward.ForwardActivity");
            Intent intent = new Intent(this, forwardClass);
            intent.putExtra("message", message);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "转发功能不可用", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 转存文件到自己的空间（像文件消息保存到网盘一样处理）
     */
    private void duplicateFile(PanFile file) {
        // 创建 FileMessageContent，像文件消息保存一样处理
        FileMessageContent content = new FileMessageContent();
        content.setName(file.getName());
        content.setSize(file.getSize() != null ? file.getSize().intValue() : 0);
        content.remoteUrl = file.getStorageUrl();
        
        // 跳转到保存到网盘页面
        PanSaveActivity.start(this, content);
    }
    
    private void renameFile(PanFile file) {
        new MaterialDialog.Builder(this)
            .title(R.string.pan_rename)
            .input(getString(R.string.pan_new_name_hint), file.getName(), false, (dialog, input) -> {
                String newName = input.toString().trim();
                if (!newName.isEmpty()) {
                    doRenameFile(file, newName);
                }
            })
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .show();
    }
    
    private void doRenameFile(PanFile file, String newName) {
        panService.renameFile(file.getId(), newName, new PanService.Callback<PanFile>() {
            @Override
            public void onSuccess(PanFile data) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, R.string.pan_rename_success, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        getString(R.string.pan_rename_failed) + ": " + message, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void moveFile(PanFile file) {
        // 打开目标选择页面
        PanTargetSelectActivity.startForResult(this, space, file, PanTargetSelectActivity.ACTION_MOVE);
    }
    
    private void copyFile(PanFile file) {
        // 打开目标选择页面
        PanTargetSelectActivity.startForResult(this, space, file, PanTargetSelectActivity.ACTION_COPY);
    }
    
    private void deleteFile(PanFile file) {
        String message = getString(file.isFolder() ? R.string.pan_delete_folder_confirm : R.string.pan_delete_file_confirm);
        new AlertDialog.Builder(this)
            .setTitle(R.string.pan_delete)
            .setMessage(message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                doDeleteFile(file);
            })
            .show();
    }
    
    private void doDeleteFile(PanFile file) {
        panService.deleteFile(file.getId(), new PanService.SimpleCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, R.string.pan_delete_success, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        getString(R.string.pan_delete_failed) + ": " + message, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void createFolder() {
        new MaterialDialog.Builder(this)
            .title(R.string.pan_create_folder)
            .input(getString(R.string.pan_folder_name_hint), "", false, (dialog, input) -> {
                String name = input.toString().trim();
                if (!name.isEmpty()) {
                    doCreateFolder(name);
                }
            })
            .negativeText(R.string.cancel)
            .positiveText(R.string.confirm)
            .show();
    }
    
    private void doCreateFolder(String name) {
        panService.createFolder(space.getId(), parentId, name, new PanService.Callback<PanFile>() {
            @Override
            public void onSuccess(PanFile data) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, R.string.pan_create_folder_success, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        getString(R.string.pan_create_folder_failed) + ": " + message, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 上传文件
     */
    private void uploadFile() {
        // 检查上传权限
        panService.checkUploadPermission(space.getId(), new PanService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean canUpload) {
                runOnUiThread(() -> {
                    if (canUpload != null && canUpload) {
                        showFilePicker();
                    } else {
                        Toast.makeText(PanFileListActivity.this, 
                            R.string.pan_no_permission, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        R.string.pan_check_permission_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 显示文件选择器
     */
    private void showFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.pan_select_file)), 
                REQUEST_CODE_FILE_PICKER);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开文件选择器", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_FILE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                uploadSelectedFile(uri);
            }
        }
    }
    
    /**
     * 上传选中的文件
     */
    private void uploadSelectedFile(Uri fileUri) {
        // 显示进度对话框
        MaterialDialog progressDialog = new MaterialDialog.Builder(this)
            .title(R.string.pan_uploading)
            .content(R.string.pan_upload_progress)
            .progress(false, 100)
            .cancelable(false)
            .show();
        
        // 获取文件路径
        String filePath = getRealPathFromUri(fileUri);
        if (filePath == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "无法获取文件路径", Toast.LENGTH_SHORT).show();
            return;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            progressDialog.dismiss();
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 使用上传管理器上传
        PanUploadManager.getInstance().uploadFile(filePath, 
            progress -> runOnUiThread(() -> progressDialog.setProgress(progress)),
            (remoteUrl, size, md5) -> runOnUiThread(() -> {
                progressDialog.dismiss();
                // 上传成功，创建文件记录
                createFileRecord(file.getName(), size, md5, remoteUrl);
            }),
            errorMessage -> runOnUiThread(() -> {
                progressDialog.dismiss();
                Toast.makeText(PanFileListActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            })
        );
    }
    
    /**
     * 创建文件记录
     */
    private void createFileRecord(String name, long size, String md5, String storageUrl) {
        CreateFileRequest request = new CreateFileRequest();
        request.setSpaceId(space.getId());
        request.setParentId(parentId);
        request.setName(name);
        request.setSize(size);
        request.setMimeType(PanUploadManager.getInstance().getMimeTypeFromFileName(name));
        request.setMd5(md5);
        request.setStorageUrl(storageUrl);
        request.setCopy(false);
        
        panService.createFile(request, new PanService.Callback<PanFile>() {
            @Override
            public void onSuccess(PanFile data) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        R.string.pan_upload_success, Toast.LENGTH_SHORT).show();
                    loadFiles();
                });
            }
            
            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PanFileListActivity.this, 
                        "创建文件记录失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 从 URI 获取真实文件路径
     */
    private String getRealPathFromUri(Uri uri) {
        // 简化实现，实际项目中可能需要更复杂的处理
        String path = uri.getPath();
        if (path != null && path.startsWith("/document/")) {
            // 处理 Document URI
            String[] parts = path.split(":");
            if (parts.length >= 2) {
                return "/storage/emulated/0/" + parts[1];
            }
        }
        return path;
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pan_file_list, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.menu_create_folder) {
            createFolder();
            return true;
        } else if (itemId == R.id.menu_upload) {
            uploadFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
