package cn.wildfire.chat.app.util;

import static cn.wildfire.chat.app.BaseApp.getContext;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;

public class DownloadUtil {
    private static final String TAG = "DownloadUtil";
    private static final String APK_NAME = "chat.apk";
    private static final int INSTALL_PERMISSION_CODE = 2048;

    private static BroadcastReceiver downloadReceiver;
    private static long currentDownloadId = -1;
    private static File pendingInstallFile;
    private static Context pendingInstallContext;

    public static void downloadApk(Context context, String url) {
        clearInstallState();
        pendingInstallContext = context.getApplicationContext();

        deleteOldApk(context);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType("application/vnd.android.package-archive")
                .setTitle("应用更新");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setRequiresCharging(false)
                   .setAllowedOverMetered(true);
        }

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        currentDownloadId = manager.enqueue(request);

        registerDownloadReceiver(context, manager);
    }

    private static void registerDownloadReceiver(Context context, DownloadManager manager) {
        unregisterReceiver(context);

        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == currentDownloadId) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        File apkFile = resolveDownloadedFile(context);
                        if (apkFile != null) {
                            pendingInstallFile = apkFile;
                            Toast.makeText(getContext(), "请手动安装！", Toast.LENGTH_LONG).show();
//                            startInstallProcedure(context, apkFile);
                        } else {
                            handleFileNotFound(context);
                        }
                        unregisterReceiver(context);
                    }, 3000); // 增加延迟确保文件写入
                }
            }
        };

        context.registerReceiver(downloadReceiver,
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static File resolveDownloadedFile(Context context) {
        // 三重文件获取保障
        File directFile = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APK_NAME
        );
        if (directFile.exists()) return directFile;

        File mediaFile = queryMediaStore(context);
        if (mediaFile != null) return mediaFile;

        return queryDownloadManager(context);
    }

    private static File queryMediaStore(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String[] projection = {MediaStore.Downloads.DATA};
        String selection = MediaStore.Downloads.DISPLAY_NAME + "=?";

        try (Cursor cursor = resolver.query(uri, projection, selection,
                new String[]{APK_NAME}, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.DATA));
                if (path != null) {
                    File file = new File(path);
                    if (file.exists()) return file;
                }
            }
        }
        return null;
    }

    private static File queryDownloadManager(Context context) {
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = manager.getUriForDownloadedFile(currentDownloadId);
        if (uri == null) return null;

        String path = getFilePathFromUri(context, uri);
        return path != null ? new File(path) : null;
    }

    private static String getFilePathFromUri(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return queryAbsolutePath(context, uri);
        }
        return uri.getPath();
    }

    @SuppressLint("Range")
    private static String queryAbsolutePath(Context context, Uri uri) {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            String[] split = docId.split(":");
            return Environment.getExternalStorageDirectory() + "/" + split[1];
        }
        return null;
    }

    private static void startInstallProcedure(Context context, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !hasInstallPermission(context)) {

            showPermissionDialog(context);
        } else {
            performInstall(context, apkFile);
        }
    }

    private static void performInstall(Context context, File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".install_provider", // 确保与manifest配置一致
                    apkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 关键修复：不要在此处结束进程！！！
            context.startActivity(installIntent);

        } catch (Exception e) {
            Log.e(TAG, "安装失败: " + e.getMessage());
            handleInstallError(context, e);
        }
    }

    private static String getInstallSourcePackage(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return context.getPackageManager()
                        .getInstallSourceInfo(context.getPackageName())
                        .getInstallingPackageName();
            }
        } catch (PackageManager.NameNotFoundException e) {
            return context.getPackageName();
        }
        return "";
    }

    private static void showPermissionDialog(Context context) {
        new AlertDialog.Builder(context)
                .setTitle("需要安装权限")
                .setMessage("请允许安装来自此来源的应用")
                .setPositiveButton("去设置", (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + context.getPackageName()));
                    if (context instanceof Activity) {
                        ((Activity) context).startActivityForResult(intent, INSTALL_PERMISSION_CODE);
                    } else {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // 在Activity的onActivityResult中调用
    public static void handlePermissionResult() {
        if (pendingInstallContext != null && pendingInstallFile != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (hasInstallPermission(pendingInstallContext)) {
                    performInstall(pendingInstallContext, pendingInstallFile);
                }
            }, 500);
        }
    }

    private static boolean hasInstallPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                context.getPackageManager().canRequestPackageInstalls();
    }

    private static void deleteOldApk(Context context) {
        // 方法1：直接删除
        File directFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                APK_NAME
        );
        if (directFile.exists() && !directFile.delete()) {
            // 方法2：通过MediaStore删除
            deleteViaMediaStore(context);
        }
    }

    private static void deleteViaMediaStore(Context context) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }
        String selection = MediaStore.Downloads.DISPLAY_NAME + "=?";
        resolver.delete(uri, selection, new String[]{APK_NAME});
    }

    private static void handleFileNotFound(Context context) {
        Toast.makeText(context, "找不到安装文件，请重试", Toast.LENGTH_LONG).show();
        openDownloadsFolder(context);
    }

    private static void handleInstallError(Context context, Exception e) {
        new AlertDialog.Builder(context)
                .setTitle("安装失败")
                .setMessage("错误代码：" + e.getClass().getSimpleName())
                .setPositiveButton("手动安装", (d, w) -> openDownloadsFolder(context))
                .show();
    }

    private static void openDownloadsFolder(Context context) {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    private static void clearInstallState() {
        pendingInstallFile = null;
        pendingInstallContext = null;
    }

    private static void unregisterReceiver(Context context) {
        try {
            if (downloadReceiver != null) {
                context.unregisterReceiver(downloadReceiver);
                downloadReceiver = null;
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver not registered");
        }
    }
}