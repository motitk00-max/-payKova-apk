package com.example.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

class PermissionManager(private val context: Context) {

    data class PermissionStatus(
        val hasRecordAudio: Boolean,
        val hasContacts: Boolean,
        val hasCallPhone: Boolean,
        val hasNotifications: Boolean,
        val hasCamera: Boolean
    ) {
        val hasCorePermissions: Boolean
            get() = hasRecordAudio
        val hasAllRecommended: Boolean
            get() = hasRecordAudio && hasContacts && hasCallPhone && hasNotifications
    }

    fun getPermissionStatus(): PermissionStatus {
        val hasRecordAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasContacts = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        return PermissionStatus(
            hasRecordAudio = hasRecordAudio,
            hasContacts = hasContacts,
            hasCallPhone = hasCallPhone,
            hasNotifications = hasNotifications,
            hasCamera = hasCamera
        )
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
