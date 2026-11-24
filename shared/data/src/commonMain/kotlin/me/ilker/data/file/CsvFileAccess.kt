package me.ilker.data.file

import androidx.compose.runtime.Composable
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.data_open_dialog_title
import me.ilker.balance_tracker.resources.data_save_dialog_title
import org.jetbrains.compose.resources.stringResource

internal sealed interface CsvSaveResult {
    data object Saved : CsvSaveResult
    data object Cancelled : CsvSaveResult
    data object Failed : CsvSaveResult
}

internal sealed interface CsvOpenResult {
    data class Loaded(val contents: String) : CsvOpenResult
    data object Cancelled : CsvOpenResult
    data object Failed : CsvOpenResult
}

/**
 * Reads and writes a CSV file through the platform's own picker.
 *
 * Implementations are composable factories because the pickers are not plain suspend calls: Android
 * needs an activity result launcher, desktop has to run its dialog on the AWT event thread, and the
 * browser and iOS builds do not offer the feature yet.
 */
internal interface CsvFileAccess {
    val isSupported: Boolean

    suspend fun save(fileName: String, contents: String): CsvSaveResult
    suspend fun open(): CsvOpenResult
}

@Composable
internal fun rememberCsvFileAccess(): CsvFileAccess = rememberPlatformCsvFileAccess(
    saveDialogTitle = stringResource(Res.string.data_save_dialog_title),
    openDialogTitle = stringResource(Res.string.data_open_dialog_title)
)

@Composable
internal expect fun rememberPlatformCsvFileAccess(
    saveDialogTitle: String,
    openDialogTitle: String
): CsvFileAccess

/** Used by targets where the screen reports that the feature is not available yet. */
internal object UnsupportedCsvFileAccess : CsvFileAccess {
    override val isSupported: Boolean = false

    override suspend fun save(fileName: String, contents: String): CsvSaveResult = CsvSaveResult.Failed

    override suspend fun open(): CsvOpenResult = CsvOpenResult.Failed
}
