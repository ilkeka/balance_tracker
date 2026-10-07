package me.ilker.balance_tracker.sdk.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import me.ilker.balance_tracker.database.DB
import me.ilker.balance_tracker.database.DatabaseDriverFactory
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionCategory
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.TransactionType
import me.ilker.balance_tracker.sync.LinkedDevice
import me.ilker.balance_tracker.sync.SyncImportResult
import me.ilker.balance_tracker.sync.SyncSnapshot
import me.ilker.balance_tracker.sync.hexToByteArray
import me.ilker.balance_tracker.sync.hexToString

internal class BalanceTrackerSDKImpl(
    driverFactory: DatabaseDriverFactory
) : BalanceTrackerSDK {
    private val database = DB(driverFactory.createDriver())
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val transactions: Flow<List<TransactionDomainModel>> = database
        .getTransactions()
        
        .asFlow().mapToList(Dispatchers.Default)

    override suspend fun getTransactionById(id: Long): TransactionDomainModel? = database
        .getTransactionById(id = id)
        .executeAsOneOrNull()

    @Throws(Exception::class)
    override suspend fun getTransactions() = database
        .getTransactions()
        .executeAsList()

    override suspend fun addTransaction(
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) = database.addTransaction(
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description
    )

    override suspend fun editTransaction(
        id: Long,
        amount: Double,
        dateTime: String,
        type: TransactionType,
        category: TransactionCategory,
        description: String?
    ) = database.editTransaction(
        id = id,
        amount = amount,
        dateTime = dateTime,
        type = type,
        category = category,
        description = description
    )

    override suspend fun deleteTransaction(id: Long) = database.deleteTransaction(id = id)

    override suspend fun deviceId(): String = database.deviceId()

    override suspend fun exportSyncSnapshot(): SyncSnapshot = database.exportSnapshot()

    override suspend fun importSyncSnapshot(snapshot: SyncSnapshot): SyncImportResult =
        database.importSnapshot(remote = snapshot)

    override suspend fun getPairingSecret(): ByteArray? =
        database.pairing()?.secret?.hexToByteArray()

    override suspend fun getLinkedDevice(): LinkedDevice? = database.pairing()
        ?.let { pairing -> pairing.peerDeviceId?.let { id -> id to pairing } }
        ?.takeIf { (_, pairing) -> pairing.peerFingerprint.isNotEmpty() }
        ?.let { (id, pairing) ->
            LinkedDevice(
                deviceId = id,
                alias = pairing.peerAlias.ifEmpty { LinkedDevice.DefaultAlias },
                fingerprint = pairing.peerFingerprint
            )
        }

    override suspend fun savePairingSecret(secret: ByteArray, linkedDevice: LinkedDevice?) =
        database.savePairing(secret = secret.hexToString(), linkedDevice = linkedDevice)

    override suspend fun clearPairingSecret() = database.clearPairing()

    override suspend fun deviceAlias(): String = database.deviceAlias()

    override suspend fun saveDeviceAlias(alias: String) = database.saveDeviceAlias(alias = alias)
}