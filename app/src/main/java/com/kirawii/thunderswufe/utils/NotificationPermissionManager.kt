package com.kirawii.thunderswufe.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class NotificationPermissionManager(private val activity: Activity) {
    
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    
    fun registerPermissionLauncher(
        launcher: ActivityResultLauncher<String>,
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        permissionLauncher = launcher
    }
    
    fun checkAndRequestPermission(
        onGranted: () -> Unit = {},
        onDenied: () -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    onGranted()
                }
                activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> {
                    // 显示权限说明对话框
                    showPermissionRationaleDialog(onGranted, onDenied)
                }
                else -> {
                    permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 13以下版本默认允许通知
            onGranted()
        }
    }
    
    private fun showPermissionRationaleDialog(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        // TODO: 显示权限说明对话框
        permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
} 