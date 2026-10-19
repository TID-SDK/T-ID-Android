package ru.tbank.core.app_demo_partner

import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ru.tbank.core.tid.TidAuth
import ru.tbank.core.tid.TidCall
import ru.tbank.core.tid.TidStatusCode
import ru.tbank.core.tid.TidTokenPayload

interface PartnerUi {
    fun onTokenAvailable()
    fun onTokenRefresh()
    fun onTokenRefreshCannotInvoke()
    fun onTokenRevoke()
    fun onCancelledByUser()
    fun onAuthError()
}

/**
 * @author Stanislav Mukhametshin
 */
class PartnerPresenter(
    private val tidAuth: TidAuth,
    private val partnerUi: PartnerUi,
    lifecycle: Lifecycle,
) : DefaultLifecycleObserver {

    private val compositeDisposable = CompositeDisposable()
    private var tokenPayload: TidTokenPayload? = null

    init {
        lifecycle.addObserver(this)
    }

    fun getToken(uri: Uri) {
        when (tidAuth.getStatusCode(uri)) {
            TidStatusCode.SUCCESS -> {
                tidAuth.getTidTokenPayload(uri)
                    .toSingle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            Log.d(LOG_TAG, it.accessToken)
                            tokenPayload = it
                            partnerUi.onTokenAvailable()
                        },
                        {
                            Log.e(LOG_TAG, "GetToken Error", it)
                            partnerUi.onAuthError()
                        }
                    ).apply {
                        compositeDisposable.add(this)
                    }
            }
            TidStatusCode.CANCELLED_BY_USER -> {
                partnerUi.onCancelledByUser()
            }
            else -> {
                partnerUi.onAuthError()
            }
        }
    }

    fun refreshToken() {
        val nonNullableTokenPayload = tokenPayload ?: return
        val refreshToken = nonNullableTokenPayload.refreshToken ?: run {
            Log.e(LOG_TAG, "RefreshToken in TokenPayload is Null")
            partnerUi.onTokenRefreshCannotInvoke()
            return
        }
        tidAuth.obtainTokenPayload(refreshToken)
            .toSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(LOG_TAG, it.accessToken)
                    partnerUi.onTokenRefresh()
                    tokenPayload = it
                },
                {
                    Log.e(LOG_TAG, "RefreshToken Error", it)
                }
            ).apply {
                compositeDisposable.add(this)
            }
    }

    fun revokeToken() {
        val refreshToken = tokenPayload?.refreshToken ?: return
        tidAuth.signOutByRefreshToken(refreshToken)
            .toSingle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    Log.d(LOG_TAG, "Token Revoked")
                    partnerUi.onTokenRevoke()
                },
                {
                    Log.e(LOG_TAG, "RevokeToken Error", it)
                }
            ).apply {
                compositeDisposable.add(this)
            }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        compositeDisposable.clear()
    }

    private fun <T> TidCall<T>.toSingle(): Single<T> {
        return Single.fromCallable { getResponse() }
            .doOnDispose { cancel() }
    }

    private companion object {

        private const val LOG_TAG = "TokenResponse"
    }
}
