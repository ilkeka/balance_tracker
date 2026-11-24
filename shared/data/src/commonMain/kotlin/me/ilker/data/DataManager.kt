package me.ilker.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.ilker.balance_tracker.sdk.BalanceTrackerSDK
import me.ilker.balance_tracker.sdk.TransactionDomainModel
import me.ilker.balance_tracker.sdk.getLocalDate
import me.ilker.core.Manager
import me.ilker.data.csv.CsvDocument
import me.ilker.data.csv.TransactionCsv
import kotlin.time.Clock

class DataManager(
    private val sdk: BalanceTrackerSDK
) : Manager<DataState, DataIntent, DataSideEffect>() {
    private val managerState = MutableStateFlow(DataState())

    override val state: StateFlow<DataState> = managerState.asStateFlow()
    override val sideEffect: Channel<DataSideEffect> = Channel(capacity = Channel.BUFFERED)

    override fun sendIntent(intent: DataIntent) {
        when (intent) {
            DataIntent.Export -> export()
            is DataIntent.ExportSaved -> finishExport(DataOutcome.Exported(intent.transactionCount))
            DataIntent.ExportCancelled -> finishExport(outcome = null)
            DataIntent.ExportFailed -> finishExport(DataOutcome.ExportFailed)

            DataIntent.Import -> requestImport()
            is DataIntent.ImportCsv -> import(contents = intent.contents)
            DataIntent.ImportCancelled -> finishImport(outcome = null)
            DataIntent.ImportFailed -> finishImport(DataOutcome.ImportFailed)

            DataIntent.DismissOutcome -> managerState.update { it.copy(outcome = null) }
        }
    }

    private fun export() {
        if (managerState.value.isBusy) return

        managerState.update { it.copy(isBusy = true, outcome = null) }
        scope.launch {
            val transactions = runCatching { sdk.getTransactions() }
                .getOrElse {
                    finishExport(DataOutcome.ExportFailed)
                    return@launch
                }
                // Chronological order keeps a spreadsheet readable; the id breaks same-day ties so
                // repeated exports of an unchanged ledger produce the same file.
                .sortedWith(
                    compareBy<TransactionDomainModel> { transaction -> transaction.getLocalDate() }
                        .thenBy { transaction -> transaction.id }
                )

            if (transactions.isEmpty()) {
                finishExport(DataOutcome.ExportEmpty)
                return@launch
            }

            // The dialog result comes back as a separate intent, so the busy state stays set until
            // the file has actually been written or the user has dismissed the picker.
            sideEffect.send(
                DataSideEffect.SaveCsv(
                    fileName = exportFileName(),
                    contents = TransactionCsv.encode(transactions),
                    transactionCount = transactions.size
                )
            )
        }
    }

    private fun requestImport() {
        if (managerState.value.isBusy) return

        managerState.update { it.copy(isBusy = true, outcome = null) }
        scope.launch { sideEffect.send(DataSideEffect.OpenCsv) }
    }

    private fun import(contents: String) {
        scope.launch {
            when (val document = TransactionCsv.decode(contents)) {
                CsvDocument.Unreadable -> finishImport(DataOutcome.ImportUnreadable)

                is CsvDocument.Parsed -> {
                    if (document.transactions.isEmpty()) {
                        finishImport(DataOutcome.ImportEmpty)
                        return@launch
                    }

                    val result = runCatching { sdk.importTransactions(document.transactions) }
                        .getOrElse {
                            finishImport(DataOutcome.ImportFailed)
                            return@launch
                        }

                    finishImport(
                        DataOutcome.Imported(
                            imported = result.imported,
                            skipped = result.skipped,
                            invalidRows = document.invalidRows
                        )
                    )
                }
            }
        }
    }

    private fun finishExport(outcome: DataOutcome?) {
        managerState.update { it.copy(isBusy = false, outcome = outcome) }
    }

    private fun finishImport(outcome: DataOutcome?) {
        managerState.update { it.copy(isBusy = false, outcome = outcome) }
    }

    /** A dated name keeps repeated exports from silently replacing each other. */
    private fun exportFileName(): String {
        val date = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return "balance-tracker-$date.csv"
    }
}
