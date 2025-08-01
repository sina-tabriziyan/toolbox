package com.sina.library.response

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sina.library.response.UiText.*
import com.sina.library.toolbox.R

sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(
        @StringRes val id: Int,
        val args: Array<Any> = arrayOf()
    ) : UiText()

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> LocalContext.current.getString(id)
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> context.getString(id, *args)
        }
    }
}
fun DataError.asUiText(): UiText {
    return when (this) {
        // Network Errors
        DataError.Network.REQUEST_TIMEOUT -> StringResource(R.string.error_network_request_timeout)
        DataError.Network.TOO_MANY_REQUESTS -> StringResource(R.string.error_network_too_many_requests)
        DataError.Network.NO_INTERNET -> StringResource(R.string.error_network_no_internet)
        DataError.Network.SERVER_ERROR -> StringResource(R.string.error_network_server_error)
        DataError.Network.UNAUTHORIZED -> StringResource(R.string.error_network_unauthorized)
        DataError.Network.FORBIDDEN -> StringResource(R.string.error_network_forbidden)
        DataError.Network.NOT_FOUND -> StringResource(R.string.error_network_not_found)
        DataError.Network.BAD_REQUEST -> StringResource(R.string.error_network_bad_request)
        DataError.Network.UNKNOWN_HOST -> StringResource(R.string.error_network_unknown_host)
        DataError.Network.SSL_HANDSHAKE -> StringResource(R.string.error_network_ssl_handshake)
        DataError.Network.NETWORK_IO -> StringResource(R.string.error_network_io)
        DataError.Network.UNKNOWN_NETWORK_ERROR -> StringResource(R.string.error_network_unknown)

        // Local Storage Errors
        DataError.Local.DISK_FULL -> StringResource(R.string.error_local_disk_full)
        DataError.Local.FILE_NOT_FOUND -> StringResource(R.string.error_local_file_not_found)
        DataError.Local.IO_ERROR -> StringResource(R.string.error_local_io)
        DataError.Local.SECURITY_EXCEPTION -> StringResource(R.string.error_local_security)
        DataError.Local.DATABASE_ERROR -> StringResource(R.string.error_local_database)
        DataError.Local.SERIALIZATION -> StringResource(R.string.error_local_serialization)
        DataError.Local.DESERIALIZATION -> StringResource(R.string.error_local_deserialization)
        DataError.Local.OUT_OF_MEMORY -> StringResource(R.string.error_local_out_of_memory)
        DataError.Local.STORAGE_NOT_AVAILABLE -> StringResource(R.string.error_local_storage_unavailable)

        // Device Errors
        DataError.Device.LOW_BATTERY -> StringResource(R.string.error_device_low_battery)
        DataError.Device.OVERHEATED -> StringResource(R.string.error_device_overheated)
        DataError.Device.HARDWARE_FAILURE -> StringResource(R.string.error_device_hardware)
        DataError.Device.CAMERA_ERROR -> StringResource(R.string.error_device_camera)
        DataError.Device.SENSOR_ERROR -> StringResource(R.string.error_device_sensor)
        DataError.Device.GPS_UNAVAILABLE -> StringResource(R.string.error_device_gps_unavailable)

        // Permission Errors
        DataError.Permission.DENIED -> StringResource(R.string.error_permission_denied)
        DataError.Permission.PERMANENTLY_DENIED -> StringResource(R.string.error_permission_permanently_denied)
        DataError.Permission.RESTRICTED -> StringResource(R.string.error_permission_restricted)

        // Auth Errors
        DataError.Auth.INVALID_CREDENTIALS -> StringResource(R.string.error_auth_invalid_credentials)
        DataError.Auth.SESSION_EXPIRED -> StringResource(R.string.error_auth_session_expired)
        DataError.Auth.BIOMETRIC_ERROR -> StringResource(R.string.error_auth_biometric)
        DataError.Auth.ACCOUNT_LOCKED -> StringResource(R.string.error_auth_account_locked)
        DataError.Auth.AUTH_TOKEN_INVALID -> StringResource(R.string.error_auth_token_invalid)

        // UI/Render Errors
        DataError.Render.VIEW_NOT_ATTACHED -> StringResource(R.string.error_render_view_not_attached)
        DataError.Render.INVALID_VIEW_STATE -> StringResource(R.string.error_render_invalid_state)
        DataError.Render.LAYOUT_INFLATION_ERROR -> StringResource(R.string.error_render_layout_inflation)

        // General Errors
        DataError.General.UNKNOWN_ERROR -> StringResource(R.string.error_general_unknown)
        DataError.General.NOT_IMPLEMENTED -> StringResource(R.string.error_general_not_implemented)
        DataError.General.ILLEGAL_STATE -> StringResource(R.string.error_general_illegal_state)
        DataError.General.INVALID_ARGUMENT -> StringResource(R.string.error_general_invalid_argument)
        DataError.General.CONCURRENT_MODIFICATION -> StringResource(R.string.error_general_concurrent_modification)
        DataError.Network.SERIALIZATION -> StringResource(R.string.error_general_concurrent_modification)
        DataError.Network.PAYLOAD_TOO_LARGE -> StringResource(R.string.error_general_concurrent_modification)
        DataError.Network.NetworkConnectTimeoutError -> StringResource(R.string.error_general_concurrent_modification)
    }
}

fun Result.Error<*, DataError>.asErrorUiText() = error.asUiText()

