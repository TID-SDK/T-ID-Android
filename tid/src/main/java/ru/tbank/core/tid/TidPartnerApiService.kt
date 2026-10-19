/*
 * Copyright © 2024 T-Bank
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.tbank.core.tid

import okhttp3.Call
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import ru.tbank.core.tid.api.TidApi
import ru.tbank.core.tid.api.TidApi.Companion.TOKEN_HINT_TYPE_ACCESS_TOKEN
import ru.tbank.core.tid.api.TidApi.Companion.TOKEN_HINT_TYPE_REFRESH_TOKEN
import ru.tbank.core.tid.error.TidErrorMessage
import ru.tbank.core.tid.error.TidRequestException
import ru.tbank.core.tid.error.TidTokenErrorConstants
import ru.tbank.core.tid.error.TidTokenSignOutErrorConstants
import java.io.IOException

/**
 * @author Stanislav Mukhametshin
 */
internal class TidPartnerApiService(private val tidApi: TidApi) {

    fun getToken(code: String, codeVerifier: String, clientId: String, redirectUri: String): TidCall<TidTokenPayload> {
        return tidApi.getToken(code, codeVerifier, clientId, redirectUri).wrapTokenToTidCall()
    }

    fun refreshToken(refreshToken: String, clientId: String): TidCall<TidTokenPayload> {
        return tidApi.refreshToken(refreshToken, clientId).wrapTokenToTidCall()
    }

    fun revokeAccessToken(accessToken: String, clientId: String): TidCall<Unit> {
        return tidApi.revokeToken(accessToken, TOKEN_HINT_TYPE_ACCESS_TOKEN, clientId).wrapToTidCall(
            { /* left empty intentionally */ },
            { getRevokeErrorType(it) }
        )
    }

    fun revokeRefreshToken(refreshToken: String, clientId: String): TidCall<Unit> {
        return tidApi.revokeToken(refreshToken, TOKEN_HINT_TYPE_REFRESH_TOKEN, clientId).wrapToTidCall(
            { /* left empty intentionally */ },
            { getRevokeErrorType(it) }
        )
    }

    private fun Call.wrapTokenToTidCall(): TidCall<TidTokenPayload> {
        return wrapToTidCall({
            if (it.body == null) throw TidRequestException(IOException("Empty body $it"))
            val jsonObject = JSONObject(requireNotNull(it.body).string())
            TidTokenPayload(
                accessToken = jsonObject.getString("access_token"),
                expiresIn = jsonObject.getInt("expires_in"),
                idToken = jsonObject.optString("id_token"),
                refreshToken = jsonObject.opt("refresh_token")?.toString()
            )
        }, { getTokenErrorType(it) })
    }

    private fun getTokenErrorType(error: String): Int {
        return when (error) {
            "invalid_request" -> TidTokenErrorConstants.INVALID_REQUEST
            "invalid_client" -> TidTokenErrorConstants.INVALID_CLIENT
            "invalid_grant" -> TidTokenErrorConstants.INVALID_GRANT
            "unauthorized_client" -> TidTokenErrorConstants.UNAUTHORIZED_CLIENT
            "unsupported_grant_type" -> TidTokenErrorConstants.UNSUPPORTED_GRANT_TYPE
            "server_error" -> TidTokenErrorConstants.SERVER_ERROR
            "limit_exceeded" -> TidTokenErrorConstants.LIMIT_EXCEEDED
            else -> TidTokenErrorConstants.UNKNOWN_ERROR
        }
    }

    private fun getRevokeErrorType(error: String): Int {
        return when (error) {
            "invalid_request" -> TidTokenSignOutErrorConstants.INVALID_REQUEST
            "invalid_grant" -> TidTokenSignOutErrorConstants.INVALID_GRANT
            else -> TidTokenSignOutErrorConstants.UNKNOWN_ERROR
        }
    }

    private fun <T> Call.wrapToTidCall(
        responseMapping: (response: Response) -> T,
        errorMapping: (errorCode: String) -> Int
    ): TidCall<T> {
        return object : TidCall<T> {

            @Throws(TidRequestException::class)
            @Suppress("TooGenericExceptionCaught")
            override fun getResponse(): T {
                try {
                    val response = execute()
                    response.checkResponseOnErrors(errorMapping)
                    if (!response.isSuccessful) throw IOException("Unexpected response $response")
                    return responseMapping(response)
                } catch (e: Exception) {
                    throw if (e !is TidRequestException) TidRequestException(reason = e) else e
                }
            }

            override fun cancel() = this@wrapToTidCall.cancel()
        }
    }

    @Throws(TidRequestException::class)
    private fun Response.checkResponseOnErrors(errorMapping: (errorCode: String) -> Int) {
        if (isSuccessful) return
        val message = if (body != null) {
            try {
                val jsonObject = JSONObject(body!!.string())
                val error: String? = jsonObject.optString("error")
                val message: String? = jsonObject.optString("error_message")
                if (error != null) {
                    TidErrorMessage(message, errorMapping(error))
                } else null
            } catch (e: JSONException) {
                null
            }
        } else null
        throw TidRequestException(
            reason = IOException("Request problem $this"),
            message = "Request exception",
            errorMessage = message
        )
    }
}
