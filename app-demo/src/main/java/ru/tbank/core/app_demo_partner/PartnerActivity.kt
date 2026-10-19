package ru.tbank.core.app_demo_partner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.textfield.TextInputLayout
import ru.tbank.core.tid.TidAuth
import ru.tbank.core.tid.TidWebMode
import ru.tbank.core.tid.error.TidInvalidPhoneException
import ru.tbank.core.tid.ui.TidSignInButton
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.roundToInt
import ru.tbank.core.tid.R as TidR

class PartnerActivity : AppCompatActivity(), PartnerUi {

    private companion object {
        private const val REDIRECT_URI_KEY = "redirect_uri_key"
        private const val CLIENT_ID_KEY = "client_id_key"
        private const val PHONE_NUMBER_KEY = "phone_number_key"
        private const val HOST_NUMBER_KEY = "host_number_key"
    }

    private enum class AuthFlow {
        AUTO,
        T_BANK_APP,
        CCT_WEB,
    }

    private lateinit var tidAuth: TidAuth
    private lateinit var partnerPresenter: PartnerPresenter

    private val keyToEditTextList: List<Pair<String, EditText>> by lazy(NONE) {
        listOf(
            REDIRECT_URI_KEY to redirectUriEditText,
            CLIENT_ID_KEY to clientIdEditText,
            PHONE_NUMBER_KEY to phoneEditText,
            HOST_NUMBER_KEY to hostEditText,
        )
    }

    private val rootView by lazy(NONE) { findViewById<View>(android.R.id.content) }

    private val clientIdEditText by lazy(NONE) { findViewById<EditText>(R.id.partner_auth_edit_text_client_id) }
    private val redirectUriEditText by lazy(NONE) { findViewById<EditText>(R.id.partner_auth_edit_text_redirect_uri) }
    private val hostEditText by lazy(NONE) { findViewById<EditText>(R.id.partner_auth_edit_text_host) }
    private val phoneEditText by lazy(NONE) { findViewById<EditText>(R.id.partner_auth_edit_text_phone) }

    private val whiteLabelModeSwitch by lazy(NONE) { findViewById<SwitchCompat>(R.id.partner_auth_switch_compat_white_label_mode) }
    private val phoneLayout by lazy(NONE) { findViewById<TextInputLayout>(R.id.partner_auth_input_layout_phone) }

    private val overrideHostSwitch by lazy(NONE) { findViewById<SwitchCompat>(R.id.partner_auth_switch_compat_override_host) }
    private val hostLayout by lazy(NONE) { findViewById<TextInputLayout>(R.id.partner_auth_input_layout_host) }

    private val authFlowSpinner by lazy(NONE) { findViewById<Spinner>(R.id.partner_auth_flow_spinner) }
    private val authFlowValues = AuthFlow.values()

    private val resetButton by lazy(NONE) { findViewById<Button>(R.id.partner_auth_button_reset) }
    private val signInButton by lazy(NONE) { findViewById<TidSignInButton>(R.id.partner_auth_button_tid_auth) }

    private val styleSpinner by lazy(NONE) { findViewById<Spinner>(R.id.partner_auth_style_spinner) }
    private val sizeSpinner by lazy(NONE) { findViewById<Spinner>(R.id.partner_auth_size_spinner) }

    private val buttonTitleEditText by lazy(NONE) { findViewById<EditText>(R.id.partner_auth_edit_text_button_title) }
    private val buttonTitleLayout by lazy(NONE) { findViewById<TextInputLayout>(R.id.partner_auth_input_layout_button_title) }

    private val updateTokenButton by lazy(NONE) { findViewById<Button>(R.id.partner_auth_button_update_token) }
    private val revokeTokenButton by lazy(NONE) { findViewById<Button>(R.id.partner_auth_button_revoke_token) }

    private val isDataCorrect: Boolean
        get() = when {
            clientIdEditText.text.isEmpty() -> {
                clientIdEditText.error = getString(R.string.partner_auth_edit_text_empty_error_description)
                false
            }

            redirectUriEditText.text.isEmpty() -> {
                redirectUriEditText.error = getString(R.string.partner_auth_edit_text_empty_error_description)
                false
            }

            overrideHostSwitch.isChecked && hostEditText.text.isEmpty() -> {
                hostEditText.error = getString(R.string.partner_auth_edit_text_empty_error_description)
                false
            }

            whiteLabelModeSwitch.isChecked && phoneEditText.text.isEmpty() -> {
                phoneEditText.error = getString(R.string.partner_auth_edit_text_empty_error_description)
                false
            }

            else -> true
        }

    @Suppress("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_partner)

        if (savedInstanceState != null) handleSavedInstanceState(savedInstanceState)

        initTidAuth()
        intent.data?.let { partnerPresenter.getToken(it) }

        initStyleButtons()
        initSizeButtons()
        initAuthFlowSpinner()

        buttonTitleEditText.addTextChangedListener { signInButton.title = it.toString() }
        signInButton.setOnClickListener { trySignIn() }
        updateTokenButton.setOnClickListener { partnerPresenter.refreshToken() }
        revokeTokenButton.setOnClickListener { partnerPresenter.revokeToken() }
        resetButton.setOnClickListener { resetTidAuth() }
        whiteLabelModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            phoneLayout.isVisible = isChecked

            if (isChecked) {
                authFlowSpinner.setSelection(AuthFlow.CCT_WEB.ordinal)
                authFlowSpinner.isEnabled = false
            } else {
                authFlowSpinner.isEnabled = true
            }
        }
        overrideHostSwitch.setOnCheckedChangeListener { _, isChecked -> hostLayout.isVisible = isChecked }
    }

    override fun onResume() {
        super.onResume()
        setupInsets(rootView)
    }

    private fun setupInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val insetsMask = Type.statusBars() or Type.navigationBars()
            val systemInsets = insets.getInsets(insetsMask)

            view.updatePadding(
                top = systemInsets.top,
                bottom = systemInsets.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    @Suppress("NewApi")
    private fun trySignIn() {
        if (!isDataCorrect) return
        initTidAuth()
        try {
            val webMode = if (whiteLabelModeSwitch.isChecked) {
                TidWebMode.WhiteLabel(phone = phoneEditText.text.toString())
            } else {
                TidWebMode.Partner
            }
            val selectedAuthFlow = if (whiteLabelModeSwitch.isChecked) {
                AuthFlow.CCT_WEB
            } else {
                authFlowValues.getOrElse(authFlowSpinner.selectedItemPosition) { AuthFlow.AUTO }
            }
            val intent = when (selectedAuthFlow) {
                AuthFlow.AUTO -> tidAuth.createTidAuthIntent(webMode)
                AuthFlow.T_BANK_APP -> tidAuth.createTBankAppAuthIntent()
                AuthFlow.CCT_WEB -> tidAuth.createTidWebAuthIntent(webMode)
            }
            startActivity(intent)
        } catch (error: TidInvalidPhoneException) {
            showShortToast("WhiteLabel Error: invalid phone")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        keyToEditTextList.forEach { (key, editText) ->
            outState.putString(key, editText.text.toString())
        }
    }

    private fun handleSavedInstanceState(savedInstanceState: Bundle) {
        keyToEditTextList.forEach { (key, editText) ->
            savedInstanceState.getString(key)?.let { savedString -> editText.setText(savedString) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { partnerPresenter.getToken(it) }
    }

    override fun onTokenAvailable() {
        updateTokenButton.isVisible = true
        revokeTokenButton.isVisible = true
        showShortToast("Token Available")
    }

    override fun onTokenRefresh() {
        showShortToast("Token Refresh Success")
    }

    override fun onTokenRefreshCannotInvoke() {
        showShortToast("RefreshToken in TokenPayload is Null")
    }

    override fun onTokenRevoke() {
        showShortToast("Token Revoke Success")
    }

    override fun onCancelledByUser() {
        showShortToast("Partner authorization was cancelled")
    }

    override fun onAuthError() {
        showShortToast("Auth error occurred")
    }

    private fun initTidAuth() {
        val clientId = clientIdEditText.text.toString()
        val redirectUri = redirectUriEditText.text.toString()
        val tidApiHost = if (overrideHostSwitch.isChecked) {
            hostEditText.text.toString()
        } else {
            TidAuth.TID_API_DEFAULT_HOST
        }

        tidAuth = TidAuth(
            context = applicationContext,
            clientId = clientId,
            redirectUri = redirectUri,
            tidApiHost = tidApiHost,
        )
        partnerPresenter = PartnerPresenter(
            tidAuth = tidAuth,
            partnerUi = this,
            lifecycle = lifecycle,
        )
    }

    private fun resetTidAuth() {
        clientIdEditText.apply {
            error = null
            setText(R.string.partner_auth_default_client_id)
        }
        redirectUriEditText.apply {
            error = null
            setText(R.string.partner_auth_default_redirect_uri)
        }
        phoneEditText.apply {
            error = null
            setText(R.string.partner_auth_default_phone_number)
        }
        hostEditText.apply {
            error = null
            setText(R.string.partner_auth_default_host)
        }
        initTidAuth()
    }

    private fun initStyleButtons() {
        ArrayAdapter.createFromResource(
            this,
            R.array.styles_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            styleSpinner.adapter = adapter
            styleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val styles = arrayOf(
                        TidSignInButton.ButtonStyle.PRIMARY,
                        TidSignInButton.ButtonStyle.WHITE,
                        TidSignInButton.ButtonStyle.GRAY,
                        TidSignInButton.ButtonStyle.BLACK,
                    )

                    val style = styles[position]

                    AppCompatDelegate.setDefaultNightMode(
                        if (style == TidSignInButton.ButtonStyle.BLACK ||
                            style == TidSignInButton.ButtonStyle.WHITE
                        ) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                    )
                    signInButton.style = style
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
    }

    private fun initAuthFlowSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.auth_flows_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            authFlowSpinner.adapter = adapter
        }
    }

    @Suppress("MagicNumber")
    private fun initSizeButtons() {
        ArrayAdapter.createFromResource(
            this,
            R.array.sizes_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sizeSpinner.adapter = adapter
            sizeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val sizes = arrayOf(
                        getDimension(TidR.dimen.tid_small_min_height),
                        getDimension(TidR.dimen.tid_medium_min_height),
                        getDimension(TidR.dimen.tid_large_min_height),
                        dpToPx(56),
                    )
                    val size = sizes[position]
                    val compact = position == 3

                    signInButton.apply {
                        isCompact = compact
                        layoutParams.height = size
                        requestLayout()
                    }
                    buttonTitleLayout.isVisible = !compact
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        sizeSpinner.setSelection(2)
    }

    private fun getDimension(@DimenRes id: Int) = resources.getDimension(id).roundToInt()

    private fun showShortToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
