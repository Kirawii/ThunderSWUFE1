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
import androidx.navigation.compose.rememberNavController
import com.kirawii.thunderswufe.navigation.NavGraph
import com.kirawii.thunderswufe.ui.theme.ThunderSWUFETheme
import com.kirawii.thunderswufe.utils.NotificationPermissionManager

class MainActivity : ComponentActivity() {
    private lateinit var notificationPermissionManager: NotificationPermissionManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        notificationPermissionManager = NotificationPermissionManager(this)
        notificationPermissionManager.registerPermissionLauncher(
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
        )

        checkNotificationPermission()

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
            Runnable { },
            Runnable { }
        )
    }
}
