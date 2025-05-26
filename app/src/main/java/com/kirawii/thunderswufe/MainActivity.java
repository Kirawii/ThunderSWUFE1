package com.kirawii.thunderswufe;

import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.kirawii.thunderswufe.databinding.ActivityMainBinding;
import com.kirawii.thunderswufe.utils.NotificationPermissionManager;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NotificationPermissionManager notificationPermissionManager;
    private NavController navController;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化视图绑定
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置导航
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        }

        // 初始化通知权限管理
        notificationPermissionManager = new NotificationPermissionManager(this);
        ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startBackgroundServices();
                    } else {
                        showPermissionDeniedMessage();
                    }
                }
        );

        notificationPermissionManager.registerPermissionLauncher(
                permissionLauncher,
                this::startBackgroundServices,
                this::showPermissionDeniedMessage
        );

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        notificationPermissionManager.checkAndRequestPermission(
                this::startBackgroundServices,
                this::showPermissionDeniedMessage
        );
    }

    private void startBackgroundServices() {
        ThunderApplication app = (ThunderApplication) getApplication();
        app.startBackgroundServices();
    }

    private void showPermissionDeniedMessage() {
        // TODO: 实现权限被拒绝时的提示
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
} 