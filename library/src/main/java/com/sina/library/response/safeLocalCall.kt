package com.sina.library.response

import io.ktor.utils.io.errors.IOException

suspend fun <T> safeLocalCall(
    operation: suspend () -> T
): Result<T, DataError.Local> {
    return try {
        Result.Success(operation())
    } catch (e: SecurityException) {
        Result.Error(DataError.Local.SECURITY_EXCEPTION)  // خطای دسترسی
    } catch (e: IOException) {
        when {
            e.message?.contains("No space left", ignoreCase = true) == true ->
                Result.Error(DataError.Local.DISK_FULL)
            e.message?.contains("No such file", ignoreCase = true) == true ->
                Result.Error(DataError.Local.FILE_NOT_FOUND)
            else -> Result.Error(DataError.Local.IO_ERROR)
        }
    } catch (e: OutOfMemoryError) {
        Result.Error(DataError.Local.OUT_OF_MEMORY)
    } catch (e: android.database.SQLException) {
        Result.Error(DataError.Local.DATABASE_ERROR)
    } catch (e: kotlinx.serialization.SerializationException) {
        Result.Error(DataError.Local.SERIALIZATION)
    }  catch (e: Exception) {
        Result.Error(DataError.Local.IO_ERROR)
    }
}
