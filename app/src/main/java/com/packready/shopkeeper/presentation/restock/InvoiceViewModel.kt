package com.packready.shopkeeper.presentation.restock

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.packready.shopkeeper.data.hardware.HapticManager
import com.packready.shopkeeper.data.hardware.SoundPoolManager
import com.packready.shopkeeper.domain.model.InvoiceDocument
import com.packready.shopkeeper.domain.model.InvoiceLineItem
import com.packready.shopkeeper.domain.repository.InvoiceRepository
import com.packready.shopkeeper.domain.usecase.ProcessInvoiceOcrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InvoiceUiState(
    val invoices: List<InvoiceDocument> = emptyList(),
    val activeInvoice: InvoiceDocument? = null,
    val isOcrProcessing: Boolean = false,
    val geminiApiKey: String = "",
    val toastMessage: String? = null,
    val errorMessage: String? = null
)

sealed interface InvoiceUiEvent {
    data class ProcessInvoiceImage(val bitmap: Bitmap) : InvoiceUiEvent
    data class SelectInvoice(val invoice: InvoiceDocument) : InvoiceUiEvent
    data class ToggleLineItemConfirmation(val itemId: String) : InvoiceUiEvent
    data class UpdateLineItemQuantity(val itemId: String, val newQty: Double) : InvoiceUiEvent
    data class UpdateLineItemRate(val itemId: String, val newRate: Double) : InvoiceUiEvent
    data class ConfirmAndReconcileStock(val invoiceId: String) : InvoiceUiEvent
    object ClearMessages : InvoiceUiEvent
}

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val processInvoiceOcrUseCase: ProcessInvoiceOcrUseCase,
    private val soundPoolManager: SoundPoolManager,
    private val hapticManager: HapticManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    init {
        observeInvoices()
    }

    private fun observeInvoices() {
        viewModelScope.launch {
            invoiceRepository.getAllInvoices().collectLatest { list ->
                _uiState.update { it.copy(invoices = list) }
            }
        }
    }

    fun onEvent(event: InvoiceUiEvent) {
        when (event) {
            is InvoiceUiEvent.ProcessInvoiceImage -> processImage(event.bitmap)
            is InvoiceUiEvent.SelectInvoice -> _uiState.update { it.copy(activeInvoice = event.invoice) }
            is InvoiceUiEvent.ToggleLineItemConfirmation -> toggleItem(event.itemId)
            is InvoiceUiEvent.UpdateLineItemQuantity -> updateQty(event.itemId, event.newQty)
            is InvoiceUiEvent.UpdateLineItemRate -> updateRate(event.itemId, event.newRate)
            is InvoiceUiEvent.ConfirmAndReconcileStock -> reconcileStock(event.invoiceId)
            is InvoiceUiEvent.ClearMessages -> _uiState.update { it.copy(toastMessage = null, errorMessage = null) }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update { it.copy(isOcrProcessing = true, errorMessage = null) }
            val apiKey = _uiState.value.geminiApiKey
            val result = processInvoiceOcrUseCase(bitmap, apiKey)
            if (result.isSuccess) {
                val doc = result.getOrNull()
                soundPoolManager.playSaleSuccess()
                hapticManager.pulseSaleSuccess()
                _uiState.update {
                    it.copy(
                        isOcrProcessing = false,
                        activeInvoice = doc,
                        toastMessage = "Extracted ${doc?.items?.size ?: 0} line items from invoice!"
                    )
                }
            } else {
                soundPoolManager.playErrorBuzz()
                hapticManager.pulseError()
                _uiState.update {
                    it.copy(
                        isOcrProcessing = false,
                        errorMessage = "Failed to parse distributor invoice. Check image clarity."
                    )
                }
            }
        }
    }

    private fun toggleItem(itemId: String) {
        _uiState.update { state ->
            val invoice = state.activeInvoice ?: return@update state
            val updatedItems = invoice.items.map { item ->
                if (item.id == itemId) item.copy(isConfirmed = !item.isConfirmed) else item
            }
            state.copy(activeInvoice = invoice.copy(items = updatedItems))
        }
    }

    private fun updateQty(itemId: String, newQty: Double) {
        _uiState.update { state ->
            val invoice = state.activeInvoice ?: return@update state
            val updatedItems = invoice.items.map { item ->
                if (item.id == itemId) item.copy(quantity = newQty, totalAmount = newQty * item.purchaseRate) else item
            }
            state.copy(activeInvoice = invoice.copy(items = updatedItems))
        }
    }

    private fun updateRate(itemId: String, newRate: Double) {
        _uiState.update { state ->
            val invoice = state.activeInvoice ?: return@update state
            val updatedItems = invoice.items.map { item ->
                if (item.id == itemId) item.copy(purchaseRate = newRate, totalAmount = item.quantity * newRate) else item
            }
            state.copy(activeInvoice = invoice.copy(items = updatedItems))
        }
    }

    private fun reconcileStock(invoiceId: String) {
        viewModelScope.launch {
            val invoice = _uiState.value.activeInvoice ?: return@launch
            // Save updated items first
            invoiceRepository.saveInvoice(invoice)
            // Apply stock updates to Room DB
            invoiceRepository.reconcileInvoiceAndApplyStock(invoiceId)

            soundPoolManager.playSaleSuccess()
            hapticManager.pulseSaleSuccess()

            _uiState.update {
                it.copy(
                    activeInvoice = null,
                    toastMessage = "Inventory successfully synced with invoice ${invoice.invoiceNumber}!"
                )
            }
        }
    }
}
