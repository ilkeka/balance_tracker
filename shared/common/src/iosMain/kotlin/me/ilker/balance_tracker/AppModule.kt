package me.ilker.balance_tracker

import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.impl.BalanceTrackerSDKImpl
import org.koin.core.context.startKoin
import org.koin.dsl.module

private val appModule = module {
    single<BalanceTrackerSDK> {
        BalanceTrackerSDKImpl(
            driverFactory = DatabaseDriverFactory()
        )
    }
}

fun startKoin() {
    startKoin {
        modules(appModule)
    }
}
