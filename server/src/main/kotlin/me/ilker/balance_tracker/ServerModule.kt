package me.ilker.balance_tracker

import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.impl.DBImpl
import org.koin.dsl.module

internal val serverModule = module {
    single<DB> {
        DBImpl(
            databaseDriverFactory = DatabaseDriverFactoryImpl()
        )
    }
}
