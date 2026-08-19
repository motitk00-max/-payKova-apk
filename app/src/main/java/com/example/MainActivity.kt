package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.presentation.KovaScreen
import com.example.presentation.KovaViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VoidBlack

class MainActivity : ComponentActivity() {

    private val viewModel: KovaViewModel by viewModels()

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VoidBlack
                ) {
                    KovaScreen(
                        viewModel = viewModel,
                        onRequestPermissions = { requestAllPermissions() },
                        onOpenAppSettings = { viewModel.permissionManager.openAppSettings() }
                    )
                }
            }
        }

        // Check and prompt permissions if not yet granted
        requestAllPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    private fun requestAllPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionRequestLauncher.launch(permissions.toTypedArray())
    }
}

