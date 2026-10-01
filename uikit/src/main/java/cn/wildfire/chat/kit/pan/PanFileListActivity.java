/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanFileUrl;
import cn.wildfire.chat.kit.pan.model.PanUploadCancelToken;
import cn.wildfire.chat.kit.pan.model.PanVersion;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 网盘目录浏览：上传、新建文件夹、下载/在线预览、重命名、移动、复制、删除、分享、历史版本。
 */
public class PanFileListActivity extends WfcBaseActivity {
    private static final String EXTRA_SPACE_ID = "spaceId";
    private static final String EXTRA_PARENT_ID = "parentId";
    private static final String EXTRA_TITLE = "title";

    private static final int REQUEST_PICK_UPLOAD = 401;
    private static final int REQUEST_DESTINATION_MOVE = 402;
    private static final int REQUEST_DESTINATION_COPY = 403;

    private long spaceId;
    private long parentId;
    private String folderTitle;

    private ProgressBar progressBar;
    private TextView emptyTextView;
    private PanFileAdapter adapter;

    public static void start(Context context, long spaceId, long parentId, String title) {
        Intent intent = new Intent(context, PanFileListActivity.class);
        intent.putExtra(EXTRA_SPACE_ID, spaceId);
        intent.putExtra(EXTRA_PARENT_ID, parentId);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_file_list;
    }

    @Override
    protected int menu() {
        return R.menu.pan_file_list;
    }

    @Override
    protected void afterViews() {
        spaceId = getIntent().getLongExtra(EXTRA_SPACE_ID, 0);
        parentId = getIntent().getLongExtra(EXTRA_PARENT_ID, 0);
        folderTitle = getIntent().getStringExtra(EXTRA_TITLE);
        setTitle(TextUtils.isEmpty(folderTitle) ? getString(R.string.cloud_drive) : folderTitle);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanFileAdapter(this::onFileClick, this::showFileMenu);
        recyclerView.setAdapter(adapter);
        loadFiles();
    }

    private PanService service() {
        return PanServiceProvider.getInstance().getService();
    }

    private void loadFiles() {
        PanService service = service();
        if (service == null) {
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getSpaceFiles(spaceId, parentId, new PanService.ResultCallback<List<PanFile>>() {
            @Override
            public void onSuccess(List<PanFile> files) {
                progressBar.setVisibility(View.GONE);
                adapter.setFiles(files);
                emptyTextView.setVisibility(files == null || files.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(int code, String message) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                showToast(message);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.newFolder) {
            showNewFolderDialog();
            return true;
        } else if (item.getItemId() == R.id.upload) {
            pickFileForUpload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ==================== 文件点击 ====================

    private void onFileClick(PanFile file) {
        if (file.isFolder()) {
            start(this, spaceId, file.fileId, file.name);
            return;
        }
        openFile(file);
    }

    private void openFile(PanFile file) {
        String docUrl = PanService.docOpenUrl(file.fileId);
        if (docUrl != null && PanService.isOnlineDocName(file.name)) {
            WfcWebViewActivity.loadUrl(this, file.name, docUrl);
            return;
        }
        downloadAndOpen(file);
    }

    private void downloadAndOpen(PanFile file) {
        PanService service = service();
        if (service == null) {
            return;
        }
        showToast(getString(R.string.pan_loading));
        service.getFileDownloadUrl(file.fileId, 0, new PanService.ResultCallback<PanFileUrl>() {
            @Override
            public void onSuccess(PanFileUrl url) {
                if (url == null || TextUtils.isEmpty(url.storageUrl)) {
                    showToast(getString(R.string.pan_download_failed));
                    return;
                }
                download(new File(Config.FILE_SAVE_DIR, file.name), url.storageUrl);
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    private void download(File target, String storageUrl) {
        DownloadManager.download(storageUrl, target.getParent(), target.getName(), new DownloadManager.SimpleOnDownloadListener() {
            @Override
            public void onUiSuccess(File file) {
                openLocalFile(file);
            }

            @Override
            public void onUiFail() {
                showToast(getString(R.string.pan_download_failed));
            }
        });
    }

    private void openLocalFile(File file) {
        try {
            startActivity(FileUtils.getViewIntent(this, file));
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.pan_no_app_to_open));
        }
    }

    // ==================== 长按菜单 ====================

    private void showFileMenu(PanFile file) {
        List<String> labels = new ArrayList<>();
        List<Integer> actions = new ArrayList<>();
        if (file.isFolder()) {
            labels.add(getString(R.string.pan_open));
            actions.add(0);
        } else if (PanService.isOnlineDocName(file.name)) {
            labels.add(getString(R.string.doc_online_preview));
            actions.add(1);
        }
        if (!file.isFolder()) {
            labels.add(getString(R.string.pan_download));
            actions.add(2);
        }
        labels.add(getString(R.string.pan_rename));
        actions.add(3);
        labels.add(getString(R.string.pan_move_to));
        actions.add(4);
        labels.add(getString(R.string.pan_copy_to));
        actions.add(5);
        labels.add(getString(R.string.pan_share));
        actions.add(6);
        labels.add(getString(R.string.pan_versions));
        actions.add(7);
        if (!file.isFolder()) {
            labels.add(getString(R.string.pan_save_to_my_pan));
            actions.add(8);
        }
        labels.add(getString(R.string.pan_delete));
        actions.add(9);

        new AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(labels.toArray(new String[0]), (dialog, which) -> handleFileAction(actions.get(which), file))
            .show();
    }

    private void handleFileAction(int action, PanFile file) {
        switch (action) {
            case 0:
            case 1:
                openFile(file);
                break;
            case 2:
                downloadAndOpen(file);
                break;
            case 3:
                showRenameDialog(file);
                break;
            case 4:
                startDestinationPicker(file, REQUEST_DESTINATION_MOVE);
                break;
            case 5:
                startDestinationPicker(file, REQUEST_DESTINATION_COPY);
                break;
            case 6:
                PanShareActivity.start(this, file.fileId, file.name);
                break;
            case 7:
                showVersions(file);
                break;
            case 8:
                saveToMyPan(file);
                break;
            case 9:
                confirmDelete(file);
                break;
            default:
                break;
        }
    }

    private void startDestinationPicker(PanFile file, int requestCode) {
        pendingFileId = file.fileId;
        Intent intent = PanDestinationPickerActivity.buildIntent(this, file.fileId, file.name,
            requestCode == REQUEST_DESTINATION_COPY);
        startActivityForResult(intent, requestCode);
    }

    // ==================== 新建文件夹 / 重命名 ====================

    private void showNewFolderDialog() {
        EditText editText = new EditText(this);
        editText.setHint(R.string.pan_folder_name);
        new AlertDialog.Builder(this)
            .setTitle(R.string.pan_new_folder)
            .setView(editText)
            .setPositiveButton(R.string.pan_confirm, (dialog, which) -> {
                String name = editText.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    showToast(getString(R.string.pan_name_empty));
                    return;
                }
                createFolder(name);
            })
            .setNegativeButton(R.string.pan_cancel, null)
            .show();
    }

    private void createFolder(String name) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.createFolder(spaceId, parentId, name, new PanService.ResultCallback<PanFile>() {
            @Override
            public void onSuccess(PanFile file) {
                showToast(getString(R.string.pan_operation_success));
                loadFiles();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    private void showRenameDialog(PanFile file) {
        EditText editText = new EditText(this);
        editText.setText(file.name);
        editText.setSelection(editText.getText().length());
        new AlertDialog.Builder(this)
            .setTitle(R.string.pan_rename)
            .setView(editText)
            .setPositiveButton(R.string.pan_confirm, (dialog, which) -> {
                String name = editText.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    showToast(getString(R.string.pan_name_empty));
                    return;
                }
                rename(file, name);
            })
            .setNegativeButton(R.string.pan_cancel, null)
            .show();
    }

    private void rename(PanFile file, String newName) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.renameFile(file.fileId, newName, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                showToast(getString(R.string.pan_operation_success));
                loadFiles();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    private void confirmDelete(PanFile file) {
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.pan_delete_confirm, file.name))
            .setPositiveButton(R.string.pan_confirm, (dialog, which) -> delete(file))
            .setNegativeButton(R.string.pan_cancel, null)
            .show();
    }

    private void delete(PanFile file) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.deleteFile(file.fileId, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                showToast(getString(R.string.pan_operation_success));
                loadFiles();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    // ==================== 历史版本 ====================

    private void showVersions(PanFile file) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.getVersions(file.fileId, new PanService.ResultCallback<List<PanVersion>>() {
            @Override
            public void onSuccess(List<PanVersion> versions) {
                if (versions == null || versions.isEmpty()) {
                    showToast(getString(R.string.pan_empty));
                    return;
                }
                String[] items = new String[versions.size()];
                for (int i = 0; i < versions.size(); i++) {
                    PanVersion version = versions.get(i);
                    String label = "v" + version.versionNo + "  " + PanService.formatPanSize(version.size);
                    if (version.current) {
                        label = label + "  (" + getString(R.string.pan_version_current) + ")";
                    }
                    items[i] = label;
                }
                new AlertDialog.Builder(PanFileListActivity.this)
                    .setTitle(R.string.pan_versions)
                    .setItems(items, (dialog, which) -> confirmRestore(file, versions.get(which)))
                    .show();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    private void confirmRestore(PanFile file, PanVersion version) {
        if (version.current) {
            return;
        }
        new AlertDialog.Builder(this)
            .setMessage(getString(R.string.pan_restore) + " v" + version.versionNo + "?")
            .setPositiveButton(R.string.pan_confirm, (dialog, which) -> restore(file, version))
            .setNegativeButton(R.string.pan_cancel, null)
            .show();
    }

    private void restore(PanFile file, PanVersion version) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.restoreVersion(file.fileId, version.versionNo, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                showToast(getString(R.string.pan_operation_success));
                loadFiles();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    // ==================== 存到我的网盘 ====================

    private void saveToMyPan(PanFile file) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.getFileDownloadUrl(file.fileId, 0, new PanService.ResultCallback<PanFileUrl>() {
            @Override
            public void onSuccess(PanFileUrl url) {
                if (url == null || TextUtils.isEmpty(url.storageUrl)) {
                    showToast(getString(R.string.pan_save_failed));
                    return;
                }
                PanSaveHelper.saveUrlToMyPan(service, file.name, file.size, url.storageUrl,
                    file.mimeType, file.md5, new PanService.ResultCallback<PanFile>() {
                        @Override
                        public void onSuccess(PanFile saved) {
                            showToast(getString(R.string.pan_save_success));
                        }

                        @Override
                        public void onError(int code, String message) {
                            showToast(getString(R.string.pan_save_failed));
                        }
                    });
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        });
    }

    // ==================== 上传 ====================

    private void pickFileForUpload() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, getString(R.string.pan_upload)), REQUEST_PICK_UPLOAD);
        } catch (ActivityNotFoundException e) {
            showToast(getString(R.string.pan_operation_failed));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_PICK_UPLOAD) {
            if (data == null || data.getData() == null) {
                return;
            }
            String path = copyUriToCache(data.getData());
            if (path == null) {
                showToast(getString(R.string.pan_operation_failed));
                return;
            }
            upload(path);
        } else if (requestCode == REQUEST_DESTINATION_MOVE || requestCode == REQUEST_DESTINATION_COPY) {
            if (data == null) {
                return;
            }
            long targetSpaceId = data.getLongExtra(PanDestinationPickerActivity.RESULT_SPACE_ID, 0);
            long targetParentId = data.getLongExtra(PanDestinationPickerActivity.RESULT_PARENT_ID, 0);
            // 目标回调里带上被操作文件 id
            onDestinationPicked(targetSpaceId, targetParentId, requestCode);
        }
    }

    private long pendingFileId;

    private void onDestinationPicked(long targetSpaceId, long targetParentId, int requestCode) {
        PanService service = service();
        if (service == null || pendingFileId == 0) {
            return;
        }
        PanService.OperationCallback callback = new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                showToast(getString(R.string.pan_operation_success));
                loadFiles();
            }

            @Override
            public void onError(int code, String message) {
                showToast(message);
            }
        };
        if (requestCode == REQUEST_DESTINATION_MOVE) {
            service.moveFile(pendingFileId, targetSpaceId, targetParentId, callback);
        } else {
            service.copyFile(pendingFileId, targetSpaceId, targetParentId, callback);
        }
    }

    private void upload(String localPath) {
        PanService service = service();
        if (service == null) {
            return;
        }
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(100);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.pan_upload));
        dialog.show();
        PanUploadCancelToken token = new PanUploadCancelToken();
        dialog.setOnCancelListener(d -> token.cancel());
        service.uploadFile(localPath, spaceId, parentId, null, token,
            progress -> dialog.setProgress((int) (progress * 100)),
            new PanService.ResultCallback<PanFile>() {
                @Override
                public void onSuccess(PanFile file) {
                    dialog.dismiss();
                    showToast(getString(R.string.pan_operation_success));
                    loadFiles();
                }

                @Override
                public void onError(int code, String message) {
                    dialog.dismiss();
                    showToast(message);
                }
            });
    }

    private String copyUriToCache(Uri uri) {
        String name = null;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    name = cursor.getString(index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(name)) {
            name = "upload_" + System.currentTimeMillis();
        }
        File dir = new File(getCacheDir(), "pan_upload");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File out = new File(dir, name);
        try (InputStream is = getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(out)) {
            if (is == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            return out.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message == null ? "" : message, Toast.LENGTH_SHORT).show();
    }
}
