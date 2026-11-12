package ru.tbank.core.tid

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import ru.tbank.core.tid.ui.customTabs.TidCustomTabsAuthActivity
import java.util.UUID

@Config(sdk = [Build.VERSION_CODES.S], shadows = [TidAppLinkAvailabilityTest.ShadowDomainVerificationManager::class])
@RunWith(RobolectricTestRunner::class)
internal class TidAppLinkAvailabilityTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val appLinkUtil = TidAppLinkUtil(context)
    private val manager = Shadow.newInstanceOf(DomainVerificationManager::class.java)
    private val domainVerificationManager: ShadowDomainVerificationManager =
        Shadow.extract(manager)

    @Before
    fun setUp() {
        Shadow.extract<ShadowContextImpl>(context.baseContext)
            .setSystemService(Context.DOMAIN_VERIFICATION_SERVICE, manager)
    }

    @Test
    fun testAppAuthUnavailableWhenResolvedPackageCannotBeFound() {
        addAuthPackage(MISSING_PACKAGE)
        domainVerificationManager.missingPackages.add(MISSING_PACKAGE)

        assertFalse(appLinkUtil.isPossibleToHandleAppLink(T_BANK_AUTH_HOST))
    }

    @Test
    fun testAppAuthChecksAnotherPackageWhenFirstPackageCannotBeFound() {
        addAuthPackage(MISSING_PACKAGE)
        addAuthPackage(AVAILABLE_PACKAGE)
        domainVerificationManager.missingPackages.add(MISSING_PACKAGE)
        domainVerificationManager.userStates[AVAILABLE_PACKAGE] = verifiedUserState(AVAILABLE_PACKAGE)

        assertTrue(appLinkUtil.isPossibleToHandleAppLink(T_BANK_AUTH_HOST))
    }

    @Test
    fun testAuthFallsBackToCustomTabsWhenResolvedPackageCannotBeFound() {
        addAuthPackage(MISSING_PACKAGE)
        domainVerificationManager.missingPackages.add(MISSING_PACKAGE)

        val intent = TidAuth(context, CLIENT_ID, REDIRECT_URI).createTidAuthIntent()

        assertEquals(TidCustomTabsAuthActivity::class.java.name, intent.component?.className)
        assertEquals(CLIENT_ID, intent.getStringExtra("clientId"))
        assertEquals(REDIRECT_URI, intent.getStringExtra("redirect_uri"))
    }

    @Test(expected = SecurityException::class)
    fun testOtherVerificationErrorsAreNotSuppressed() {
        addAuthPackage(AVAILABLE_PACKAGE)
        domainVerificationManager.failure = SecurityException("Domain verification denied")

        appLinkUtil.isPossibleToHandleAppLink(T_BANK_AUTH_HOST)
    }

    private fun addAuthPackage(packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://$T_BANK_AUTH_HOST/partner_auth"))
            .addCategory("ru.tinkoff.partner.TINKOFF_APP")
        val resolveInfo = ResolveInfo().apply {
            isDefault = true
            activityInfo = ActivityInfo().apply {
                this.packageName = packageName
                name = "AuthActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(intent, resolveInfo)
    }

    private fun verifiedUserState(packageName: String): DomainVerificationUserState {
        return ReflectionHelpers.callConstructor(
            DomainVerificationUserState::class.java,
            ClassParameter.from(UUID::class.java, UUID.randomUUID()),
            ClassParameter.from(String::class.java, packageName),
            ClassParameter.from(UserHandle::class.java, Process.myUserHandle()),
            ClassParameter.from(Boolean::class.javaPrimitiveType, true),
            ClassParameter.from(Map::class.java, mapOf(T_BANK_AUTH_HOST to DomainVerificationUserState.DOMAIN_STATE_VERIFIED)),
        )
    }

    @Implements(value = DomainVerificationManager::class, minSdk = Build.VERSION_CODES.S)
    class ShadowDomainVerificationManager {
        val missingPackages: MutableSet<String> = mutableSetOf()
        val userStates: MutableMap<String, DomainVerificationUserState> = mutableMapOf()
        var failure: Exception? = null

        @Implementation
        fun getDomainVerificationUserState(packageName: String): DomainVerificationUserState? {
            failure?.let { throw it }
            if (packageName in missingPackages) {
                throw PackageManager.NameNotFoundException(packageName)
            }
            return userStates[packageName]
        }
    }

    private companion object {
        private const val T_BANK_AUTH_HOST = "www.tbank.ru"
        private const val MISSING_PACKAGE = "com.example.missingbank"
        private const val AVAILABLE_PACKAGE = "com.example.availablebank"
        private const val CLIENT_ID = "testClientId"
        private const val REDIRECT_URI = "mobile://redirectUrl"
    }
}
