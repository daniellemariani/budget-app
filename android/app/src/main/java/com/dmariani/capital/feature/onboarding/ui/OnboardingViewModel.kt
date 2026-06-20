package com.dmariani.capital.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dmariani.capital.R
import com.dmariani.capital.core.domain.AccountType
import com.dmariani.capital.feature.onboarding.domain.CreateAccountParams
import com.dmariani.capital.feature.onboarding.domain.CreateAccountUseCase
import com.dmariani.capital.feature.onboarding.domain.DuplicateAccountNameException
import com.dmariani.capital.feature.onboarding.domain.InitializeWorkspaceUseCase
import com.dmariani.capital.feature.onboarding.domain.SaveDisplayNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Error Types
// ---------------------------------------------------------------------------

/**
 * Typed error tokens for onboarding form fields.
 *
 * Composables resolve the display string via:
 *   error?.let { stringResource(it.stringRes) }
 */
sealed class OnboardingError {
    data object MaxAmount : OnboardingError()
    data object CreditLimitZero : OnboardingError()
    data object CreditLimitBelowBalance : OnboardingError()
    data object DuplicateAccountName : OnboardingError()
    data object AccountSaveFailed : OnboardingError()

    val stringRes: Int
        get() = when (this) {
            MaxAmount -> R.string.onboarding_error_max_amount
            CreditLimitZero -> R.string.onboarding_error_credit_limit_zero
            CreditLimitBelowBalance -> R.string.onboarding_error_credit_limit_below_balance
            DuplicateAccountName -> R.string.onboarding_error_duplicate_account_name
            AccountSaveFailed -> R.string.onboarding_error_account_save_failed
        }
}

// ---------------------------------------------------------------------------
// UI State
// ---------------------------------------------------------------------------

data class OnboardingUiState(
    val isInitializing: Boolean = true,
    val initializationError: Boolean = false,
    val displayName: String = "",
    val isDisplayNameValid: Boolean = false,
    val accountName: String = "",
    val accountType: AccountType = AccountType.CHECKING,
    val initialBalance: String = "",
    val creditLimit: String = "",
    val isAccountFormValid: Boolean = false,
    val accountNameError: OnboardingError? = null,
    val initialBalanceError: OnboardingError? = null,
    val creditLimitError: OnboardingError? = null,
    val accountSaveError: OnboardingError? = null,
    val showAccountSavedDialog: Boolean = false,
    val savedAccountsCount: Int = 0,
)

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

sealed class OnboardingEvent {
    object InitializationRetried : OnboardingEvent()
    object SlidesCompleted : OnboardingEvent()
    data class DisplayNameChanged(val name: String) : OnboardingEvent()
    object ContinueWithName : OnboardingEvent()
    data class AccountNameChanged(val name: String) : OnboardingEvent()
    data class AccountTypeChanged(val type: AccountType) : OnboardingEvent()
    data class InitialBalanceChanged(val value: String) : OnboardingEvent()
    data class CreditLimitChanged(val value: String) : OnboardingEvent()
    object CreditLimitFocusLost : OnboardingEvent()
    object SaveAccount : OnboardingEvent()
    object AddAnotherAccount : OnboardingEvent()
    object GoToHome : OnboardingEvent()
}

// ---------------------------------------------------------------------------
// Side Effects
// ---------------------------------------------------------------------------

sealed class OnboardingSideEffect {
    object NavigateToSetYourName : OnboardingSideEffect()
    object NavigateToAddAnAccount : OnboardingSideEffect()
    object NavigateToHome : OnboardingSideEffect()
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val initializeWorkspaceUseCase: InitializeWorkspaceUseCase,
    private val saveDisplayNameUseCase: SaveDisplayNameUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _sideEffects = Channel<OnboardingSideEffect>(Channel.BUFFERED)
    val sideEffects = _sideEffects.receiveAsFlow()

    private var workspaceId: String = ""

    init {
        initialize()
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.InitializationRetried -> {
                _uiState.update { it.copy(initializationError = false, isInitializing = true) }
                initialize()
            }
            OnboardingEvent.SlidesCompleted -> {
                viewModelScope.launch {
                    _sideEffects.send(OnboardingSideEffect.NavigateToSetYourName)
                }
            }
            is OnboardingEvent.DisplayNameChanged -> {
                _uiState.update {
                    it.copy(
                        displayName = event.name,
                        isDisplayNameValid = event.name.trim().length in DISPLAY_NAME_MIN_LENGTH..DISPLAY_NAME_MAX_LENGTH,
                    )
                }
            }
            OnboardingEvent.ContinueWithName -> {
                saveDisplayNameUseCase(_uiState.value.displayName)
                viewModelScope.launch {
                    _sideEffects.send(OnboardingSideEffect.NavigateToAddAnAccount)
                }
            }
            is OnboardingEvent.AccountNameChanged -> {
                val updated = _uiState.value.copy(
                    accountName = event.name,
                    accountNameError = null,
                    accountSaveError = null,
                )
                _uiState.value = updated.copy(isAccountFormValid = computeIsFormValid(updated))
            }
            is OnboardingEvent.AccountTypeChanged -> {
                val updated = _uiState.value.copy(
                    accountType = event.type,
                    creditLimit = if (event.type != AccountType.CREDIT_CARD) "" else _uiState.value.creditLimit,
                    creditLimitError = if (event.type != AccountType.CREDIT_CARD) null else _uiState.value.creditLimitError,
                )
                _uiState.value = updated.copy(isAccountFormValid = computeIsFormValid(updated))
            }
            is OnboardingEvent.InitialBalanceChanged -> {
                val updated = _uiState.value.copy(
                    initialBalance = event.value,
                    initialBalanceError = null,
                )
                _uiState.value = updated.copy(isAccountFormValid = computeIsFormValid(updated))
            }
            is OnboardingEvent.CreditLimitChanged -> {
                val updated = _uiState.value.copy(creditLimit = event.value)
                _uiState.value = updated.copy(isAccountFormValid = computeIsFormValid(updated))
            }
            OnboardingEvent.CreditLimitFocusLost -> {
                if (_uiState.value.accountType == AccountType.CREDIT_CARD) {
                    val limitCents = parseToCents(_uiState.value.creditLimit)
                    if (limitCents != null) {
                        val balanceCents = parseToCents(_uiState.value.initialBalance) ?: 0L
                        val error = if (limitCents < balanceCents) OnboardingError.CreditLimitBelowBalance else null
                        val updated = _uiState.value.copy(creditLimitError = error)
                        _uiState.value = updated.copy(isAccountFormValid = computeIsFormValid(updated))
                    }
                }
            }
            OnboardingEvent.SaveAccount -> saveAccount()
            OnboardingEvent.AddAnotherAccount -> {
                _uiState.update {
                    it.copy(
                        showAccountSavedDialog = false,
                        accountName = "",
                        accountType = AccountType.CHECKING,
                        initialBalance = "",
                        creditLimit = "",
                        accountNameError = null,
                        initialBalanceError = null,
                        creditLimitError = null,
                        accountSaveError = null,
                        isAccountFormValid = false,
                    )
                }
            }
            OnboardingEvent.GoToHome -> {
                _uiState.update { it.copy(showAccountSavedDialog = false) }
                viewModelScope.launch {
                    _sideEffects.send(OnboardingSideEffect.NavigateToHome)
                }
            }
        }
    }

    private fun initialize() {
        viewModelScope.launch {
            initializeWorkspaceUseCase()
                .onSuccess { id ->
                    workspaceId = id
                    _uiState.update { it.copy(isInitializing = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(isInitializing = false, initializationError = true) }
                }
        }
    }

    private fun saveAccount() {
        val state = _uiState.value

        val balanceCents = parseToCents(state.initialBalance)
        if (balanceCents == null || balanceCents < 0 || balanceCents > MAX_AMOUNT_CENTS) {
            _uiState.update { it.copy(initialBalanceError = OnboardingError.MaxAmount) }
            return
        }

        var creditLimitCents: Long? = null
        if (state.accountType == AccountType.CREDIT_CARD) {
            val limitCents = parseToCents(state.creditLimit)
            when {
                limitCents == null || limitCents <= 0 -> {
                    _uiState.update { it.copy(creditLimitError = OnboardingError.CreditLimitZero) }
                    return
                }
                limitCents > MAX_AMOUNT_CENTS -> {
                    _uiState.update { it.copy(creditLimitError = OnboardingError.MaxAmount) }
                    return
                }
                limitCents < balanceCents -> {
                    _uiState.update { it.copy(creditLimitError = OnboardingError.CreditLimitBelowBalance) }
                    return
                }
                else -> creditLimitCents = limitCents
            }
        }

        viewModelScope.launch {
            createAccountUseCase(
                CreateAccountParams(
                    workspaceId = workspaceId,
                    name = state.accountName,
                    type = state.accountType,
                    currencyCode = "USD",
                    initialBalanceCents = balanceCents,
                    creditLimitCents = creditLimitCents,
                )
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            showAccountSavedDialog = true,
                            savedAccountsCount = it.savedAccountsCount + 1,
                            accountSaveError = null,
                        )
                    }
                },
                onFailure = { e ->
                    if (e is DuplicateAccountNameException) {
                        _uiState.update { it.copy(accountNameError = OnboardingError.DuplicateAccountName) }
                    } else {
                        _uiState.update { it.copy(accountSaveError = OnboardingError.AccountSaveFailed) }
                    }
                }
            )
        }
    }

    private fun computeIsFormValid(state: OnboardingUiState): Boolean {
        val trimmedName = state.accountName.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 100) return false

        val balanceCents = parseToCents(state.initialBalance) ?: return false
        if (balanceCents < 0 || balanceCents > MAX_AMOUNT_CENTS) return false

        if (state.accountType == AccountType.CREDIT_CARD) {
            val limitCents = parseToCents(state.creditLimit) ?: return false
            if (limitCents <= 0 || limitCents > MAX_AMOUNT_CENTS) return false
            if (state.creditLimitError != null) return false
        }

        return true
    }

    private fun parseToCents(value: String): Long? {
        if (value.isBlank()) return null
        val cleaned = value.filter { it.isDigit() || it == '.' }
        return cleaned.toDoubleOrNull()?.let { (it * 100).roundToLong() }
    }

    companion object {
        const val DISPLAY_NAME_MIN_LENGTH = 2
        const val DISPLAY_NAME_MAX_LENGTH = 30  // also enforced in SetYourNameScreen maxLength
        const val MAX_AMOUNT_CENTS = 999_999_999L
    }
}
