package com.packready.shopkeeper.domain.usecase

import com.packready.shopkeeper.domain.model.Product
import com.packready.shopkeeper.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTopSellingProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(limit: Int = 20): Flow<List<Product>> {
        return productRepository.getTopSellingProducts(limit)
    }
}
