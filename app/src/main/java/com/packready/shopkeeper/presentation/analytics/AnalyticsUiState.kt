package com.packready.shopkeeper.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.BluetoothPrinterManager
import com.packready.shopkeeper.domain.model.DailyAnalytics
import com.packready.shopkeeper.domain.model.SaleTransaction
import com.packready.shopkeeper.domain.repository.SaleRepository
import com.packready.shopkeeper.domain.usecase.GetDailyAnalyticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsUiState(
    val dailyAnalytics: DailyAnalytics? = null,
    val recentTransactions: List<SaleTransaction> = emptyList(),
    val selectedSaleForReceipt: SaleTransaction? = null,
    val toastMessage: String? = null
)

sealed interface AnalyticsUiEvent {
    data class SelectSaleReceipt(val sale: SaleTransaction) : AnalyticsUiEvent
    object DismissReceipt : AnalyticsUiEvent
    object PrintReceiptBluetooth : AnalyticsUiEvent
    object Refresh : AnalyticsUiEvent
    object ClearMessages : AnalyticsUiEvent
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val saleRepository: SaleRepository,
    private val getDailyAnalyticsUseCase: GetDailyAnalyticsUseCase,
    private val bluetoothPrinterManager: BluetoothPrinterManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onEvent(event: AnalyticsUiEvent) {
        when (event) {
            is AnalyticsUiEvent.SelectSaleReceipt -> _uiState.update { it.copy(selectedSaleForReceipt = event.sale) }
            is AnalyticsUiEvent.DismissReceipt -> _uiState.update { it.copy(selectedSaleForReceipt = null) }
            is AnalyticsUiEvent.PrintReceiptBluetooth -> printCurrentReceipt()
            is AnalyticsUiEvent.Refresh -> loadData()
            is AnalyticsUiEvent.ClearMessages -> _uiState.update { it.copy(toastMessage = null) }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val stats = getDailyAnalyticsUseCase(Calendar.getInstance())
            _uiState.update { it.copy(dailyAnalytics = stats) }

            saleRepository.getAllTransactions().collectLatest { list ->
                _uiState.update { it.copy(recentTransactions = list) }
            }
        }
    }

    private fun printCurrentReceipt() {
        viewModelScope.launch {
            val sale = _uiState.value.selectedSaleForReceipt ?: return@launch
            val printers = bluetoothPrinterManager.getPairedPrinters()
            if (printers.isNotEmpty()) {
                val addr = printers.first().second
                bluetoothPrinterManager.printReceipt(addr, sale)
                _uiState.update { it.copy(toastMessage = "Receipt reprinted via Bluetooth!") }
            } else {
                _uiState.update { it.copy(toastMessage = "No paired thermal printer found.") }
            }
        }
    }
}
