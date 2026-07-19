package com.strobingn.wildlifefieldops.ml.di

import android.content.Context
import com.strobingn.wildlifefieldops.ml.domain.MultimodalFusionEngine
import com.strobingn.wildlifefieldops.ml.domain.VisionAnalyzer
import com.strobingn.wildlifefieldops.ml.domain.VoiceJobParser
import com.strobingn.wildlifefieldops.ml.fusion.DefaultMultimodalFusionEngine
import com.strobingn.wildlifefieldops.ml.vision.MlKitTaxonomyVisionAnalyzer
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyCatalog
import com.strobingn.wildlifefieldops.ml.vision.TaxonomyMapper
import com.strobingn.wildlifefieldops.ml.voice.GrokVoiceJobParser
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

    /**
     * Primary voice parser: Grok when key present, always falls back to regex internally.
     * Concrete [com.strobingn.wildlifefieldops.ml.voice.RegexVoiceJobParser] is also
     * `@Inject` constructible for tests and as Grok's offline fallback dependency.
     */
    @Binds
    @Singleton
    abstract fun bindVoiceJobParser(impl: GrokVoiceJobParser): VoiceJobParser

    @Binds
    @Singleton
    abstract fun bindMultimodalFusionEngine(impl: DefaultMultimodalFusionEngine): MultimodalFusionEngine
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
