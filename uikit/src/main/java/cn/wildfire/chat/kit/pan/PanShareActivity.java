/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.group.GroupListActivity;
import cn.wildfire.chat.kit.pan.model.PanShare;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

/**
 * 分享管理：查看/新增/删除文件的分享，权限可查看或可编辑。
 */
public class PanShareActivity extends WfcBaseActivity {
    private static final String EXTRA_FILE_ID = "fileId";
    private static final String EXTRA_FILE_NAME = "fileName";

    private static final int REQUEST_PICK_USER = 501;
    private static final int REQUEST_PICK_GROUP = 502;

    private long fileId;
    private String fileName;

    private ProgressBar progressBar;
    private TextView emptyTextView;
    private PanShareAdapter adapter;

    public static void start(Context context, long fileId, String fileName) {
        Intent intent = new Intent(context, PanShareActivity.class);
        intent.putExtra(EXTRA_FILE_ID, fileId);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        context.startActivity(intent);
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_home;
    }

    @Override
    protected int menu() {
        return R.menu.pan_share;
    }

    @Override
    protected void afterViews() {
        fileId = getIntent().getLongExtra(EXTRA_FILE_ID, 0);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        setTitle(TextUtils.isEmpty(fileName) ? getString(R.string.pan_share) : fileName);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanShareAdapter(this::showShareOptions);
        recyclerView.setAdapter(adapter);
        loadShares();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addShare) {
            chooseTargetType();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private PanService service() {
        return PanServiceProvider.getInstance().getService();
    }

    private void loadShares() {
        PanService service = service();
        if (service == null) {
            finish();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getShares(fileId, new PanService.ResultCallback<List<PanShare>>() {
            @Override
            public void onSuccess(List<PanShare> shares) {
                progressBar.setVisibility(View.GONE);
                adapter.setShares(shares);
                emptyTextView.setVisibility(shares == null || shares.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(int code, String message) {
                progressBar.setVisibility(View.GONE);
                emptyTextView.setVisibility(View.VISIBLE);
                Toast.makeText(PanShareActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void chooseTargetType() {
        new AlertDialog.Builder(this)
            .setItems(new String[]{getString(R.string.pan_share_to_user), getString(R.string.pan_share_to_group)},
                (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = PickContactActivity.buildPickIntent(this, 1, null, null);
                        startActivityForResult(intent, REQUEST_PICK_USER);
                    } else {
                        Intent intent = new Intent(this, GroupListActivity.class);
                        intent.putExtra(GroupListActivity.INTENT_FOR_RESULT, true);
                        startActivityForResult(intent, REQUEST_PICK_GROUP);
                    }
                })
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQUEST_PICK_USER) {
            List<UserInfo> users = data.getParcelableArrayListExtra(PickContactActivity.RESULT_PICKED_USERS);
            if (users != null && !users.isEmpty()) {
                addShare(false, users.get(0).uid);
            }
        } else if (requestCode == REQUEST_PICK_GROUP) {
            List<GroupInfo> groups = data.getParcelableArrayListExtra("groupInfos");
            if (groups != null && !groups.isEmpty()) {
                addShare(true, groups.get(0).target);
            }
        }
    }

    private void addShare(boolean isGroup, String targetId) {
        askPermission(canEdit -> doAddShare(isGroup, targetId, canEdit));
    }

    private interface PermissionCallback {
        void onPicked(boolean canEdit);
    }

    private void askPermission(PermissionCallback callback) {
        new AlertDialog.Builder(this)
            .setItems(new String[]{getString(R.string.pan_permission_view), getString(R.string.pan_permission_edit)},
                (dialog, which) -> callback.onPicked(which == 1))
            .show();
    }

    private void doAddShare(boolean isGroup, String targetId, boolean canEdit) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.setShare(fileId, isGroup, targetId, canEdit, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                loadShares();
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(PanShareActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShareOptions(PanShare share) {
        String[] items = {
            getString(R.string.pan_permission_edit),
            getString(R.string.pan_delete),
        };
        new AlertDialog.Builder(this)
            .setItems(items, (dialog, which) -> {
                if (which == 0) {
                    togglePermission(share);
                } else {
                    removeShare(share);
                }
            })
            .show();
    }

    private void togglePermission(PanShare share) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.setShare(fileId, share.isGroup(), share.targetId, !share.canEdit(), new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                loadShares();
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(PanShareActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeShare(PanShare share) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.removeShare(share.id, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                loadShares();
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(PanShareActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
