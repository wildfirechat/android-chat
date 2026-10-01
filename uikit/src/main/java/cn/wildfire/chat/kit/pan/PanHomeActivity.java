/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.pan.model.PanSpace;

/**
 * 网盘首页：列出可访问的空间（全局公共 / 我的公共 / 我的私有）。
 */
public class PanHomeActivity extends WfcBaseActivity {
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private PanSpaceAdapter adapter;

    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_home;
    }

    @Override
    protected void afterViews() {
        setTitle(R.string.cloud_drive);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanSpaceAdapter(space ->
            PanFileListActivity.start(this, space.spaceId, 0, space.name));
        recyclerView.setAdapter(adapter);
        loadSpaces();
    }

    private void loadSpaces() {
        PanService service = PanServiceProvider.getInstance().getService();
        if (service == null) {
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getVisibleSpaces(new PanService.ResultCallback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> spaces) {
                progressBar.setVisibility(View.GONE);
                adapter.setSpaces(spaces);
                emptyTextView.setVisibility(spaces == null || spaces.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(int code, String message) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                Toast.makeText(PanHomeActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
