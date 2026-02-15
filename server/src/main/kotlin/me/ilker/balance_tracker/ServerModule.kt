package me.ilker.balance_tracker

import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.database.ServerDB
import me.ilker.balance_tracker.database.impl.ServerDBImpl
import org.koin.dsl.module

internal val serverModule = module {
    single<ServerDB> {
        ServerDBImpl(
            databaseDriverFactory = DatabaseDriverFactory()
        )
    }
}
