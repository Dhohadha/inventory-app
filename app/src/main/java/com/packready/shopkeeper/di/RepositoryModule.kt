package com.packready.shopkeeper.di

import com.packready.shopkeeper.data.repository.InvoiceRepositoryImpl
import com.packready.shopkeeper.data.repository.KhataRepositoryImpl
import com.packready.shopkeeper.data.repository.OcrRepositoryImpl
import com.packready.shopkeeper.data.repository.OutboxRepositoryImpl
import com.packready.shopkeeper.data.repository.ProductRepositoryImpl
import com.packready.shopkeeper.data.repository.SaleRepositoryImpl
import com.packready.shopkeeper.domain.repository.InvoiceRepository
import com.packready.shopkeeper.domain.repository.KhataRepository
import com.packready.shopkeeper.domain.repository.OcrRepository
import com.packready.shopkeeper.domain.repository.OutboxRepository
import com.packready.shopkeeper.domain.repository.ProductRepository
import com.packready.shopkeeper.domain.repository.SaleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds
    @Singleton
    abstract fun bindSaleRepository(impl: SaleRepositoryImpl): SaleRepository

    @Binds
    @Singleton
    abstract fun bindKhataRepository(impl: KhataRepositoryImpl): KhataRepository

    @Binds
    @Singleton
    abstract fun bindOutboxRepository(impl: OutboxRepositoryImpl): OutboxRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository
}
