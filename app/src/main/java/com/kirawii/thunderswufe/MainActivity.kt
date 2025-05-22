package com.kirawii.thunderswufe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kirawii.thunderswufe.navigation.NavGraph
import com.kirawii.thunderswufe.ui.theme.ThunderSWUFETheme
import com.kirawii.thunderswufe.utils.NotificationPermissionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var notificationPermissionManager: NotificationPermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as ThunderApplication

        notificationPermissionManager = NotificationPermissionManager(this)
        notificationPermissionManager.registerPermissionLauncher(
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    startBackgroundServices()
                } else {
                    showPermissionDeniedMessage()
                }
            }
        )

        checkNotificationPermission()

        lifecycleScope.launch {
            val roomInfo = app.userPreferencesManager.roomInfo.first()
            if (roomInfo.roomNo.isEmpty()) {
            }
        }
        
        enableEdgeToEdge()
        setContent {
            ThunderSWUFETheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    private fun checkNotificationPermission() {
        notificationPermissionManager.checkAndRequestPermission(
            onGranted = {
                startBackgroundServices()
            },
            onDenied = {
                showPermissionDeniedMessage()
            }
        )
    }
    
    private fun startBackgroundServices() {
        val app = application as ThunderApplication
        app.startBackgroundServices()
    }
    
    private fun showPermissionDeniedMessage() {
    }
}