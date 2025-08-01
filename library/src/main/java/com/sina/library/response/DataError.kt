package com.sina.library.response

sealed interface DataError : Error {
    // Network-related errors
    enum class Network : DataError {
        REQUEST_TIMEOUT,       // When the request times out
        TOO_MANY_REQUESTS,    // When rate limiting is applied (HTTP 429)
        NO_INTERNET,          // When there's no internet connection
        SERVER_ERROR,        // When server returns 5xx error
        UNAUTHORIZED,        // When authentication fails (HTTP 401)
        FORBIDDEN,          // When access is denied (HTTP 403)
        NOT_FOUND,        // When resource is not found (HTTP 404)
        BAD_REQUEST,     // When the request is malformed (HTTP 400)
        UNKNOWN_HOST,   // When DNS resolution fails
        SSL_HANDSHAKE,  // When SSL handshake fails
        NETWORK_IO,    // General network I/O error
        UNKNOWN_NETWORK_ERROR, // Catch-all for other network issues

        SERIALIZATION,
        PAYLOAD_TOO_LARGE,

        NetworkConnectTimeoutError
    }

    // Local storage errors
    enum class Local : DataError {
        DISK_FULL,               // When device storage is full
        FILE_NOT_FOUND,         // When trying to access non-existent file
        IO_ERROR,             // General I/O error
        SECURITY_EXCEPTION,   // When security permissions are missing
        DATABASE_ERROR,      // SQLite or Room database error
        SERIALIZATION,     // Error serializing/deserializing data
        DESERIALIZATION,
        OUT_OF_MEMORY,    // When memory allocation fails
        STORAGE_NOT_AVAILABLE // When storage is not mounted or available
    }

    // Device/hardware related errors
    enum class Device : DataError {
        LOW_BATTERY,           // When battery is too low for operation
        OVERHEATED,            // When device overheats
        HARDWARE_FAILURE,     // General hardware failure
        CAMERA_ERROR,        // Camera service failure
        SENSOR_ERROR,       // When sensors fail
        GPS_UNAVAILABLE    // When GPS/location services are unavailable
    }

    // Permission related errors
    enum class Permission : DataError {
        DENIED,             // When user denied permission
        PERMANENTLY_DENIED, // When user selected "Don't ask again"
        RESTRICTED         // When permission is restricted by policy
    }

    // Authentication/security errors
    enum class Auth : DataError {
        INVALID_CREDENTIALS,
        SESSION_EXPIRED,
        BIOMETRIC_ERROR,
        ACCOUNT_LOCKED,
        AUTH_TOKEN_INVALID
    }

    // UI/rendering errors
    enum class Render : DataError {
        VIEW_NOT_ATTACHED,
        INVALID_VIEW_STATE,
        LAYOUT_INFLATION_ERROR
    }

    // Other general errors
    enum class General : DataError {
        UNKNOWN_ERROR,
        NOT_IMPLEMENTED,
        ILLEGAL_STATE,
        INVALID_ARGUMENT,
        CONCURRENT_MODIFICATION
    }
}