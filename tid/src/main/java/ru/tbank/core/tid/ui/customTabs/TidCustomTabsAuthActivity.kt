package ru.tbank.core.tid.ui.customTabs

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService
import ru.tbank.core.tid.TidAppLinkUtil
import ru.tbank.core.tid.error.TidLoggingConstants.LOG_TAG

internal class TidCustomTabsAuthActivity : Activity() {

    private val presenter: TidCustomTabsAuthPresenter by lazy { TidCustomTabsAuthPresenter() }
    private val appLinkUtil by lazy { TidAppLinkUtil(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiData = appLinkUtil.parseTidCustomTabsUiData(intent)
        val customTabsPackage = CustomTabsClient.getPackageName(this, getCustomTabsPackages())

        if (customTabsPackage == null) {
            finishWithCancellation(uiData.redirectUri)
            return
        }

        val authUri = presenter.buildCustomTabsAuthStartUri(uiData)
        launchCustomTabs(authUri, customTabsPackage, uiData.redirectUri)
    }

    private fun launchCustomTabs(authUri: Uri, customTabsPackage: String, redirectUri: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setSendToExternalDefaultHandlerEnabled(true)
            .build()
            .apply {
                intent.setPackage(customTabsPackage)
            }

        try {
            customTabsIntent.launchUrl(this, authUri)
            finish()
        } catch (e: ActivityNotFoundException) {
            Log.e(LOG_TAG, "error when trying to launch Custom Tabs", e)
            finishWithCancellation(redirectUri)
        }
    }

    @Suppress("DEPRECATION")
    private fun getCustomTabsPackages(): List<String> {
        return packageManager.queryIntentServices(
            Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION),
            0,
        )
            .mapNotNull { resolveInfo -> resolveInfo.serviceInfo?.packageName }
            .distinct()
    }

    private fun finishWithCancellation(redirectUri: String) {
        val intent = appLinkUtil.createBackAppCancelIntent(redirectUri)
            .setPackage(packageName)
        startActivitySafely(intent)
        finish()
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(LOG_TAG, "error when trying to return auth cancellation result", e)
        }
    }
}
