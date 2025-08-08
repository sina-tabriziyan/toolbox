package com.sina.library.permission

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.collections.contains

object SimplePermission {
    private const val PREFS_NAME = "simple_perm_prefs"
    private const val COUNT_PREFIX = "perm_count_"
    private const val DENIALS_NEEDED_FOR_SETTINGS = 2

    private lateinit var prefs: SharedPreferences

    fun register(application: Application) {
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Permissions array including MANAGE_EXTERNAL_STORAGE for API 30+
     * and granular media permissions for API 33+
     */
    fun getStoragePermissionsForApi(api: Int): Array<String> {
        return when {
            api >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES
            )

            api >= 30 -> arrayOf(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            )

            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("ContextCastToActivity")
    @Composable
    fun rememberPermissionRequest(
        permissions: Array<String>? = null,
        onResult: (Boolean) -> Unit = {}
    ): PermissionRequestState {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val activity = LocalContext.current as? Activity

        if (!this::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val actualPermissions = permissions ?: getStoragePermissionsForApi(Build.VERSION.SDK_INT)

        var denialCount by remember {
            mutableStateOf(prefs.getInt(COUNT_PREFIX + actualPermissions.joinToString("_"), 0))
        }
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showRationaleDialog by remember { mutableStateOf(false) }
        var resultDelivered by remember { mutableStateOf(false) }

        var rationaleShownForCurrentCount by remember { mutableStateOf(false) }
        var settingsShownForCurrentCount by remember { mutableStateOf(false) }

        /**
         * Special check for MANAGE_EXTERNAL_STORAGE permission
         */
        val isManageExternalStorage =
            actualPermissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)

        // Check if all permissions granted
        val allGranted = if (isManageExternalStorage) {
            // For MANAGE_EXTERNAL_STORAGE check special method
            Environment.isExternalStorageManager()
        } else {
            actualPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }

        // Launcher for runtime permissions
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { resultMap ->
            if (resultDelivered) return@rememberLauncherForActivityResult

            val grantedAll = resultMap.values.all { it }

            coroutineScope.launch {
                val key = COUNT_PREFIX + actualPermissions.joinToString("_")
                if (grantedAll) {
                    prefs.edit().remove(key).apply()
                    denialCount = 0
                    resultDelivered = true
                    onResult(true)
                } else {
                    denialCount++
                    prefs.edit().putInt(key, denialCount).apply()

                    if (denialCount >= DENIALS_NEEDED_FOR_SETTINGS) {
                        showSettingsDialog = true
                    } else {
                        showRationaleDialog = true
                    }
                    resultDelivered = true
                    onResult(false)
                }
            }
        }

        /**
         * Request permission logic, handles MANAGE_EXTERNAL_STORAGE separately:
         */
        val requestPermission: () -> Unit = {
            if (allGranted) {
                onResult(true)
            } else

                if (isManageExternalStorage) {
                    if (!Environment.isExternalStorageManager()) {
                        // Open system settings for MANAGE_EXTERNAL_STORAGE permission
                        try {
                            val intent =
                                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            intent.data = Uri.parse("package:" + context.packageName)
                            activity?.startActivity(intent)
                        } catch (e: Exception) {
                            // fallback
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            activity?.startActivity(intent)
                        }
                    } else {
                        // Permission granted after returning from settings
                        onResult(true)
                    }
                } else {
                    // Request normal runtime permissions
                    resultDelivered = false
                    launcher.launch(actualPermissions)
                }
        }

        // Lifecycle effect to refresh permission state when returning from settings:
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val currentlyGranted = if (isManageExternalStorage) {
                        Environment.isExternalStorageManager()
                    } else {
                        actualPermissions.all {
                            ContextCompat.checkSelfPermission(
                                context,
                                it
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    if (currentlyGranted != allGranted) {
                        // Update denial count and dialogs accordingly
                        if (currentlyGranted) {
                            denialCount = 0
                            showSettingsDialog = false
                            showRationaleDialog = false
                            onResult(true)
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Rationale dialog shown once per denial count
        if (showRationaleDialog && !rationaleShownForCurrentCount) {
            AlertDialog(
                onDismissRequest = { /* allow dismiss or ignore */ },
                title = { Text("Permission Needed") },
                text = { Text("This permission is needed for proper app functionality. Please allow it.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        rationaleShownForCurrentCount = true
                        requestPermission()
                    }) { Text("Allow") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        rationaleShownForCurrentCount = true
                        onResult(false)
                    }) { Text("Deny") }
                }
            )
        }

        // Settings dialog shown once per denial count
        if (showSettingsDialog && !settingsShownForCurrentCount) {
            AlertDialog(
                onDismissRequest = { /* allow dismiss or ignore */ },
                title = { Text("Permission Required") },
                text = {
                    Text(
                        "You've denied this permission $DENIALS_NEEDED_FOR_SETTINGS times. " +
                                "Please enable it in app settings."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSettingsDialog = false
                        settingsShownForCurrentCount = true
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                        onResult(false)
                    }) { Text("Settings") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showSettingsDialog = false
                        settingsShownForCurrentCount = true
                        onResult(false)
                    }) { Text("Cancel") }
                }
            )
        }

        return PermissionRequestState(
            isGranted = allGranted,
            RequestPermission = requestPermission
        )
    }

    class PermissionRequestState(
        val isGranted: Boolean,
        val RequestPermission: () -> Unit
    )
}
