package com.packready.shopkeeper.presentation.khata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.domain.model.KhataCustomer
import com.packready.shopkeeper.domain.model.KhataTransaction
import com.packready.shopkeeper.domain.repository.KhataRepository
import com.packready.shopkeeper.domain.usecase.SettleKhataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KhataUiState(
    val customers: List<KhataCustomer> = emptyList(),
    val totalOutstandingDues: Double = 0.0,
    val searchQuery: String = "",
    val searchResults: List<KhataCustomer> = emptyList(),
    val isSearching: Boolean = false,
    val selectedCustomer: KhataCustomer? = null,
    val customerTransactions: List<KhataTransaction> = emptyList(),
    val isSettleDialogOpen: Boolean = false,
    val settlementAmountInput: String = "",
    val settlementNoteInput: String = "Cash Received",
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface KhataUiEvent {
    data class OnSearchQueryChange(val query: String) : KhataUiEvent
    data class SelectCustomer(val customer: KhataCustomer) : KhataUiEvent
    object OpenSettleDialog : KhataUiEvent
    object CloseSettleDialog : KhataUiEvent
    data class OnSettlementAmountChange(val amount: String) : KhataUiEvent
    data class OnSettlementNoteChange(val note: String) : KhataUiEvent
    object ConfirmSettlement : KhataUiEvent
    object ClearMessages : KhataUiEvent
}

@HiltViewModel
class KhataViewModel @Inject constructor(
    private val khataRepository: KhataRepository,
    private val settleKhataUseCase: SettleKhataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(KhataUiState())
    val uiState: StateFlow<KhataUiState> = _uiState.asStateFlow()

    init {
        observeCustomers()
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            khataRepository.getAllCustomers().collectLatest { list ->
                val totalDues = list.sumOf { it.totalOutstanding }
                _uiState.update {
                    it.copy(
                        customers = list,
                        totalOutstandingDues = totalDues
                    )
                }
            }
        }
    }

    fun onEvent(event: KhataUiEvent) {
        when (event) {
            is KhataUiEvent.OnSearchQueryChange -> {
                val q = event.query
                _uiState.update { state ->
                    val filtered = if (q.isBlank()) emptyList() else {
                        state.customers.filter {
                            it.customerName.contains(q, ignoreCase = true) ||
                            it.customerPhone.contains(q, ignoreCase = true)
                        }
                    }
                    state.copy(
                        searchQuery = q,
                        searchResults = filtered,
                        isSearching = q.isNotBlank()
                    )
                }
            }
            is KhataUiEvent.SelectCustomer -> loadCustomerDetails(event.customer)
            is KhataUiEvent.OpenSettleDialog -> {
                val cust = _uiState.value.selectedCustomer
                _uiState.update {
                    it.copy(
                        isSettleDialogOpen = true,
                        settlementAmountInput = String.format("%.0f", cust?.totalOutstanding ?: 0.0)
                    )
                }
            }
            is KhataUiEvent.CloseSettleDialog -> _uiState.update { it.copy(isSettleDialogOpen = false) }
            is KhataUiEvent.OnSettlementAmountChange -> _uiState.update { it.copy(settlementAmountInput = event.amount) }
            is KhataUiEvent.OnSettlementNoteChange -> _uiState.update { it.copy(settlementNoteInput = event.note) }
            is KhataUiEvent.ConfirmSettlement -> confirmSettlement()
            is KhataUiEvent.ClearMessages -> _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
        }
    }

    private fun loadCustomerDetails(customer: KhataCustomer) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedCustomer = customer) }
            khataRepository.getCustomerTransactions(customer.customerPhone).collectLatest { txns ->
                _uiState.update { it.copy(customerTransactions = txns) }
            }
        }
    }

    private fun confirmSettlement() {
        viewModelScope.launch {
            val state = _uiState.value
            val cust = state.selectedCustomer ?: return@launch
            val amount = state.settlementAmountInput.toDoubleOrNull() ?: 0.0
            if (amount <= 0.0) {
                _uiState.update { it.copy(errorMessage = "Please enter valid settlement amount") }
                return@launch
            }

            settleKhataUseCase(
                customerPhone = cust.customerPhone,
                customerName = cust.customerName,
                amount = amount,
                note = state.settlementNoteInput
            )

            _uiState.update {
                it.copy(
                    isSettleDialogOpen = false,
                    toastMessage = "Recorded settlement of ₹$amount for ${cust.customerName}!"
                )
            }
        }
    }
}
