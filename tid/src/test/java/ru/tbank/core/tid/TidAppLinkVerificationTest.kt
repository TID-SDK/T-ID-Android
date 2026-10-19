package ru.tbank.core.tid

import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import androidx.annotation.RequiresApi
import org.junit.Assert.assertEquals
import org.junit.Test

@RequiresApi(Build.VERSION_CODES.S)
internal class TidAppLinkVerificationTest {

    @Test
    fun `returns true when partner host is verified and link handling is allowed`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = true,
            hostToStateMap = mapOf(T_BANK_AUTH_HOST to DomainVerificationUserState.DOMAIN_STATE_VERIFIED),
        )

        assertEquals(true, result)
    }

    @Test
    fun `returns true when partner host is selected and link handling is allowed`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = true,
            hostToStateMap = mapOf(T_BANK_AUTH_HOST to DomainVerificationUserState.DOMAIN_STATE_SELECTED),
        )

        assertEquals(true, result)
    }

    @Test
    fun `returns false when partner host is verified but link handling is disabled`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = false,
            hostToStateMap = mapOf(T_BANK_AUTH_HOST to DomainVerificationUserState.DOMAIN_STATE_VERIFIED),
        )

        assertEquals(false, result)
    }

    @Test
    fun `returns false when link handling is allowed but partner host is not verified`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = true,
            hostToStateMap = mapOf(T_BANK_AUTH_HOST to DomainVerificationUserState.DOMAIN_STATE_NONE),
        )

        assertEquals(false, result)
    }

    @Test
    fun `returns false when link handling is allowed but another host is verified`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = true,
            hostToStateMap = mapOf(OTHER_HOST to DomainVerificationUserState.DOMAIN_STATE_VERIFIED),
        )

        assertEquals(false, result)
    }

    @Test
    fun `returns false when link handling is allowed but host state is missing`() {
        val result = isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = true,
            hostToStateMap = emptyMap(),
        )

        assertEquals(false, result)
    }

    private fun isPartnerHostVerifiedAndAllowed(
        isLinkHandlingAllowed: Boolean,
        hostToStateMap: Map<String, Int>,
    ): Boolean {
        return TidAppLinkUtil.isPartnerHostVerifiedAndAllowed(
            isLinkHandlingAllowed = isLinkHandlingAllowed,
            hostToStateMap = hostToStateMap,
            appDomain = T_BANK_AUTH_HOST,
        )
    }

    private companion object {
        private const val T_BANK_AUTH_HOST = "www.tbank.ru"
        private const val OTHER_HOST = "www.example.com"
    }
}
