package ru.tbank.core.tid.ui.customTabs

import android.net.Uri
import androidx.core.net.toUri
import ru.tbank.core.tid.api.TidApi

internal class TidCustomTabsAuthPresenter {

    fun buildCustomTabsAuthStartUri(uiData: TidCustomTabsUiData): Uri {
        return TidApi.buildWebAuthStartUrl(
            host = uiData.host,
            clientId = uiData.clientId,
            codeChallenge = uiData.codeChallenge,
            codeChallengeMethod = uiData.codeChallengeMethod,
            redirectUri = uiData.redirectUri,
            phone = uiData.phone,
        ).toUri()
    }
}
