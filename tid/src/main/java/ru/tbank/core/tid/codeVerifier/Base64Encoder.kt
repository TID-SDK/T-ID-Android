package ru.tbank.core.tid.codeVerifier

import android.util.Base64 as Base64Android
import kotlin.io.encoding.Base64 as Base64Kotlin
import kotlin.io.encoding.ExperimentalEncodingApi

public object Base64Encoder {

    public fun encodeToString(
        data: ByteArray,
        encodingSettings: Int = ANDROID_BASE64_DEFAULT_FLAGS
    ): String {
        return try {
            AndroidEncoder.encodeToString(data, encodingSettings)
        } catch (e: AssertionError) {
            KotlinEncoder.encodeToString(data)
        }
    }

    public fun decodeToString(
        data: String,
        encodingSettings: Int = ANDROID_BASE64_DEFAULT_FLAGS
    ): String {
        return decode(data.encodeToByteArray(), encodingSettings).decodeToString()
    }

    public fun decode(
        data: String,
        encodingSettings: Int = ANDROID_BASE64_DEFAULT_FLAGS
    ): ByteArray {
        return decode(data.encodeToByteArray(), encodingSettings)
    }

    private fun decode(
        data: ByteArray,
        encodingSettings: Int = ANDROID_BASE64_DEFAULT_FLAGS
    ): ByteArray {
        return try {
            AndroidEncoder.decode(data, encodingSettings)
        } catch (e: AssertionError) {
            KotlinEncoder.decode(data)
        }
    }

    private const val ANDROID_BASE64_DEFAULT_FLAGS =
        Base64Android.NO_WRAP or Base64Android.NO_PADDING or Base64Android.URL_SAFE
}

public object AndroidEncoder {

    public fun encodeToString(data: ByteArray, encodingSettings: Int): String {
        return Base64Android.encodeToString(data, encodingSettings)
    }

    public fun decode(data: ByteArray, encodingSettings: Int): ByteArray {
        return Base64Android.decode(data, encodingSettings)
    }
}

@OptIn(ExperimentalEncodingApi::class)
public object KotlinEncoder {

    public fun encodeToString(data: ByteArray): String {
        return Base64Kotlin.UrlSafe
            .encode(data)
            .trimEnd('=') // cut off padding
    }

    public fun decode(data: ByteArray): ByteArray {
        return Base64Kotlin.UrlSafe.decode(source = data)
    }
}