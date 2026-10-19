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
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsAuthActivity
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsUiData
import java.util.regex.Pattern
import androidx.core.net.toUri

/**
 * @author Stanislav Mukhametshin
 */
internal class TidAppLinkUtil(private val context: Context) {

    companion object {
        private const val PARTNER_AUTH_CATEGORY = "ru.tinkoff.partner.TINKOFF_APP"

        private const val QUERY_PARAMETER_HOST = "host"
        private const val QUERY_PARAMETER_CLIENT_ID = "clientId"
        private const val QUERY_PARAMETER_CODE_CHALLENGE = "code_challenge"
        private const val QUERY_PARAMETER_CODE_CHALLENGE_METHOD = "code_challenge_method"
        private const val QUERY_PARAMETER_CALLBACK_URL = "callback_url"
        private const val QUERY_PARAMETER_PACKAGE = "package_name"
        private const val QUERY_PARAMETER_CODE = "code"
        private const val QUERY_PARAMETER_AUTH_STATUS_CODE = "auth_status_code"
        private const val QUERY_PARAMETER_REDIRECT_URI = "redirect_uri"
        private const val QUERY_PARAMETER_PHONE = "phone"
        private const val QUERY_PARTNER_SDK_VERSION = "partner_sdk_version"

        private const val AUTH_STATUS_CODE_SUCCESS = "success"
        private const val AUTH_STATUS_CODE_CANCELLED_BY_USER = "cancelled_by_user"

        @VisibleForTesting
        @RequiresApi(Build.VERSION_CODES.S)
        internal fun isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed: Boolean,
            hostToStateMap: Map<String, Int>,
            appDomain: String,
        ): Boolean {
            return isLinkHandlingAllowed && hostToStateMap[appDomain].isVerifiedOrSelectedDomainState()
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun Int?.isVerifiedOrSelectedDomainState(): Boolean {
            return this == DomainVerificationUserState.DOMAIN_STATE_VERIFIED ||
                    this == DomainVerificationUserState.DOMAIN_STATE_SELECTED
        }
    }

    fun createTBankAppAuthAppLink(
        appDomain: String,
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        packageName: String?,
        redirectUrl: String,
        partnerSdkVersion: String,
    ): Intent {
        return buildTBankAppAuthAppLink(
            appDomain = appDomain,
            clientId = clientId,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod,
            callbackUrl = redirectUrl.toUri(),
            packageName = packageName,
            redirectUrl = redirectUrl,
            partnerSdkVersion = partnerSdkVersion,
        )
    }

    fun createTBankAppAuthAppLink(
        appDomain: String,
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        callbackUrl: Uri,
        packageName: String?,
        redirectUrl: String,
        partnerSdkVersion: String,
    ): Intent {
        return buildTBankAppAuthAppLink(
            appDomain = appDomain,
            clientId = clientId,
            codeChallenge = codeChallenge,
            codeChallengeMethod = codeChallengeMethod,
            callbackUrl = callbackUrl,
            packageName = packageName,
            redirectUrl = redirectUrl,
            partnerSdkVersion = partnerSdkVersion,
        )
    }

    private fun buildTBankAppAuthAppLink(
        appDomain: String,
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        callbackUrl: Uri?,
        packageName: String?,
        redirectUrl: String,
        partnerSdkVersion: String,
    ): Intent {
        val builder = getBaseUri(appDomain).buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_CLIENT_ID, clientId)
            .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE, codeChallenge)
            .appendQueryParameter(QUERY_PARAMETER_CODE_CHALLENGE_METHOD, codeChallengeMethod)
            .appendQueryParameter(QUERY_PARAMETER_PACKAGE, packageName)
            .appendQueryParameter(QUERY_PARAMETER_REDIRECT_URI, redirectUrl)
            .appendQueryParameter(QUERY_PARTNER_SDK_VERSION, partnerSdkVersion)
        callbackUrl?.let { builder.appendQueryParameter(QUERY_PARAMETER_CALLBACK_URL, it.toString()) }
        val uri = builder.build()
        return Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }
    }

    fun createCustomTabsAuthIntent(
        context: Context,
        uiData: TidCustomTabsUiData,
    ): Intent {
        return Intent(context, TidCustomTabsAuthActivity::class.java)
            .putExtra(QUERY_PARAMETER_HOST, uiData.host)
            .putExtra(QUERY_PARAMETER_CLIENT_ID, uiData.clientId)
            .putExtra(QUERY_PARAMETER_CODE_CHALLENGE, uiData.codeChallenge)
            .putExtra(QUERY_PARAMETER_CODE_CHALLENGE_METHOD, uiData.codeChallengeMethod)
            .putExtra(QUERY_PARAMETER_REDIRECT_URI, uiData.redirectUri)
            .putExtra(QUERY_PARAMETER_PHONE, uiData.phone)
    }

    fun parseTidCustomTabsUiData(
        intent: Intent,
    ): TidCustomTabsUiData {
        return TidCustomTabsUiData(
            host = intent.requireStringExtra(QUERY_PARAMETER_HOST),
            clientId = intent.requireStringExtra(QUERY_PARAMETER_CLIENT_ID),
            codeChallenge = intent.requireStringExtra(QUERY_PARAMETER_CODE_CHALLENGE),
            codeChallengeMethod = intent.requireStringExtra(QUERY_PARAMETER_CODE_CHALLENGE_METHOD),
            redirectUri = intent.requireStringExtra(QUERY_PARAMETER_REDIRECT_URI),
            phone = intent.getStringExtra(QUERY_PARAMETER_PHONE),
        )
    }

    private fun Intent.requireStringExtra(key: String): String {
        return requireNotNull(getStringExtra(key)) { "the required web auth parameter is missing" }
    }

    fun isPossibleToHandleAppLink(appDomain: String): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            isPossibleToHandleAppLinkFor30AndBelow(appDomain)
        } else {
            isPossibleToHandleAppLinkFor31AndAbove(appDomain)
        }
    }

    fun isPhoneValidForWhiteLabel(phone: String): Boolean {
        return Pattern.compile("^\\+7\\d{10}").matcher(phone).matches()
    }

    private fun isPossibleToHandleAppLinkFor30AndBelow(appDomain: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = getBaseUri(appDomain)
            addCategory(PARTNER_AUTH_CATEGORY)
        }
        val availablePackages = getAvailablePackages(intent)

        return availablePackages.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun isPossibleToHandleAppLinkFor31AndAbove(appDomain: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = getBaseUri(appDomain)
            addCategory(PARTNER_AUTH_CATEGORY)
        }
        val availablePackages = getAvailablePackages(intent)

        return availablePackages.any { availablePackage ->
            checkAppLinkVerify(packageName = availablePackage, appDomain = appDomain)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun checkAppLinkVerify(packageName: String, appDomain: String): Boolean {
        val manager = context.getSystemService(DomainVerificationManager::class.java)
        val userState = manager.getDomainVerificationUserState(packageName) ?: return false

        return isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = userState.isLinkHandlingAllowed,
            hostToStateMap = userState.hostToStateMap,
            appDomain = appDomain,
        )
    }

    private fun getAvailablePackages(intent: Intent): List<String> {
        val flag = PackageManager.MATCH_DEFAULT_ONLY
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(
                    flag.toLong()
                )
            )
        } else {
            context.packageManager.queryIntentActivities(intent, flag)
        }

        return resolveInfo.map { it.activityInfo.packageName }
    }

    fun createBackAppCodeIntent(
        callbackUrl: String,
        code: String,
    ): Intent {
        val data = buildBackAppLinkWithStatus(callbackUrl, AUTH_STATUS_CODE_SUCCESS)
            .appendQueryParameter(QUERY_PARAMETER_CODE, code)
            .build()
        return Intent(Intent.ACTION_VIEW)
            .setData(data)
    }

    fun createBackAppCancelIntent(
        callbackUrl: String,
    ): Intent {
        val data = buildBackAppLinkWithStatus(callbackUrl, AUTH_STATUS_CODE_CANCELLED_BY_USER)
            .build()
        return Intent(Intent.ACTION_VIEW)
            .setData(data)
    }

    private fun buildBackAppLinkWithStatus(callbackUrl: String, status: String): Uri.Builder {
        return callbackUrl.toUri().buildUpon()
            .appendQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE, status)
    }

    fun getAuthCode(uri: Uri): String? = uri.getQueryParameter(QUERY_PARAMETER_CODE)

    fun getAuthStatusCode(uri: Uri): TidStatusCode? {
        val statusCode = uri.getQueryParameter(QUERY_PARAMETER_AUTH_STATUS_CODE)
        return when {
            statusCode != null -> authStatusCodesMap[statusCode]
            getAuthCode(uri) != null -> TidStatusCode.SUCCESS
            else -> null
        }
    }

    private fun getBaseUri(appDomain: String): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority(appDomain)
            .appendPath("partner_auth")
            .build()
    }

    private val authStatusCodesMap = mapOf(
        AUTH_STATUS_CODE_SUCCESS to TidStatusCode.SUCCESS,
        AUTH_STATUS_CODE_CANCELLED_BY_USER to TidStatusCode.CANCELLED_BY_USER
    )
}
