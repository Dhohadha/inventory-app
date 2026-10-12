package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.domain.model.DailyAnalytics
import com.packready.shopkeeper.domain.repository.SaleRepository
import java.util.Calendar
import javax.inject.Inject

class GetDailyAnalyticsUseCase @Inject constructor(
    private val saleRepository: SaleRepository
) {
    suspend operator fun invoke(calendar: Calendar = Calendar.getInstance()): DailyAnalytics {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfDay = cal.timeInMillis

        return saleRepository.getDailyAnalytics(startOfDay, endOfDay)
    }
}
