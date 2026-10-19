package ru.tbank.core.tid.ui.customTabs

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import ru.tbank.core.tid.TidAppLinkUtil

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
internal class TidCustomTabsAuthActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testLaunchesCustomTabsWhenAvailable() {
        addCustomTabsProvider()
        val launchIntent = TidAppLinkUtil(context).createCustomTabsAuthIntent(
            context = context,
            uiData = TidCustomTabsUiData(
                host = VALUE_PARAMETER_HOST,
                clientId = VALUE_PARAMETER_CLIENT_ID,
                codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
                codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
                redirectUri = VALUE_PARAMETER_REDIRECT_URI,
                phone = VALUE_PARAMETER_PHONE,
            ),
        )

        val activity = Robolectric.buildActivity(TidCustomTabsAuthActivity::class.java, launchIntent)
            .setup()
            .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        val authUri = requireNotNull(startedIntent.data)
        assertTrue(activity.isFinishing)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(CUSTOM_TABS_PACKAGE, startedIntent.`package`)
        assertTrue(CustomTabsIntent.isSendToExternalDefaultHandlerEnabled(startedIntent))
        assertEquals("$VALUE_PARAMETER_HOST/$PATH_AUTHORIZE", "${authUri.scheme}://${authUri.host}${authUri.path}")
        assertEquals(VALUE_PARAMETER_CLIENT_ID, authUri.getQueryParameter(QUERY_PARAMETER_CLIENT_ID))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE, authUri.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE))
        assertEquals(VALUE_PARAMETER_CODE_CHALLENGE_METHOD, authUri.getQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD))
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, authUri.getQueryParameter(QUERY_PARAMETER_REDIRECT_URI))
        assertEquals(VALUE_PARAMETER_PHONE, authUri.getQueryParameter(QUERY_PARAMETER_PHONE))
        assertNull(authUri.getQueryParameter(QUERY_PARAMETER_CALLBACK_URL))
    }

    @Test
    fun testLaunchesCustomTabsWhenDefaultViewHandlerIsResolverActivity() {
        addResolverOnlyDefaultViewHandler()
        addCustomTabsService(CUSTOM_TABS_PACKAGE)
        val launchIntent = TidAppLinkUtil(context).createCustomTabsAuthIntent(
            context = context,
            uiData = TidCustomTabsUiData(
                host = VALUE_PARAMETER_HOST,
                clientId = VALUE_PARAMETER_CLIENT_ID,
                codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
                codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
                redirectUri = VALUE_PARAMETER_REDIRECT_URI,
                phone = null,
            ),
        )

        val activity = Robolectric.buildActivity(TidCustomTabsAuthActivity::class.java, launchIntent)
            .setup()
            .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        val authUri = requireNotNull(startedIntent.data)
        assertTrue(activity.isFinishing)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(CUSTOM_TABS_PACKAGE, startedIntent.`package`)
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, authUri.getQueryParameter(QUERY_PARAMETER_REDIRECT_URI))
    }

    @Test
    fun testReturnsCancellationWhenCustomTabsUnavailable() {
        val launchIntent = TidAppLinkUtil(context).createCustomTabsAuthIntent(
            context = context,
            uiData = TidCustomTabsUiData(
                host = VALUE_PARAMETER_HOST,
                clientId = VALUE_PARAMETER_CLIENT_ID,
                codeChallenge = VALUE_PARAMETER_CODE_CHALLENGE,
                codeChallengeMethod = VALUE_PARAMETER_CODE_CHALLENGE_METHOD,
                redirectUri = VALUE_PARAMETER_REDIRECT_URI,
                phone = null,
            ),
        )

        val activity = Robolectric.buildActivity(TidCustomTabsAuthActivity::class.java, launchIntent)
            .setup()
            .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        val cancelUri = requireNotNull(startedIntent.data)
        assertTrue(activity.isFinishing)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals(context.packageName, startedIntent.`package`)
        assertEquals(VALUE_PARAMETER_REDIRECT_URI, cancelUri.buildUpon().clearQuery().build().toString())
        assertEquals(AUTH_STATUS_CODE_CANCELLED_BY_USER, cancelUri.getQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE))
    }

    @Suppress("DEPRECATION")
    private fun addCustomTabsProvider() {
        addDefaultViewHandler(CUSTOM_TABS_PACKAGE)
        addCustomTabsService(CUSTOM_TABS_PACKAGE)
    }

    @Suppress("DEPRECATION")
    private fun addDefaultViewHandler(packageName: String) {
        val packageManager = shadowOf(context.packageManager)
        packageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse(CUSTOM_TABS_DEFAULT_VIEW_URL)),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    this.packageName = packageName
                    name = "$packageName.BrowserActivity"
                }
            },
        )
    }

    @Suppress("DEPRECATION")
    private fun addCustomTabsService(packageName: String) {
        val packageManager = shadowOf(context.packageManager)
        packageManager.addResolveInfoForIntent(
            Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION),
            createCustomTabsServiceResolveInfo(packageName),
        )
        packageManager.addResolveInfoForIntent(
            Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).setPackage(packageName),
            createCustomTabsServiceResolveInfo(packageName),
        )
    }

    private fun createCustomTabsServiceResolveInfo(packageName: String): ResolveInfo {
        return ResolveInfo().apply {
            serviceInfo = ServiceInfo().apply {
                this.packageName = packageName
                name = "$packageName.CustomTabsService"
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun addResolverOnlyDefaultViewHandler() {
        val packageManager = shadowOf(context.packageManager)
        packageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse(CUSTOM_TABS_DEFAULT_VIEW_URL)),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = RESOLVER_PACKAGE
                    name = RESOLVER_ACTIVITY
                }
            },
        )
    }

    private companion object {

        private const val CUSTOM_TABS_PACKAGE = "com.example.browser"
        private const val RESOLVER_PACKAGE = "android"
        private const val RESOLVER_ACTIVITY = "com.android.internal.app.ResolverActivity"
        private const val CUSTOM_TABS_DEFAULT_VIEW_URL = "http://"
        private const val VALUE_PARAMETER_HOST = "https://id.tbank.ru"
        private const val VALUE_PARAMETER_CLIENT_ID = "client_id_value"
        private const val VALUE_PARAMETER_CODE_CHALLENGE = "code_challenge_value"
        private const val VALUE_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method_value"
        private const val VALUE_PARAMETER_REDIRECT_URI = "mobile://redirect"
        private const val VALUE_PARAMETER_PHONE = "+79999999999"

        private const val PATH_AUTHORIZE = "auth/authorize"
        private const val QUERY_PARAMETER_CLIENT_ID = "client_id"
        private const val QUERY_PARAMETER_CODE_CHALLENGE = "code_challenge"
        private const val QUERY_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method"
        private const val QUERY_PARAMETER_REDIRECT_URI = "redirect_uri"
        private const val QUERY_PARAMETER_PHONE = "phone"
        private const val QUERY_PARAMETER_CALLBACK_URL = "callback_url"
        private const val QUERY_PARAMETER_AUTH_STATUS_CODE = "auth_status_code"

        private const val AUTH_STATUS_CODE_CANCELLED_BY_USER = "cancelled_by_user"
    }
}
