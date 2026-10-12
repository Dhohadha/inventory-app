package com.packready.shopkeeper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentBreakdown(
    val cashTotal: Double = 0.0,
    val upiTotal: Double = 0.0,
    val khataTotal: Double = 0.0,
    val cardTotal: Double = 0.0
)

@Serializable
data class DailyAnalytics(
    val dateString: String,
    val totalRevenue: Double = 0.0,
    val totalCost: Double = 0.0,
    val grossProfit: Double = 0.0,
    val profitMarginPercent: Double = 0.0,
    val totalTransactionsCount: Int = 0,
    val totalUnitsSold: Double = 0.0,
    val paymentBreakdown: PaymentBreakdown = PaymentBreakdown(),
    val topSellingProducts: List<Pair<String, Double>> = emptyList() // Pair(Name, Qty)
)
