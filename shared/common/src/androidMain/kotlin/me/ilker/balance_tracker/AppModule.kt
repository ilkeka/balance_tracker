package me.ilker.balance_tracker

import me.ilker.balance_tracker.auth.SessionStorage
import me.ilker.balance_tracker.auth.createSessionDataStore
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.impl.BalanceTrackerSDKImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val appModule = module {
    single<BalanceTrackerSDK> {
        BalanceTrackerSDKImpl(
            driverFactory = DatabaseDriverFactory(
                context = androidContext()
            ),
            sessionStorage = SessionStorage(
                dataStore = createSessionDataStore(context = androidContext())
            ),
            baseUrl = serverUrl
        )
    }
}
