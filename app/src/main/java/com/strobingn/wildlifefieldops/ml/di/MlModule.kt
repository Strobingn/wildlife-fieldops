package com.strobingn.wildlifefieldops.ml.di

import android.content.Context
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalyzer
import com.strobingn.wildlifefieldops.ml.vision.MlKitTaxonomyVisionAnalyzer
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyCatalog
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MlBindingsModule {

    @Binds
    @Singleton
    abstract fun bindVisionAnalyzer(impl: MlKitTaxonomyVisionAnalyzer): VisionAnalyzer
}

@Module
@InstallIn(SingletonComponent::class)
object MlProvidesModule {

    @Provides
    @Singleton
    fun provideTaxonomyCatalog(@ApplicationContext context: Context): TaxonomyCatalog =
        TaxonomyCatalog.load(context)

    @Provides
    @Singleton
    fun provideTaxonomyMapper(catalog: TaxonomyCatalog): TaxonomyMapper =
        TaxonomyMapper(catalog)
}
