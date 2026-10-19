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

package ru.tbank.core.tid.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue.COMPLEX_UNIT_PX
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import ru.tbank.core.tid.R
import kotlin.math.roundToInt

internal typealias TidDrawable = R.drawable
internal typealias TidColor = R.color
internal typealias TidDimen = R.dimen

/**
 * A button with an icon, title text and other customization options that can be used for partner authorization.
 *
 * ## Usage
 * ``` xml
 * <ru.tbank.core.tid.ui.TidSignInButton
 *     android:id="@+id/standardButtonTidAuth"
 *     android:layout_width="wrap_content"
 *     android:layout_height="60dp"
 *     app:tid_compact="false"
 *     app:tid_title="Sign in with"
 *     app:tid_style="primary"
 *     app:tid_corner_radius="8dp"
 *     app:tid_font="@font/neue_haas_unica_w1g"/>
 * ```
 *
 * ## View attributes:
 * - `tid_compact` - way to customize the button size, one of the options - false (default) / true.
 * - `tid_style` - special button style, one of the options - "primary" (default) / "gray" / "black" / "white".
 * - `tid_title` - text on the button. Used only if `tid_compact` attribute is false. Default value is "Т-Банк".
 * - `tid_corner_radius` - radius for button corners. Used only if `tid_compact` attribute is false.
 * - `tid_font` - font of the text on the button. Used only if `tid_compact` attribute is false.
 *
 * @author Kirill Voskrebentsev
 */
public class TidSignInButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ViewGroup(context, attrs, defStyle, defStyleRes) {

    // Customization elements
    public var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = if (value.isNullOrEmpty()) fallbackTitle else value
            updateChildrenVisibility()
        }

    public var isCompact: Boolean = false
        set(value) {
            field = value
            updateChildrenVisibility()
            updateStyle()
        }

    public var style: ButtonStyle = ButtonStyle.PRIMARY
        set(value) {
            field = value
            updateStyle()
        }

    @Px
    public var cornerRadius: Int = getDimension(TidDimen.tid_default_corner_radius)
        set(value) {
            field = value
            updateStyle()
        }

    public var textFont: Typeface? = getFont(R.font.neue_haas_unica_w1g)
        set(value) {
            field = value
            titleView.typeface = value
        }

    // Auxiliary elements
    private var size: ButtonSize = ButtonSize.LARGE
        set(value) {
            field = value
            updateSize()
        }
    private val fallbackTitle = context.getString(R.string.tid_tbank_text)

    // Internal views
    private var titleView: TextView = AppCompatTextView(context).apply {
        gravity = Gravity.CENTER
    }
    // ImageView for T-ID logo in common (S/M/L) buttons
    private var commonLogoView: ImageView = AppCompatImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }
    // ImageView for shield logo in compact button
    private var compactLogoView: ImageView = AppCompatImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    // Sizes and paddings of views
    private val minHeight = getDimension(ButtonSize.SMALL.minHeight)
    private val compactTopPadding = getDimension(TidDimen.tid_compact_top_padding)
    private val compactBottomPadding = getDimension(TidDimen.tid_compact_bottom_padding)
    private val compactHorizontalPadding = getDimension(TidDimen.tid_compact_horizontal_padding)
    private var commonLogoBorder = 0
        set(value) {
            field = if (style == ButtonStyle.BLACK) value else 0
        }
    private var minVerticalPadding = 0
    private var minHorizontalPadding = 0
    private var titleCommonLogoOffset = 0
    private var commonLogoHeight = 0
    private var commonLogoWidth = 0

    init {
        addView(titleView)
        addView(commonLogoView)
        addView(compactLogoView)

        context.obtainStyledAttributes(attrs, R.styleable.TidSignInButton, defStyle, defStyleRes).apply {
            isCompact = getBoolean(R.styleable.TidSignInButton_tid_compact, false)
            style = getInt(R.styleable.TidSignInButton_tid_style, ButtonStyle.PRIMARY.ordinal).let(ButtonStyle.values()::get)
            cornerRadius = getDimension(R.styleable.TidSignInButton_tid_corner_radius, getDimension(TidDimen.tid_default_corner_radius).toFloat()).roundToInt()
            textFont = this@TidSignInButton.getFont(getResourceId(R.styleable.TidSignInButton_tid_font, R.font.neue_haas_unica_w1g))
            title = getString(R.styleable.TidSignInButton_tid_title)
            recycle()
        }

        updateStyle()
    }

    @Suppress("ComplexMethod")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        fun measureTextView(textView: TextView) {
            val wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            textView.measure(wrapContentSpec, wrapContentSpec)
        }

        fun measureImageView(imageView: ImageView, width: Int, height: Int) {
            imageView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            )
        }

        val totalHeight = if (MeasureSpec.getSize(heightMeasureSpec) < minHeight) {
            minHeight
        } else {
            resolveSize(minHeight, heightMeasureSpec)
        }

        size = getButtonSize(totalHeight)

        if (!isCompact) {
            measureTextView(titleView)
            measureImageView(
                imageView = commonLogoView,
                width = commonLogoWidth + commonLogoBorder + commonLogoBorder,
                height = commonLogoHeight + commonLogoBorder + commonLogoBorder,
            )
        }

        val minWidth = totalHeight * WIDTH_RELATIVE_HEIGHT

        val contentWidth = if (isCompact) {
            totalHeight
        } else {
            maxOf(
                minHorizontalPadding +
                        titleView.measuredWidth +
                        titleCommonLogoOffset +
                        commonLogoView.measuredWidth +
                        minHorizontalPadding, minWidth
            )
        }

        val totalWidth = if (isCompact) {
            contentWidth
        } else {
            resolveSize(contentWidth, widthMeasureSpec)
        }

        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val parentWidth = r - l
        val parentHeight = b - t
        val parentTop = minVerticalPadding
        val parentBottom = parentHeight - minVerticalPadding

        fun layoutChild(child: View, childLeft: Int) {
            val width = child.measuredWidth
            val height = child.measuredHeight
            val childTop = parentTop + (parentBottom - parentTop - height) / 2
            child.layout(childLeft, childTop, childLeft + width, childTop + height)
        }

        if (isCompact) {
            compactLogoView.layout(
                /* left = */ compactHorizontalPadding,
                /* top = */ compactTopPadding,
                /* right = */ parentWidth - compactHorizontalPadding,
                /* bottom = */ parentHeight - compactBottomPadding
            )
        } else {
            var currentLeft = (parentWidth -
                    titleView.measuredWidth -
                    titleCommonLogoOffset -
                    commonLogoView.measuredWidth) / 2

            layoutChild(titleView, currentLeft)
            currentLeft += titleView.measuredWidth + titleCommonLogoOffset - commonLogoBorder

            layoutChild(commonLogoView, currentLeft)
        }
    }

    private fun updateStyle() {
        background = if (isCompact) {
            getDrawable(style.backgroundCompactRes)
        } else {
            val pressedState = getColor(style.buttonPressedStateColorRes)
            val contentDrawable = GradientDrawable().apply {
                cornerRadius = this@TidSignInButton.cornerRadius.toFloat()
                color = getColor(style.buttonEnabledStateColorRes)
            }
            RippleDrawable(pressedState, contentDrawable, null)
        }
        titleView.setTextColor(getColor(style.textColorRes))
        updateLogoDrawable()
    }

    private fun updateSize() {
        minVerticalPadding = getDimension(size.minVerticalPadding)
        minHorizontalPadding = getDimension(size.minHorizontalPadding)
        titleCommonLogoOffset = getDimension(size.titleCommonLogoOffset)
        commonLogoHeight = getDimension(size.commonLogoHeight)
        commonLogoWidth = getDimension(size.commonLogoWidth)
        commonLogoBorder = getDimension(size.commonLogoBorder)
        updateLogoDrawable()
        titleView.setTextSize(COMPLEX_UNIT_PX, getDimension(size.titleFontSize).toFloat())
    }

    private fun updateLogoDrawable() {
        compactLogoView.setImageDrawable(getDrawable(style.compactLogoIconImageRes))
        commonLogoView.setImageDrawable(
            getDrawable(
                id = if (size != ButtonSize.SMALL) style.commonLogoIconImageRes else style.smallCommonLogoIconImageRes
            )
        )
    }

    private fun updateChildrenVisibility() {
        compactLogoView.isVisible = isCompact
        commonLogoView.isVisible = !isCompact
        titleView.isVisible = !isCompact
    }

    public enum class ButtonSize(
        @DimenRes internal val minHeight: Int,
        @DimenRes internal val minVerticalPadding: Int,
        @DimenRes internal val minHorizontalPadding: Int,
        @DimenRes internal val titleFontSize: Int,
        @DimenRes internal val titleCommonLogoOffset: Int,
        @DimenRes internal val commonLogoHeight: Int,
        @DimenRes internal val commonLogoWidth: Int,
        @DimenRes internal val commonLogoBorder: Int,
    ) {
        SMALL(
            minHeight = TidDimen.tid_small_min_height,
            minVerticalPadding = TidDimen.tid_small_vertical_padding,
            minHorizontalPadding = TidDimen.tid_small_horizontal_padding,
            titleFontSize = TidDimen.tid_small_title_font_size,
            titleCommonLogoOffset = TidDimen.tid_small_title_tid_logo_offset,
            commonLogoHeight = TidDimen.tid_small_tid_logo_height,
            commonLogoWidth = TidDimen.tid_small_tid_logo_width,
            commonLogoBorder = TidDimen.tid_small_tid_logo_border,
        ),
        MEDIUM(
            minHeight = TidDimen.tid_medium_min_height,
            minVerticalPadding = TidDimen.tid_medium_vertical_padding,
            minHorizontalPadding = TidDimen.tid_medium_horizontal_padding,
            titleFontSize = TidDimen.tid_medium_title_font_size,
            titleCommonLogoOffset = TidDimen.tid_medium_title_tid_logo_offset,
            commonLogoHeight = TidDimen.tid_medium_tid_logo_height,
            commonLogoWidth = TidDimen.tid_medium_tid_logo_width,
            commonLogoBorder = TidDimen.tid_medium_tid_logo_border,
        ),
        LARGE(
            minHeight = TidDimen.tid_large_min_height,
            minVerticalPadding = TidDimen.tid_large_vertical_padding,
            minHorizontalPadding = TidDimen.tid_large_horizontal_padding,
            titleFontSize = TidDimen.tid_large_title_font_size,
            titleCommonLogoOffset = TidDimen.tid_large_title_tid_logo_offset,
            commonLogoHeight = TidDimen.tid_large_tid_logo_height,
            commonLogoWidth = TidDimen.tid_large_tid_logo_width,
            commonLogoBorder = TidDimen.tid_large_tid_logo_border,
        ),
    }

    private fun getButtonSize(height: Int): ButtonSize = when {
        height < getDimension(ButtonSize.MEDIUM.minHeight) -> ButtonSize.SMALL
        height < getDimension(ButtonSize.LARGE.minHeight) -> ButtonSize.MEDIUM
        else -> ButtonSize.LARGE
    }

    public enum class ButtonStyle(
        @ColorRes internal val buttonEnabledStateColorRes: Int,
        @ColorRes internal val buttonPressedStateColorRes: Int,
        @DrawableRes internal val backgroundCompactRes: Int,
        @DrawableRes internal val commonLogoIconImageRes: Int,
        @DrawableRes internal val smallCommonLogoIconImageRes: Int = commonLogoIconImageRes,
        @DrawableRes internal val compactLogoIconImageRes: Int,
        @ColorRes internal val textColorRes: Int,
    ) {
        PRIMARY(
            buttonEnabledStateColorRes = TidColor.tid_primary_button,
            buttonPressedStateColorRes = TidColor.tid_primary_button_pressed,
            backgroundCompactRes = TidDrawable.tid_primary_compact_background,
            commonLogoIconImageRes = TidDrawable.tid_capsule_logo,
            compactLogoIconImageRes = TidDrawable.tid_shield_logo_white,
            textColorRes = TidColor.tid_primary_text,
        ),
        WHITE(
            buttonEnabledStateColorRes = TidColor.tid_white_button,
            buttonPressedStateColorRes = TidColor.tid_white_button_pressed,
            backgroundCompactRes = TidDrawable.tid_white_compact_background,
            commonLogoIconImageRes = TidDrawable.tid_capsule_logo,
            compactLogoIconImageRes = TidDrawable.tid_shield_logo_yellow,
            textColorRes = TidColor.tid_white_text,
        ),
        GRAY(
            buttonEnabledStateColorRes = TidColor.tid_gray_button,
            buttonPressedStateColorRes = TidColor.tid_gray_button_pressed,
            backgroundCompactRes = TidDrawable.tid_gray_compact_background,
            commonLogoIconImageRes = TidDrawable.tid_capsule_logo,
            compactLogoIconImageRes = TidDrawable.tid_shield_logo_yellow,
            textColorRes = TidColor.tid_gray_text,
        ),
        BLACK(
            buttonEnabledStateColorRes = TidColor.tid_black_button,
            buttonPressedStateColorRes = TidColor.tid_black_button_pressed,
            backgroundCompactRes = TidDrawable.tid_black_compact_background,
            commonLogoIconImageRes = TidDrawable.tid_capsule_logo_border,
            smallCommonLogoIconImageRes = TidDrawable.tid_capsule_logo_border_small,
            compactLogoIconImageRes = TidDrawable.tid_shield_logo_yellow,
            textColorRes = TidColor.tid_black_text,
        ),
    }

    private fun getDimension(@DimenRes id: Int) = context.resources.getDimension(id).roundToInt()
    private fun getDrawable(@DrawableRes id: Int) = AppCompatResources.getDrawable(context, id)
    private fun getColor(@ColorRes id: Int) = AppCompatResources.getColorStateList(context, id)
    private fun getFont(@FontRes id: Int) = ResourcesCompat.getFont(context, id)

    private companion object {
        private const val WIDTH_RELATIVE_HEIGHT = 4
    }
}
