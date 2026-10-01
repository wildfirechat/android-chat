/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.pan.model.PanDocEntry;
import cn.wildfire.chat.kit.pan.model.PanFile;

/**
 * 在线文档首页：最近打开、共享给我，新建 docx / xlsx / pptx，开源许可入口。
 */
public class PanDocsActivity extends WfcBaseActivity {
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private PanDocAdapter adapter;
    private boolean showingShared;

    @Override
    protected int contentLayout() {
        return R.layout.activity_pan_home;
    }

    @Override
    protected int menu() {
        return R.menu.pan_docs;
    }

    @Override
    protected void afterViews() {
        setTitle(R.string.online_docs);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PanDocAdapter(this::openDoc, this::onDocLongClick);
        recyclerView.setAdapter(adapter);
        loadRecent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.newDoc) {
            showNewDocDialog();
            return true;
        } else if (item.getItemId() == R.id.sharedWithMe) {
            if (showingShared) {
                loadRecent();
            } else {
                loadSharedWithMe();
            }
            return true;
        } else if (item.getItemId() == R.id.licenses) {
            String url = PanService.docLicensesUrl();
            if (url != null) {
                WfcWebViewActivity.loadUrl(this, getString(R.string.pan_licenses), url);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private PanService service() {
        return PanServiceProvider.getInstance().getService();
    }

    private void loadRecent() {
        PanService service = service();
        if (service == null) {
            finish();
            return;
        }
        showingShared = false;
        setTitle(R.string.online_docs);
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getRecentDocs(new PanService.ResultCallback<List<PanDocEntry>>() {
            @Override
            public void onSuccess(List<PanDocEntry> entries) {
                showEntries(entries);
            }

            @Override
            public void onError(int code, String message) {
                showError(message);
            }
        });
    }

    private void loadSharedWithMe() {
        PanService service = service();
        if (service == null) {
            return;
        }
        showingShared = true;
        setTitle(R.string.pan_shared_with_me);
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        service.getSharedWithMe(new PanService.ResultCallback<List<PanDocEntry>>() {
            @Override
            public void onSuccess(List<PanDocEntry> entries) {
                showEntries(entries);
            }

            @Override
            public void onError(int code, String message) {
                showError(message);
            }
        });
    }

    private void showEntries(List<PanDocEntry> entries) {
        progressBar.setVisibility(View.GONE);
        adapter.setEntries(entries);
        emptyTextView.setVisibility(entries == null || entries.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void openDoc(PanDocEntry entry) {
        if (entry == null || entry.file == null) {
            return;
        }
        String url = PanService.docOpenUrl(entry.file.fileId);
        if (url != null) {
            WfcWebViewActivity.loadUrl(this, entry.file.name, url);
        }
    }

    private void onDocLongClick(PanDocEntry entry) {
        if (entry == null || entry.file == null) {
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(entry.file.name)
            .setItems(new String[]{getString(R.string.pan_remove_recent)}, (dialog, which) -> removeRecent(entry))
            .show();
    }

    private void removeRecent(PanDocEntry entry) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.removeRecentDoc(entry.file.fileId, new PanService.OperationCallback() {
            @Override
            public void onSuccess() {
                loadRecent();
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(PanDocsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNewDocDialog() {
        String[] labels = {
            getString(R.string.pan_doc_word),
            getString(R.string.pan_doc_excel),
            getString(R.string.pan_doc_ppt),
        };
        String[] types = {"docx", "xlsx", "pptx"};
        new AlertDialog.Builder(this)
            .setTitle(R.string.pan_new_doc)
            .setItems(labels, (dialog, which) -> createDoc(types[which]))
            .show();
    }

    private void createDoc(String type) {
        PanService service = service();
        if (service == null) {
            return;
        }
        service.createDoc(type, null, new PanService.ResultCallback<PanFile>() {
            @Override
            public void onSuccess(PanFile file) {
                if (file == null) {
                    return;
                }
                String url = PanService.docOpenUrl(file.fileId);
                if (url != null) {
                    WfcWebViewActivity.loadUrl(PanDocsActivity.this, file.name, url);
                }
                loadRecent();
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(PanDocsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
