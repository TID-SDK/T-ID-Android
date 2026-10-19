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

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsAuthActivity
import java.net.HttpURLConnection
import java.net.URLDecoder

/**
 * @author Stanislav Mukhametshin
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
public class TidAuthTest {

    private var context: Context = ApplicationProvider.getApplicationContext()
    private val tidAuth: TidAuth = TidAuth(context, CLIENT_ID, REDIRECT_URL)
    private val testUri: Uri = Uri.Builder()
        .scheme("https")
        .authority("www.partner.com")
        .appendPath("partner")
        .build()

    @Test
    public fun testRedirectUriOnlyIntentCreationAddsCompatibilityCallbackUrl() {
        val intent = tidAuth.createTBankAppAuthIntent()
        assertThat(intent.data).isNotNull()
        val queryParam = { paramName: String -> requireNotNull(intent.data) { "intent.data is null" }.getQueryParameter(paramName) }
        assertThat(queryParam("clientId")).isEqualTo(CLIENT_ID)
        assertThat(queryParam("code_challenge")).isNotEmpty()
        assertThat(queryParam("code_challenge_method")).isNotEmpty()
        assertThat(queryParam("redirect_uri")).isEqualTo(REDIRECT_URL)
        assertThat(queryParam("callback_url")).isEqualTo(REDIRECT_URL)
        assertThat(queryParam("package_name")).isNotEmpty()
        assertThat(queryParam("partner_sdk_version")).isEqualTo(BuildConfig.VERSION_NAME)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun testDeprecatedCallbackUrlIntentCreation() {
        val intent = tidAuth.createTBankAppAuthIntent(testUri)
        assertThat(intent.data).isNotNull()
        val queryParam = { paramName: String -> requireNotNull(intent.data) { "intent.data is null" }.getQueryParameter(paramName) }
        assertThat(queryParam("clientId")).isEqualTo(CLIENT_ID)
        assertThat(queryParam("code_challenge")).isNotEmpty()
        assertThat(queryParam("code_challenge_method")).isNotEmpty()
        assertThat(queryParam("redirect_uri")).isEqualTo(REDIRECT_URL)
        assertThat(queryParam("callback_url")).isEqualTo(testUri.toString())
        assertThat(queryParam("package_name")).isNotEmpty()
        assertThat(queryParam("partner_sdk_version")).isEqualTo(BuildConfig.VERSION_NAME)
    }

    @Test
    public fun testRedirectUriOnlyWebAuthIntentCreation() {
        val intent = tidAuth.createTidWebAuthIntent()
        assertThat(intent.extras).isNotNull()
        val queryParam = { paramName: String -> requireNotNull(intent.getStringExtra(paramName)) { "$paramName is null" } }
        assertThat(intent.component?.className).isEqualTo(TidCustomTabsAuthActivity::class.java.name)
        assertThat(queryParam("clientId")).isEqualTo(CLIENT_ID)
        assertThat(queryParam("code_challenge")).isNotEmpty()
        assertThat(queryParam("code_challenge_method")).isNotEmpty()
        assertThat(queryParam("redirect_uri")).isEqualTo(REDIRECT_URL)
        assertThat(intent.hasExtra("callback_url")).isFalse()
    }

    @Suppress("DEPRECATION")
    @Test
    public fun testDeprecatedCallbackUrlWebAuthIntentCreation() {
        val intent = tidAuth.createTidWebViewAuthIntent(testUri)
        assertThat(intent.extras).isNotNull()
        val queryParam = { paramName: String -> requireNotNull(intent.getStringExtra(paramName)) { "$paramName is null" } }
        assertThat(intent.component?.className).isEqualTo(TidCustomTabsAuthActivity::class.java.name)
        assertThat(queryParam("clientId")).isEqualTo(CLIENT_ID)
        assertThat(queryParam("code_challenge")).isNotEmpty()
        assertThat(queryParam("code_challenge_method")).isNotEmpty()
        assertThat(queryParam("redirect_uri")).isEqualTo(testUri.toString())
    }

    @Test
    public fun testWhiteLabelWebAuthIntentCreation() {
        val intent = tidAuth.createTidWebAuthIntent(TidWebMode.WhiteLabel(phone = PHONE))
        val queryParam = { paramName: String -> requireNotNull(intent.getStringExtra(paramName)) { "$paramName is null" } }
        assertThat(intent.component?.className).isEqualTo(TidCustomTabsAuthActivity::class.java.name)
        assertThat(queryParam("redirect_uri")).isEqualTo(REDIRECT_URL)
        assertThat(queryParam("phone")).isEqualTo(PHONE)
    }

    @Test
    public fun testStatusWhenReturningBack() {
        listOf(AUTH_STATUS_CODE_SUCCESS to TidStatusCode.SUCCESS, AUTH_STATUS_CODE_CANCELLED_BY_USER to TidStatusCode.CANCELLED_BY_USER)
            .forEach { (param, status) ->
                val uri = testUri.buildUpon().appendQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE, param).build()
                assertThat(tidAuth.getStatusCode(uri)).isEqualTo(status)
            }

        val redirectUriResult = testUri.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_CODE, AUTH_CODE)
            .build()
        assertThat(tidAuth.getStatusCode(redirectUriResult)).isEqualTo(TidStatusCode.SUCCESS)

        val legacyStatusWithCode = testUri.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE, AUTH_STATUS_CODE_CANCELLED_BY_USER)
            .appendQueryParameter(QUERY_PARAMETER_CODE, AUTH_CODE)
            .build()
        assertThat(tidAuth.getStatusCode(legacyStatusWithCode)).isEqualTo(TidStatusCode.CANCELLED_BY_USER)
    }

    @Suppress("DEPRECATION")
    @Test
    public fun testTokenPayloadUsesStoredAuthSessionRedirectUri() {
        clearTidPrefs()
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val initialTidAuth = TidAuth(
                context = context,
                clientId = CLIENT_ID,
                redirectUri = INITIAL_REDIRECT_URL,
                tidApiHost = mockWebServer.url("/").toString(),
            )
            initialTidAuth.createTBankAppAuthIntent(testUri)

            val changedTidAuth = TidAuth(
                context = context,
                clientId = CLIENT_ID,
                redirectUri = CHANGED_REDIRECT_URL,
                tidApiHost = mockWebServer.url("/").toString(),
            )
            mockWebServer.enqueue(tokenResponse())

            changedTidAuth.getTidTokenPayload(authCodeUri()).getResponse()

            val requestBodyParams = mockWebServer.takeRequest().body.readUtf8().toFormParams()
            assertThat(requestBodyParams["code"]).isEqualTo(AUTH_CODE)
            assertThat(requestBodyParams["code_verifier"]).isNotEmpty()
            assertThat(requestBodyParams["redirect_uri"]).isEqualTo(INITIAL_REDIRECT_URL)
        } finally {
            mockWebServer.shutdown()
            clearTidPrefs()
        }
    }

    @Suppress("DEPRECATION")
    @Test
    public fun testDeprecatedWebAuthTokenPayloadUsesCallbackUrlAsAuthSessionRedirectUri() {
        clearTidPrefs()
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val tidAuth = TidAuth(
                context = context,
                clientId = CLIENT_ID,
                redirectUri = REDIRECT_URL,
                tidApiHost = mockWebServer.url("/").toString(),
            )
            tidAuth.createTidWebViewAuthIntent(testUri)
            mockWebServer.enqueue(tokenResponse())

            tidAuth.getTidTokenPayload(authCodeUri()).getResponse()

            val requestBodyParams = mockWebServer.takeRequest().body.readUtf8().toFormParams()
            assertThat(requestBodyParams["redirect_uri"]).isEqualTo(testUri.toString())
        } finally {
            mockWebServer.shutdown()
            clearTidPrefs()
        }
    }

    @Test
    public fun testTokenPayloadFallsBackToCurrentRedirectUriForLegacyAuthSession() {
        clearTidPrefs()
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(CODE_VERIFIER, LEGACY_CODE_VERIFIER)
            .commit()
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            val tidAuth = TidAuth(
                context = context,
                clientId = CLIENT_ID,
                redirectUri = REDIRECT_URL,
                tidApiHost = mockWebServer.url("/").toString(),
            )
            mockWebServer.enqueue(tokenResponse())

            tidAuth.getTidTokenPayload(authCodeUri()).getResponse()

            val requestBodyParams = mockWebServer.takeRequest().body.readUtf8().toFormParams()
            assertThat(requestBodyParams["code_verifier"]).isEqualTo(LEGACY_CODE_VERIFIER)
            assertThat(requestBodyParams["redirect_uri"]).isEqualTo(REDIRECT_URL)
        } finally {
            mockWebServer.shutdown()
            clearTidPrefs()
        }
    }

    private fun authCodeUri(): Uri {
        return testUri.buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_CODE, AUTH_CODE)
            .build()
    }

    private fun tokenResponse(): MockResponse {
        return MockResponse()
            .setResponseCode(HttpURLConnection.HTTP_OK)
            .setBody(TOKEN_JSON)
    }

    private fun String.toFormParams(): Map<String, String> {
        return split("&").associate { param ->
            val parts = param.split("=", limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts[1], "UTF-8")
        }
    }

    private fun clearTidPrefs() {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {

        private const val CLIENT_ID = "testClientId"
        private const val REDIRECT_URL = "mobile://redirectUrl"
        private const val INITIAL_REDIRECT_URL = "mobile://initialRedirectUrl"
        private const val CHANGED_REDIRECT_URL = "mobile://changedRedirectUrl"
        private const val QUERY_PARAMETER_AUTH_STATUS_CODE = "auth_status_code"
        private const val QUERY_PARAMETER_CODE = "code"

        private const val AUTH_STATUS_CODE_SUCCESS = "success"
        private const val AUTH_STATUS_CODE_CANCELLED_BY_USER = "cancelled_by_user"
        private const val AUTH_CODE = "auth_code"
        private const val LEGACY_CODE_VERIFIER = "legacy_code_verifier"
        private const val PHONE = "+79999999999"

        private const val PREFS_FILENAME = "prefs_tid_partner"
        private const val CODE_VERIFIER = "code_verifier"

        private const val TOKEN_JSON = """
            {
                "access_token": "access_token",
                "expires_in": 100,
                "id_token": "id_token",
                "refresh_token": "refresh_token"
            }
        """
    }
}
