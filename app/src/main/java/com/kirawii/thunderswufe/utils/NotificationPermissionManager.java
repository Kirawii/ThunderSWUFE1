package com.kirawii.thunderswufe.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.activity.result.ActivityResultLauncher;
import androidx.core.content.ContextCompat;

public class NotificationPermissionManager {
    private final Activity activity;
    private ActivityResultLauncher<String> permissionLauncher;
    private Runnable onGrantedCallback = null;
    private Runnable onDeniedCallback = null;

    public NotificationPermissionManager(Activity activity) {
        this.activity = activity;
    }

    public void registerPermissionLauncher(ActivityResultLauncher<String> launcher) {
        this.permissionLauncher = launcher;
    }

    public void registerPermissionLauncher(ActivityResultLauncher<String> launcher, Runnable onGranted, Runnable onDenied) {
        this.permissionLauncher = launcher;
        this.onGrantedCallback = onGranted;
        this.onDeniedCallback = onDenied;
    }

    public void checkAndRequestPermission(Runnable onGranted, Runnable onDenied) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                if (onGranted != null) onGranted.run();
            } else if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showPermissionRationaleDialog(onGranted, onDenied);
            } else {
                if (permissionLauncher != null) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (onGranted != null) onGranted.run();
        }
    }

    public void checkAndRequestPermission() {
        checkAndRequestPermission(onGrantedCallback, onDeniedCallback);
    }

    private void showPermissionRationaleDialog(Runnable onGranted, Runnable onDenied) {
        if (permissionLauncher != null) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
} 