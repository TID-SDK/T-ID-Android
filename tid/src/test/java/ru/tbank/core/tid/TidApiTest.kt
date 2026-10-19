package ru.tbank.core.tid

import android.net.Uri
import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.tbank.core.tid.api.TidApi

/**
 * @author k.voskrebentsev
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
internal class TidApiTest {

    @Test
    fun testWebAuthUrlCreationWithPhone() {
        val result = TidApi.buildWebAuthStartUrl(
            host = HOST,
            clientId = VALUE_CLIENT_ID,
            codeChallenge = VALUE_CODE_CHALLENGE,
            codeChallengeMethod = VALUE_CODE_CHALLENGE_METHOD,
            redirectUri = VALUE_REDIRECT_URI,
            phone = VALUE_PHONE,
        )

        val uri = Uri.parse(result)
        assertEquals(HOST, "${uri.scheme}://${uri.host}")
        assertEquals("/$PATH_AUTHORIZE", uri.path)
        uri.assertQueryParam(FIELD_CLIENT_ID, VALUE_CLIENT_ID)
        uri.assertQueryParam(FIELD_CODE_CHALLENGE, VALUE_CODE_CHALLENGE)
        uri.assertQueryParam(FIELD_CODE_CHALLENGE_METHOD, VALUE_CODE_CHALLENGE_METHOD)
        uri.assertQueryParam(FIELD_REDIRECT_URI, VALUE_REDIRECT_URI)
        uri.assertQueryParam(FIELD_RESPONSE_TYPE, VALUE_RESPONSE_TYPE)
        uri.assertQueryParam(FIELD_RESPONSE_MODE, VALUE_RESPONSE_MODE)
        uri.assertQueryParam(FIELD_PHONE, VALUE_PHONE)
    }

    @Test
    fun testWebAuthUrlCreationWithoutPhone() {
        val result = TidApi.buildWebAuthStartUrl(
            host = HOST,
            clientId = VALUE_CLIENT_ID,
            codeChallenge = VALUE_CODE_CHALLENGE,
            codeChallengeMethod = VALUE_CODE_CHALLENGE_METHOD,
            redirectUri = VALUE_REDIRECT_URI,
            phone = null,
        )

        val uri = Uri.parse(result)
        assertEquals(HOST, "${uri.scheme}://${uri.host}")
        assertEquals("/$PATH_AUTHORIZE", uri.path)
        uri.assertQueryParam(FIELD_CLIENT_ID, VALUE_CLIENT_ID)
        uri.assertQueryParam(FIELD_CODE_CHALLENGE, VALUE_CODE_CHALLENGE)
        uri.assertQueryParam(FIELD_CODE_CHALLENGE_METHOD, VALUE_CODE_CHALLENGE_METHOD)
        uri.assertQueryParam(FIELD_REDIRECT_URI, VALUE_REDIRECT_URI)
        uri.assertQueryParam(FIELD_RESPONSE_TYPE, VALUE_RESPONSE_TYPE)
        uri.assertQueryParam(FIELD_RESPONSE_MODE, VALUE_RESPONSE_MODE)
        assertNull(uri.getQueryParameter(FIELD_PHONE))
    }

    @Test
    fun testCodeParsing() {
        val url = Uri.parse(HOST).buildUpon()
            .appendQueryParameter(FIELD_CODE, VALUE_CODE)
            .build()
            .toString()

        val result = TidApi.parseCode(url)

        assertEquals(VALUE_CODE, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCodeAbsence() {
        TidApi.parseCode("")
    }

    private fun Uri.assertQueryParam(paramName: String, expectedValue: String) {
        val actualValue = getQueryParameter(paramName)
        assertEquals(expectedValue, actualValue)
    }

    companion object {
        private const val HOST = "https://id.tbank.ru"
        private const val PATH_AUTHORIZE = "auth/authorize"

        private const val FIELD_CLIENT_ID = "client_id"
        private const val FIELD_REDIRECT_URI = "redirect_uri"
        private const val FIELD_CODE_CHALLENGE = "code_challenge"
        private const val FIELD_CODE_CHALLENGE_METHOD = "code_challenge_method"
        private const val FIELD_RESPONSE_TYPE = "response_type"
        private const val FIELD_RESPONSE_MODE = "response_mode"
        private const val FIELD_CODE = "code"
        private const val FIELD_PHONE = "phone"

        private const val VALUE_CLIENT_ID = "client_id_value"
        private const val VALUE_CODE_CHALLENGE = "code_challenge_value"
        private const val VALUE_CODE_CHALLENGE_METHOD = "code_challenge_method_value"
        private const val VALUE_REDIRECT_URI = "redirect_uri_value"
        private const val VALUE_CODE = "code_value"
        private const val VALUE_RESPONSE_TYPE = "code"
        private const val VALUE_RESPONSE_MODE = "query"
        private const val VALUE_PHONE = "+79999999999"
    }
}
