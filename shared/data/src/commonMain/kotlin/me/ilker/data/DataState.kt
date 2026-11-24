package me.ilker.data

import me.ilker.core.Intent
import me.ilker.core.SideEffect
import me.ilker.core.State

/** What the data screen has to report after the last action. */
sealed interface DataOutcome {
    data class Exported(val transactionCount: Int) : DataOutcome
    data object ExportEmpty : DataOutcome
    data object ExportFailed : DataOutcome

    data class Imported(
        val imported: Int,
        val skipped: Int,
        val invalidRows: Int
    ) : DataOutcome

    data object ImportEmpty : DataOutcome
    data object ImportUnreadable : DataOutcome
    data object ImportFailed : DataOutcome
}

data class DataState(
    /** True while a file dialog is open or a file is being read or written. */
    val isBusy: Boolean = false,
    val outcome: DataOutcome? = null
) : State

sealed interface DataIntent : Intent {
    data object Export : DataIntent
    data class ExportSaved(val transactionCount: Int) : DataIntent
    data object ExportCancelled : DataIntent
    data object ExportFailed : DataIntent

    data object Import : DataIntent
    data class ImportCsv(val contents: String) : DataIntent
    data object ImportCancelled : DataIntent
    data object ImportFailed : DataIntent

    data object DismissOutcome : DataIntent
}

/**
 * The platform-specific half of the flow.
 *
 * The manager prepares and applies data, while the screen owns the file dialog: Android needs an
 * activity result launcher and desktop needs the AWT event thread, neither of which belongs in a
 * plain coroutine-scoped manager.
 */
sealed interface DataSideEffect : SideEffect {
    data class SaveCsv(
        val fileName: String,
        val contents: String,
        val transactionCount: Int
    ) : DataSideEffect

    data object OpenCsv : DataSideEffect
}
