/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.uikit.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class PermissionKit extends DialogFragment {

    private final PermissionReqTuple permissionReqTuple;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    public static void checkThenRequestPermission(Activity context, FragmentManager fragmentManager, PermissionReqTuple[] permissionReqTuples, RequestPermissionResultCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (callback != null) {
                callback.onRequestPermissionResult(true);
            }
            return;
        }
        Queue<PermissionReqTuple> permissionTupleQueue = new LinkedList<>(Arrays.asList(permissionReqTuples));
        final PermissionReqTuple[] tuple = {permissionTupleQueue.poll()};
        if (tuple[0] != null) {
            ActivityResultCallback<Boolean> _cb = new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean o) {
                    if (!o) {
                        // fail fast
                        showPermissionDeniedDialog(context, tuple[0].denyTitle, tuple[0].denyDesc);
                        if (callback != null) {
                            callback.onRequestPermissionResult(false);
                        }
                        return;
                    }
                    tuple[0] = permissionTupleQueue.poll();
                    if (tuple[0] != null) {
                        _requestPermission(context, fragmentManager, tuple[0], false, this);
                    } else {
                        if (callback != null) {
                            callback.onRequestPermissionResult(true);
                        }
                    }
                }
            };
            _requestPermission(context, fragmentManager, tuple[0], false, _cb);
        }
    }

    public static PermissionReqTuple[] buildRequestPermissionTuples(Context context, String[] permissions) {
        List<PermissionReqTuple> tuples = new ArrayList<>();
        CharSequence appName = context.getApplicationInfo().loadLabel(context.getPackageManager());
        for (String permission : permissions) {
            PermissionReqTuple tuple = null;
            switch (permission) {
                case Manifest.permission.RECORD_AUDIO:
                    tuple = new PermissionReqTuple(Manifest.permission.RECORD_AUDIO,
                        context.getString(R.string.permission_audio_title, appName),
                        context.getString(R.string.permission_audio_desc),
                        context.getString(R.string.permission_audio_denied_title),
                        context.getString(R.string.permission_audio_denied_desc, appName));
                    break;
                case Manifest.permission.CAMERA:
                    tuple = new PermissionReqTuple(Manifest.permission.CAMERA,
                        context.getString(R.string.permission_camera_title, appName),
                        context.getString(R.string.permission_camera_desc),
                        context.getString(R.string.permission_camera_denied_title),
                        context.getString(R.string.permission_camera_denied_desc, appName));
                    break;
                case Manifest.permission.BLUETOOTH_CONNECT:
                    tuple = new PermissionReqTuple(Manifest.permission.BLUETOOTH_CONNECT,
                        context.getString(R.string.permission_bluetooth_connect_title, appName),
                        context.getString(R.string.permission_bluetooth_connect_desc),
                        context.getString(R.string.permission_bluetooth_connect_denied_title),
                        context.getString(R.string.permission_bluetooth_connect_denied_desc, appName));
                    break;

                case Manifest.permission.READ_EXTERNAL_STORAGE:
                    tuple = new PermissionReqTuple(Manifest.permission.READ_EXTERNAL_STORAGE,
                        context.getString(R.string.permission_storage_title, appName),
                        context.getString(R.string.permission_storage_desc),
                        context.getString(R.string.permission_storage_denied_title),
                        context.getString(R.string.permission_storage_denied_desc, appName));
                    break;

                case Manifest.permission.POST_NOTIFICATIONS:
                    tuple = new PermissionReqTuple(Manifest.permission.POST_NOTIFICATIONS,
                        context.getString(R.string.permission_notification_title, appName),
                        context.getString(R.string.permission_notification_desc),
                        context.getString(R.string.permission_notification_denied_title),
                        context.getString(R.string.permission_notification_denied_desc, appName));
                    break;

                case Manifest.permission.READ_MEDIA_IMAGES:
                    tuple = new PermissionReqTuple(Manifest.permission.READ_MEDIA_IMAGES,
                        context.getString(R.string.permission_read_image_title, appName),
                        context.getString(R.string.permission_read_image_desc),
                        context.getString(R.string.permission_read_image_denied_title),
                        context.getString(R.string.permission_read_image_denied_desc, appName));
                    break;
                case Manifest.permission.READ_MEDIA_VIDEO:
                    tuple = new PermissionReqTuple(Manifest.permission.READ_MEDIA_VIDEO,
                        context.getString(R.string.permission_read_video_title, appName),
                        context.getString(R.string.permission_read_video_desc),
                        context.getString(R.string.permission_read_video_denied_title),
                        context.getString(R.string.permission_read_video_denied_desc, appName));

                    break;
                case Manifest.permission.ACCESS_FINE_LOCATION:
                    tuple = new PermissionReqTuple(Manifest.permission.ACCESS_FINE_LOCATION,
                        context.getString(R.string.permission_location_title, appName),
                        context.getString(R.string.permission_location_desc),
                        context.getString(R.string.permission_location_denied_title),
                        context.getString(R.string.permission_location_denied_desc, appName));
                    break;
                case Manifest.permission.ACCESS_COARSE_LOCATION:
                    tuple = new PermissionReqTuple(Manifest.permission.ACCESS_COARSE_LOCATION,
                        context.getString(R.string.permission_location_title, appName),
                        context.getString(R.string.permission_location_desc),
                        context.getString(R.string.permission_location_denied_title),
                        context.getString(R.string.permission_location_denied_desc, appName));
                    break;
                default:
                    break;
            }
            if (tuple != null) {
                tuples.add(tuple);
            }

        }
        return tuples.toArray(new PermissionReqTuple[0]);
    }

    private static void _requestPermission(Activity context, FragmentManager fragmentManager, PermissionReqTuple permissionReqTuple, boolean force, ActivityResultCallback<Boolean> callback) {
        if (force) {
            showRequestPermissionDialog(fragmentManager, permissionReqTuple, callback);
            return;
        }
        if (context.checkSelfPermission(permissionReqTuple.permission) == PackageManager.PERMISSION_GRANTED) {
            callback.onActivityResult(true);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(context, permissionReqTuple.permission)) {
            showExplainPermissionDialog(context, fragmentManager, permissionReqTuple, callback);
        } else {
            showRequestPermissionDialog(fragmentManager, permissionReqTuple, callback);
        }
    }

    private static void showRequestPermissionDialog(FragmentManager fragmentManager, PermissionReqTuple tuple, ActivityResultCallback<Boolean> callback) {
        PermissionKit requestPermissionDialog = new PermissionKit(tuple);
        requestPermissionDialog.requestPermissionLauncher = requestPermissionDialog.registerForActivityResult(new ActivityResultContracts.RequestPermission(), o -> {
            requestPermissionDialog.dismiss();
            callback.onActivityResult(o);
        });
        requestPermissionDialog.show(fragmentManager, "permissionDialog");
    }

    private static void showPermissionDeniedDialog(Context context, String title, String desc) {
        MaterialDialog dialog = new MaterialDialog.Builder(context)
            .title(title)
            .content(desc)
            .positiveText("去设置")
            .negativeText("取消")
            .onPositive((dialog1, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
                dialog1.dismiss();
            }).build();
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void showExplainPermissionDialog(Activity context, FragmentManager fragmentManager, PermissionReqTuple tuple, ActivityResultCallback<Boolean> _cb) {
        new MaterialDialog.Builder(context)
            .title(tuple.requestTitle)
            .content(tuple.requestDesc)
            .cancelable(false)
            .positiveText("去授权")
            .negativeText("取消")
            .onPositive((dialog1, which) -> {
                _requestPermission(context, fragmentManager, tuple, true, _cb);
            })
            .onNegative(((dialog1, which) -> {
                _cb.onActivityResult(false);
            }))
            .build()
            .show();
    }

    private PermissionKit(PermissionReqTuple permissionReqTuple) {
        this.permissionReqTuple = permissionReqTuple;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_FullScreenDialog);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Permission_Dialog_FullScreen_Transparent);
        setCancelable(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.request_permission_dialog_fragment, container, false);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        TextView descTextView = view.findViewById(R.id.descTextView);
        titleTextView.setText(permissionReqTuple.requestTitle);
        descTextView.setText(permissionReqTuple.requestDesc);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestPermissionLauncher.launch(permissionReqTuple.permission);
    }

    public static class PermissionReqTuple {
        String permission;
        String requestTitle;
        String requestDesc;
        String denyTitle;
        String denyDesc;
        int iconResId;

        public PermissionReqTuple(String permission, String requestTitle, String requestDesc, String denyTitle, String denyDesc) {
            this.permission = permission;
            this.requestTitle = requestTitle;
            this.requestDesc = requestDesc;
            this.denyTitle = denyTitle;
            this.denyDesc = denyDesc;
        }

        public PermissionReqTuple(String permission, String requestTitle, String requestDesc, String denyTitle, String denyDesc, int iconResId) {
            this.permission = permission;
            this.requestTitle = requestTitle;
            this.requestDesc = requestDesc;
            this.denyTitle = denyTitle;
            this.denyDesc = denyDesc;
            this.iconResId = iconResId;
        }
    }

    public interface RequestPermissionResultCallback {
        void onRequestPermissionResult(boolean allGranted);
    }
}
