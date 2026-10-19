package ru.tbank.core.tid

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsAuthActivity
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsUiData

/**
 * @author k.voskrebentsev
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
internal class TidAppLinkUtilTest {

    private val appLinkUtil = TidAppLinkUtil(ApplicationProvider.getApplicationContext())

    @Test
    fun testRedirectUriOnlyTBankAppAuthAppLinkCreationAddsCompatibilityCallbackUrl() {
        val result = appLinkUtil.createTBankAppAuthAppLink(
            appDomain = T_BANK_AUTH_HOST,
            clientId = VALUE_PARAMETER_CLIENT_ID,
            codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
            codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
            packageName = VALUE_PARAMETER_PACKAGE_NAME,
            redirectUrl = VALUE_PARAMETER_REDIRECT_URI,
            partnerSdkVersion = VALUE_PARAMETER_PARTNER_SDK_VERSION,
        )

        assertEquals(Intent.ACTION_VIEW, result.action)
        assertEquals(T_BANK_AUTH_HOST, result.data?.host)
        assertEquals(VALUE_PARAMETER_CLIENT_ID, result.data?.getQueryParameter(QUERY_PARAMETER_CLIENT_ID))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE, result.data?.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE_METHOD, result.data?.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD))
        assertEquals(VALUE_PARAMETER_PACKAGE_NAME, result.data?.getQueryParameter(QUERY_PARAMETER_PACKAGE))
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, result.data?.getQueryParameter(QUERY_PARAMETER_REDIRECT_URI))
        assertEquals(VALUE_PARAMETER_PARTNER_SDK_VERSION, result.data?.getQueryParameter(QUERY_PARTNER_SDK_VERSION))
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, result.data?.getQueryParameter(QUERY_PARAMETER_CALLBACK_URL))
    }

    @Test
    fun testDeprecatedCallbackUrlTBankAppAuthAppLinkCreation() {
        val result = appLinkUtil.createTBankAppAuthAppLink(
            appDomain = T_BANK_AUTH_HOST,
            clientId = VALUE_PARAMETER_CLIENT_ID,
            codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
            codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
            callbackUrl = Uri.parse(VALUE_PARAMETER_CALLBACK_URL),
            packageName = VALUE_PARAMETER_PACKAGE_NAME,
            redirectUrl = VALUE_PARAMETER_REDIRECT_URI,
            partnerSdkVersion = VALUE_PARAMETER_PARTNER_SDK_VERSION,
        )

        assertEquals(Intent.ACTION_VIEW, result.action)
        assertEquals(T_BANK_AUTH_HOST, result.data?.host)
        assertEquals(VALUE_PARAMETER_CLIENT_ID, result.data?.getQueryParameter(QUERY_PARAMETER_CLIENT_ID))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE, result.data?.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE_METHOD, result.data?.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD))
        assertEquals(VALUE_PARAMETER_CALLBACK_URL, result.data?.getQueryParameter(QUERY_PARAMETER_CALLBACK_URL))
        assertEquals(VALUE_PARAMETER_PACKAGE_NAME, result.data?.getQueryParameter(QUERY_PARAMETER_PACKAGE))
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, result.data?.getQueryParameter(QUERY_PARAMETER_REDIRECT_URI))
        assertEquals(VALUE_PARAMETER_PARTNER_SDK_VERSION, result.data?.getQueryParameter(QUERY_PARTNER_SDK_VERSION))
    }

    @Test
    fun testCustomTabsIntentCreationAndParsing() {
        val intent = appLinkUtil.createCustomTabsAuthIntent(
            context = ApplicationProvider.getApplicationContext(),
            uiData = TidCustomTabsUiData(
                host = VALUE_PARAMETER_HOST,
                clientId = VALUE_PARAMETER_CLIENT_ID,
                codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
                codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
                redirectUri = VALUE_PARAMETER_REDIRECT_URI,
                phone = VALUE_PARAMETER_PHONE,
            ),
        )

        val result = appLinkUtil.parseTidCustomTabsUiData(intent)

        assertEquals(TidCustomTabsAuthActivity::class.java.name, intent.component?.className)
        assertEquals(VALUE_PARAMETER_HOST, result.host)
        assertEquals(VALUE_PARAMETER_CLIENT_ID, result.clientId)
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE, result.codeChallenge)
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE_METHOD, result.codeChallengeMethod)
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, result.redirectUri)
        assertEquals(VALUE_PARAMETER_PHONE, result.phone)
        assertFalse(intent.hasExtra(QUERY_PARAMETER_CALLBACK_URL))
    }

    @Test
    fun testCodeIntentCreation() {
        val code = "code_value"

        val result = appLinkUtil.createBackAppCodeIntent(
            callbackUrl = VALUE_PARAMETER_CALLBACK_URL,
            code = code,
        )

        assertEquals(Intent.ACTION_VIEW, result.action)
        assertEquals(VALUE_PARAMETER_CALLBACK_URL, result.data?.path)
        assertEquals(code, result.data?.getQueryParameter(QUERY_PARAMETER_CODE))
        assertEquals(AUTH_STATUS_CODE_SUCCESS, result.data?.getQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE))
    }

    @Test
    fun testCancelIntentCreation() {
        val result = appLinkUtil.createBackAppCancelIntent(
            callbackUrl = VALUE_PARAMETER_CALLBACK_URL,
        )

        assertEquals(Intent.ACTION_VIEW, result.action)
        assertEquals(VALUE_PARAMETER_CALLBACK_URL, result.data?.path)
        assertEquals(AUTH_STATUS_CODE_CANCELLED_BY_USER, result.data?.getQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE))
    }

    @Test
    fun testStatusCodeFromRedirectUriResult() {
        val result = Uri.parse(VALUE_PARAMETER_REDIRECT_URI)
            .buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_CODE, VALUE_PARAMETER_CODE)
            .build()

        assertEquals(TidStatusCode.SUCCESS, appLinkUtil.getAuthStatusCode(result))
    }

    @Test
    fun testStatusCodeFromLegacyStatusHasPriorityOverCode() {
        val result = Uri.parse(VALUE_PARAMETER_REDIRECT_URI)
            .buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE, AUTH_STATUS_CODE_CANCELLED_BY_USER)
            .appendQueryParameter(QUERY_PARAMETER_CODE, VALUE_PARAMETER_CODE)
            .build()

        assertEquals(TidStatusCode.CANCELLED_BY_USER, appLinkUtil.getAuthStatusCode(result))
    }

    @Test
    fun testPhoneNumberValidation() {
        listOf(
            "+79999999999" to true,
            "+799999999990" to false,
            "+7999999999" to false,
            "+359999999999" to false,
            "79999999999" to false,
            "89999999999" to false,
            "" to false,
        ).forEach { (phone, isValid) ->
            assertEquals(isValid, appLinkUtil.isPhoneValidForWhiteLabel(phone))
        }
    }

    companion object {
        private const val T_BANK_AUTH_HOST = "www.tbank.ru"
        private const val QUERY_PARAMETER_CLIENT_ID = "clientId"
        private const val QUERY_PARAMETER_CODE_CHALLENGE = "code_challenge"
        private const val QUERY_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method"
        private const val QUERY_PARAMETER_CALLBACK_URL = "callback_url"
        private const val QUERY_PARAMETER_REDIRECT_URI = "redirect_uri"
        private const val QUERY_PARAMETER_PACKAGE = "package_name"
        private const val QUERY_PARTNER_SDK_VERSION = "partner_sdk_version"

        private const val VALUE_PARAMETER_HOST = "https://id.tbank.ru"
        private const val VALUE_PARAMETER_CLIENT_ID = "clientId_value"
        private const val VALUE_PARAMETER_CODE_CHALLENGE = "code_challenge_value"
        private const val VALUE_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method_value"
        private const val VALUE_PARAMETER_CALLBACK_URL = "callback_url_value"
        private const val VALUE_PARAMETER_REDIRECT_URI = "redirect_uri_value"
        private const val VALUE_PARAMETER_PHONE = "+79999999999"
        private const val VALUE_PARAMETER_PACKAGE_NAME = "package_name_value"
        private const val VALUE_PARAMETER_PARTNER_SDK_VERSION = "partner_sdk_version_value"
        private const val VALUE_PARAMETER_CODE = "code_value"

        private const val QUERY_PARAMETER_CODE = "code"
        private const val QUERY_PARAMETER_AUTH_STATUS_CODE = "auth_status_code"

        private const val AUTH_STATUS_CODE_SUCCESS = "success"
        private const val AUTH_STATUS_CODE_CANCELLED_BY_USER = "cancelled_by_user"
    }
}
