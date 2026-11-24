package me.ilker.data.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
internal actual fun rememberPlatformCsvFileAccess(
    saveDialogTitle: String,
    openDialogTitle: String
): CsvFileAccess = remember(saveDialogTitle, openDialogTitle) {
    DesktopCsvFileAccess(
        saveDialogTitle = saveDialogTitle,
        openDialogTitle = openDialogTitle
    )
}

/**
 * Desktop picker built on Swing so the dialog is modal to the Compose window.
 *
 * Both dialogs run on the Swing event thread: opening them from a background dispatcher would either
 * block it or paint a dialog that never responds.
 */
private class DesktopCsvFileAccess(
    private val saveDialogTitle: String,
    private val openDialogTitle: String
) : CsvFileAccess {
    override val isSupported: Boolean = true

    override suspend fun save(fileName: String, contents: String): CsvSaveResult =
        withContext(Dispatchers.Swing) {
            val chooser = JFileChooser().apply {
                dialogTitle = saveDialogTitle
                selectedFile = File(fileName)
                fileFilter = FileNameExtensionFilter("CSV", "csv")
            }

            if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
                return@withContext CsvSaveResult.Cancelled
            }

            val selected = chooser.selectedFile ?: return@withContext CsvSaveResult.Failed
            val target = selected.withCsvExtension()

            runCatching { target.writeText(contents, Charsets.UTF_8) }.fold(
                onSuccess = { CsvSaveResult.Saved },
                onFailure = { CsvSaveResult.Failed }
            )
        }

    override suspend fun open(): CsvOpenResult = withContext(Dispatchers.Swing) {
        val chooser = JFileChooser().apply {
            dialogTitle = openDialogTitle
            fileFilter = FileNameExtensionFilter("CSV", "csv")
        }

        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
            return@withContext CsvOpenResult.Cancelled
        }

        val selected = chooser.selectedFile ?: return@withContext CsvOpenResult.Failed

        runCatching { selected.readText(Charsets.UTF_8) }.fold(
            onSuccess = { contents -> CsvOpenResult.Loaded(contents) },
            onFailure = { CsvOpenResult.Failed }
        )
    }

    /** The save dialog does not append the extension on every platform, so it is added here. */
    private fun File.withCsvExtension(): File =
        if (extension.equals("csv", ignoreCase = true)) this else File(parentFile, "$name.csv")
}
