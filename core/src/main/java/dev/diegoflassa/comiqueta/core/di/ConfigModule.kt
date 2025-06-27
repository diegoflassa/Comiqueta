package dev.diegoflassa.comiqueta.core.di

import dev.diegoflassa.comiqueta.core.data.config.IConfig
import dev.diegoflassa.comiqueta.core.data.config.Config
import org.koin.dsl.module

val configModule = module {
    single<IConfig> { Config(get()) }
}