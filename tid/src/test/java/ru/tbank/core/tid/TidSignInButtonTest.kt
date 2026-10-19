package ru.tbank.core.tid

import android.content.Context
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.tbank.core.tid.ui.TidDimen
import ru.tbank.core.tid.ui.TidSignInButton

/**
 * @author k.voskrebentsev
 */
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
internal class TidSignInButtonTest {

    private lateinit var button: TidSignInButton

    @Before
    fun setUp() {
        button = TidSignInButton(context = context)
    }

    @Test
    fun testSetEmptyTitle() {
        button.title = ""

        val result = button.title

        assertEquals(PERMANENT_TITLE_PART, result.toString())
    }

    @Test
    fun testSetTitle() {
        button.title = CUSTOM_TITLE_PART

        val result = button.title

        assertEquals(CUSTOM_TITLE_PART, result.toString())
    }

    @Test
    fun testDefaultCompact() {
        assertEquals(false, button.isCompact)
    }

    @Test
    fun testDefaultStyle() {
        assertEquals(TidSignInButton.ButtonStyle.PRIMARY, button.style)
    }

    @Test
    fun testDefaultCornerSize() {
        assertEquals(context.resources.getDimension(TidDimen.tid_default_corner_radius).toInt(), button.cornerRadius)
    }

    @Test
    fun testDefaultFont() {
        assertEquals(ResourcesCompat.getFont(context, R.font.neue_haas_unica_w1g), button.textFont)
    }

    companion object {
        val context: Context = ApplicationProvider.getApplicationContext()

        const val CUSTOM_TITLE_PART = "title"
        val PERMANENT_TITLE_PART = context.getString(R.string.tid_tbank_text)
    }
}
