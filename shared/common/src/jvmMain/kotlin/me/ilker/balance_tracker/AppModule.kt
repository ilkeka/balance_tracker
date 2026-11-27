package me.ilker.balance_tracker

import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.preferences.AppPreferences
import me.ilker.balance_tracker.preferences.JvmAppPreferences
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.impl.BalanceTrackerSDKImpl
import org.koin.dsl.module

actual val appModule = module {
    single<AppPreferences> {
        JvmAppPreferences()
    }

    single<BalanceTrackerSDK> {
        BalanceTrackerSDKImpl(
            driverFactory = DatabaseDriverFactory()
        )
    }
}