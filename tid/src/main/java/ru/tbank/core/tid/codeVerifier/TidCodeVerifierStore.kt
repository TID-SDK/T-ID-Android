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

package ru.tbank.core.tid.codeVerifier

import android.content.Context
import android.content.SharedPreferences

/**
 * @author Stanislav Mukhametshin
 */
internal class TidCodeVerifierStore(context: Context) {

    private val preference: SharedPreferences by lazy(mode = LazyThreadSafetyMode.PUBLICATION) {
        context.getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
    }

    internal fun saveAuthSession(authSession: TidAuthSession) {
        preference.edit()
            .putString(CODE_VERIFIER, authSession.codeVerifier)
            .putString(AUTH_SESSION_REDIRECT_URI, authSession.authSessionRedirectUri)
            .apply()
    }

    internal fun getAuthSession(defaultRedirectUri: String): TidAuthSession {
        return TidAuthSession(
            codeVerifier = requireNotNull(preference.getString(CODE_VERIFIER, "")) { "$CODE_VERIFIER is null" },
            authSessionRedirectUri = preference.getString(AUTH_SESSION_REDIRECT_URI, null) ?: defaultRedirectUri,
        )
    }

    companion object {

        private const val PREFS_FILENAME = "prefs_tid_partner"
        private const val CODE_VERIFIER = "code_verifier"
        private const val AUTH_SESSION_REDIRECT_URI = "auth_session_redirect_uri"
    }
}

internal data class TidAuthSession(
    val codeVerifier: String,
    val authSessionRedirectUri: String,
)
