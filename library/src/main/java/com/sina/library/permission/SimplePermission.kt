package com.sina.library.permission

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

object SimplePermission {
    private const val PREFS_NAME = "simple_perm_prefs"
    private const val COUNT_PREFIX = "perm_count_"
    private const val DENIALS_NEEDED_FOR_SETTINGS = 2

    private lateinit var prefs: SharedPreferences

    fun register(application: Application) {
        prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Compose permission request handler.
     *
     * Usage inside your Composable:
     * val permissionState = SimplePermissionCompose.rememberPermissionRequest(
     *    permissions = arrayOf(Manifest.permission.CAMERA)
     * )
     *
     * LaunchedEffect(permissionState.isGranted) {
     *    if (permissionState.isGranted) { // do something }
     * }
     *
     * permissionState.RequestPermission()  // triggers request dialog
     */
    @Composable
    fun rememberPermissionRequest(
        permissions: Array<String>,
        onResult: (Boolean) -> Unit = {}
    ): PermissionRequestState {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        if (!this::prefs.isInitialized) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        // Compose state for denial count and whether to show settings dialog
        var denialCount by remember {
            mutableStateOf(prefs.getInt(COUNT_PREFIX + permissions.joinToString("_"), 0))
        }

        var showSettingsDialog by remember { mutableStateOf(false) }
        var showRationaleDialog by remember { mutableStateOf(false) }
        var resultDelivered by remember { mutableStateOf(false) }

        // Check if permissions already granted
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { resultMap ->
            if (resultDelivered) return@rememberLauncherForActivityResult // ignore repeated

            val grantedAll = resultMap.values.all { it }

            coroutineScope.launch {
                val key = COUNT_PREFIX + permissions.joinToString("_")

                if (grantedAll) {
                    // reset denial count on success
                    prefs.edit().remove(key).apply()
                    denialCount = 0
                    resultDelivered = true
                    onResult(true)
                } else {
                    // increment denial count
                    denialCount++
                    prefs.edit().putInt(key, denialCount).apply()

                    if (denialCount >= DENIALS_NEEDED_FOR_SETTINGS) {
                        showSettingsDialog = true
                    } else {
                        // Possibly show rationale if needed (optional)
                        showRationaleDialog = true
                    }
                    resultDelivered = true
                    onResult(false)
                }
            }
        }

        val requestPermission: () -> Unit = {
            if (allGranted) {
                // already granted
                onResult(true)
            } else {
                resultDelivered = false
                launcher.launch(permissions)
            }
        }

        // Compose UI for dialogs
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { /* prevent dismiss? */ },
                title = { Text("Permission Required") },
                text = { Text("You've denied this permission $DENIALS_NEEDED_FOR_SETTINGS times. Please enable it in app settings.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSettingsDialog = false
                        // Open app settings
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
                        onResult(false)
                    }) { Text("Cancel") }
                }
            )
        }

        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("Permission Needed") },
                text = { Text("This permission is needed for proper app functionality. Please allow it.") },
                confirmButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        requestPermission()
                    }) { Text("Allow") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showRationaleDialog = false
                        onResult(false)
                    }) { Text("Deny") }
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
