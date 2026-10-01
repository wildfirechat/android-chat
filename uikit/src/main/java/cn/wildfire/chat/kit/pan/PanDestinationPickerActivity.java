/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.content.Intent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 移动 / 复制的目标选择：先选空间，再进目录，确认后把当前位置（spaceId + parentId）回传。
 */
public class PanDestinationPickerActivity extends WfcBaseActivity {
    public static final String RESULT_SPACE_ID = "resultSpaceId";
    public static final String RESULT_PARENT_ID = "resultParentId";

    private static final String EXTRA_EXCLUDE_FILE_ID = "excludeFileId";
    private static final String EXTRA_EXCLUDE_NAME = "excludeName";
    private static final String EXTRA_IS_COPY = "isCopy";

    private long excludeFileId;
    private String excludeName;
    private boolean isCopy;
    private int pickerTitleRes = R.string.pan_move_to;

    private ProgressBar progressBar;
    private TextView emptyTextView;
    private RecyclerView recyclerView;
    private PanSpaceAdapter spaceAdapter;
    private PanFileAdapter fileAdapter;

    /** 已进入的目录路径，空表示还在选空间。 */
    private final List<long[]> pathStack = new ArrayList<>();

    public static Intent buildIntent(android.content.Context context, long excludeFileId, String excludeName, boolean isCopy) {
        Intent intent = new Intent(context, PanDestinationPickerActivity.class);
        intent.putExtra(EXTRA_EXCLUDE_FILE_ID, excludeFileId);
        intent.putExtra(EXTRA_EXCLUDE_NAME, excludeName);
        intent.putExtra(EXTRA_IS_COPY, isCopy);
        return intent;
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_file_list;
    }

    @Override
    protected int menu() {
        return R.menu.pan_destination_picker;
    }

    @Override
    protected void afterViews() {
        setTitle(R.string.pan_move_to);
        excludeFileId = getIntent().getLongExtra(EXTRA_EXCLUDE_FILE_ID, 0);
        excludeName = getIntent().getStringExtra(EXTRA_EXCLUDE_NAME);
        isCopy = getIntent().getBooleanExtra(EXTRA_IS_COPY, false);
        pickerTitleRes = isCopy ? R.string.pan_copy_to : R.string.pan_move_to;
        setTitle(pickerTitleRes);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        spaceAdapter = new PanSpaceAdapter(this::enterSpace);
        fileAdapter = new PanFileAdapter(this::onFileClick, null);
        recyclerView.setAdapter(spaceAdapter);
        loadSpaces();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            if (pathStack.isEmpty()) {
                Toast.makeText(this, R.string.pan_move_to, Toast.LENGTH_SHORT).show();
                return true;
            }
            long[] current = pathStack.get(pathStack.size() - 1);
            Intent result = new Intent();
            result.putExtra(RESULT_SPACE_ID, current[0]);
            result.putExtra(RESULT_PARENT_ID, current[1]);
            setResult(Activity.RESULT_OK, result);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!pathStack.isEmpty()) {
            pathStack.remove(pathStack.size() - 1);
            if (pathStack.isEmpty()) {
                recyclerView.setAdapter(spaceAdapter);
                loadSpaces();
            } else {
                long[] current = pathStack.get(pathStack.size() - 1);
                loadFiles(current[0], current[1]);
            }
            return;
        }
        super.onBackPressed();
    }

    private PanService service() {
        return PanServiceProvider.getInstance().getService();
    }

    private void loadSpaces() {
        PanService service = service();
        if (service == null) {
            finish();
            return;
        }
        setTitle(pickerTitleRes);
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getVisibleSpaces(new PanService.ResultCallback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> spaces) {
                progressBar.setVisibility(View.GONE);
                spaceAdapter.setSpaces(spaces);
                emptyTextView.setVisibility(spaces == null || spaces.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(int code, String message) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                Toast.makeText(PanDestinationPickerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enterSpace(PanSpace space) {
        pathStack.add(new long[]{space.spaceId, 0});
        recyclerView.setAdapter(fileAdapter);
        setTitle(space.name);
        loadFiles(space.spaceId, 0);
    }

    private void onFileClick(PanFile file) {
        if (!file.isFolder()) {
            return;
        }
        long spaceId = pathStack.get(pathStack.size() - 1)[0];
        pathStack.add(new long[]{spaceId, file.fileId});
        setTitle(file.name);
        loadFiles(spaceId, file.fileId);
    }

    private void loadFiles(long spaceId, long parentId) {
        PanService service = service();
        if (service == null) {
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getSpaceFiles(spaceId, parentId, new PanService.ResultCallback<List<PanFile>>() {
            @Override
            public void onSuccess(List<PanFile> files) {
                progressBar.setVisibility(View.GONE);
                List<PanFile> filtered = new ArrayList<>();
                if (files != null) {
                    for (PanFile file : files) {
                        // 不能移动到自身
                        if (excludeFileId != 0 && file.fileId == excludeFileId) {
                            continue;
                        }
                        filtered.add(file);
                    }
                }
                fileAdapter.setFiles(filtered);
                emptyTextView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(int code, String message) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                Toast.makeText(PanDestinationPickerActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
