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

import android.os.Build
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.tbank.core.tid.api.TidApi
import ru.tbank.core.tid.error.TidRequestException
import ru.tbank.core.tid.error.TidTokenErrorConstants
import java.net.HttpURLConnection

/**
 * @author Stanislav Mukhametshin
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
public class TidPartnerApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var partnerApiService: TidPartnerApiService

    @Before
    public fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val tidApi = TidApi(OkHttpClient(), mockWebServer.url("/"))
        partnerApiService = TidPartnerApiService(tidApi)
    }

    @Test
    public fun testValidTokenGet() {
        val call = partnerApiService.getToken("test", "test", "test", "test")
        testTokenRequestResponsesValidation(call)
    }

    @Test
    public fun testValidOnlyAccessTokenGet() {
        val call = partnerApiService.getToken("test", "test", "test", "test")
        testAccessTokenRequestResponsesValidation(call)
    }

    @Test
    public fun testInvalidTokenGet() {
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .setBody(
                """{
                    "error": "invalid_request",
                    "error_message": "Some message"
                }""".trimIndent()
            )
        mockWebServer.enqueue(response)
        val call = partnerApiService.getToken("test", "test", "test", "test")
        val exception = assertThrows(TidRequestException::class.java) { call.getResponse() }
        assertThat(exception.errorMessage).isNotNull()
        val message = requireNotNull(exception.errorMessage) { "exception.errorMessage is null" }
        assertThat(message.errorType).isEqualTo(TidTokenErrorConstants.INVALID_REQUEST)
        assertThat(message.message).isEqualTo("Some message")
    }

    @Test
    public fun testValidTokenUpdate() {
        val call = partnerApiService.refreshToken("refresh_token", "test")
        testTokenRequestResponsesValidation(call)
    }

    @Test
    public fun testRevokeAccessToken() {
        testTokenRevokeValidation(partnerApiService.revokeAccessToken("test", "test"))
    }

    @Test
    public fun revokeRefreshToken() {
        testTokenRevokeValidation(partnerApiService.revokeRefreshToken("test", "test"))
    }

    private fun testTokenRequestResponsesValidation(call: TidCall<TidTokenPayload>) {
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(TOKEN_JSON)
        mockWebServer.enqueue(response)
        val payload = call.getResponse()
        assertThat(payload.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(payload.expiresIn).isEqualTo(EXPIRES_IN)
        assertThat(payload.idToken).isEqualTo(ID_TOKEN)
        assertThat(payload.refreshToken).isEqualTo(REFRESH_TOKEN)
    }

    private fun testAccessTokenRequestResponsesValidation(call: TidCall<TidTokenPayload>) {
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(ONLY_ACCESS_TOKEN_JSON)
        mockWebServer.enqueue(response)
        val payload = call.getResponse()
        assertThat(payload.accessToken).isEqualTo(ACCESS_TOKEN)
        assertThat(payload.expiresIn).isEqualTo(EXPIRES_IN)
        assertThat(payload.idToken).isEqualTo(ID_TOKEN)
        assertThat(payload.refreshToken).isNull()
    }

    private fun testTokenRevokeValidation(call: TidCall<Unit>) {
        val response = MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody("OK")
        mockWebServer.enqueue(response)
        assertThat(call.getResponse()).isEqualTo(Unit)
    }

    @After
    public fun tearDown() {
        mockWebServer.shutdown()
    }

    private companion object {

        private const val ACCESS_TOKEN = "DR_Y7iifsfsdfRKGuXtMovTocYD4MnA7RxhwAMX3ydRKeDOYFls4a1S4IC1Daq7poz2k2AoJIOICsgA"
        private const val EXPIRES_IN = 1834
        private const val ID_TOKEN = "yJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ"
        private const val REFRESH_TOKEN = "OuyFBEMG2yhjh0KHNIbeS8sb0VmBmdqd08rY52ZniOyDmCnn"

        private val TOKEN_JSON = """{
        	"access_token": "$ACCESS_TOKEN",
        	"token_type": "Bearer",
        	"expires_in": $EXPIRES_IN,
        	"id_token": "$ID_TOKEN",
        	"refresh_token": "$REFRESH_TOKEN"
        }""".trimIndent()

        private val ONLY_ACCESS_TOKEN_JSON = """{
        	"access_token": "$ACCESS_TOKEN",
        	"token_type": "Bearer",
        	"expires_in": $EXPIRES_IN,
        	"id_token": "$ID_TOKEN"
        }""".trimIndent()
    }
}
