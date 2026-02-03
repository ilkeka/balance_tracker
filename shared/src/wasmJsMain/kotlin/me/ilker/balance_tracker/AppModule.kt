package me.ilker.balance_tracker

import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.impl.BalanceTrackerSDKImpl
import org.koin.dsl.module

val appModule = module {
    single<BalanceTrackerSDK> {
        BalanceTrackerSDKImpl(
            driverFactory = DatabaseDriverFactory()
        )
    }
}
