package com.sina.library.response

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerializationException

suspend inline fun <reified T> safeApiCall(
    noinline apiCall: suspend () -> HttpResponse,
    successCodes: List<HttpStatusCode> = listOf(HttpStatusCode.OK, HttpStatusCode.Found)
): Result<ApiSuccess<T>, DataError.Network> {
    return try {
        val response = apiCall()
        val status = response.status
        val body = try {
            response.body<T>()
        } catch (e: SerializationException) {
            null
        }

        if (status in successCodes)
            if (body != null) Result.Success(ApiSuccess(status.value, body))
            else Result.Error(DataError.Network.SERIALIZATION)
        else
            Result.Error(
                when (status) {
                    HttpStatusCode.RequestTimeout -> DataError.Network.REQUEST_TIMEOUT
                    HttpStatusCode.TooManyRequests -> DataError.Network.TOO_MANY_REQUESTS
                    HttpStatusCode.PayloadTooLarge -> DataError.Network.PAYLOAD_TOO_LARGE
                    HttpStatusCode.Unauthorized -> DataError.Network.UNAUTHORIZED
                    HttpStatusCode.Forbidden -> DataError.Network.FORBIDDEN
                    HttpStatusCode.NotFound -> DataError.Network.NOT_FOUND
                    HttpStatusCode.BadRequest -> DataError.Network.BAD_REQUEST
                    else -> DataError.Network.UNKNOWN_NETWORK_ERROR
                }
            )
    } catch (e: ClientRequestException) {
        Result.Error(
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> DataError.Network.UNAUTHORIZED
                HttpStatusCode.Forbidden -> DataError.Network.FORBIDDEN
                HttpStatusCode.NotFound -> DataError.Network.NOT_FOUND
                HttpStatusCode.BadRequest -> DataError.Network.BAD_REQUEST
                else -> DataError.Network.UNKNOWN_NETWORK_ERROR
            }
        )
    } catch (e: ServerResponseException) {
        Result.Error(DataError.Network.SERVER_ERROR)
    } catch (e: SerializationException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: NoTransformationFoundException) {
        Result.Error(DataError.Network.SERIALIZATION)
    } catch (e: RedirectResponseException) {
        Result.Error(DataError.Network.UNKNOWN_NETWORK_ERROR)
    } catch (e: IOException) {
        Result.Error(DataError.Network.NO_INTERNET)
    } catch (e: Exception) {
        Result.Error(DataError.Network.UNKNOWN_NETWORK_ERROR)
    }
}