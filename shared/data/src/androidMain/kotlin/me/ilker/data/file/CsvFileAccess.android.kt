package me.ilker.data.file

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Storage Access Framework implementation.
 *
 * The system picker owns the location, so no storage permission is needed. [saveDialogTitle] and
 * [openDialogTitle] are not used here: SAF shows its own chrome, and the file name already says what
 * is happening.
 */
@Composable
internal actual fun rememberPlatformCsvFileAccess(
    saveDialogTitle: String,
    openDialogTitle: String
): CsvFileAccess {
    val context = LocalContext.current

    var pendingSave by remember { mutableStateOf<CompletableDeferred<Uri?>?>(null) }
    var pendingOpen by remember { mutableStateOf<CompletableDeferred<Uri?>?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        pendingSave?.complete(uri)
        pendingSave = null
    }

    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        pendingOpen?.complete(uri)
        pendingOpen = null
    }

    return remember(context, saveLauncher, openLauncher) {
        object : CsvFileAccess {
            override val isSupported: Boolean = true

            override suspend fun save(fileName: String, contents: String): CsvSaveResult {
                val uri = withContext(Dispatchers.Main) {
                    val deferred = CompletableDeferred<Uri?>()
                    pendingSave = deferred
                    saveLauncher.launch(fileName)
                    deferred.await()
                } ?: return CsvSaveResult.Cancelled

                return withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                            stream.write(contents.toByteArray(Charsets.UTF_8))
                        } ?: error("The selected file could not be opened for writing")
                    }.fold(
                        onSuccess = { CsvSaveResult.Saved },
                        onFailure = { CsvSaveResult.Failed }
                    )
                }
            }

            override suspend fun open(): CsvOpenResult {
                val uri = withContext(Dispatchers.Main) {
                    val deferred = CompletableDeferred<Uri?>()
                    pendingOpen = deferred
                    // Some file providers label CSV as text/plain or a vendor type, so the picker
                    // deliberately shows every file and lets the reader decide.
                    openLauncher.launch(arrayOf("*/*"))
                    deferred.await()
                } ?: return CsvOpenResult.Cancelled

                return withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.readBytes().toString(Charsets.UTF_8)
                        } ?: error("The selected file could not be read")
                    }.fold(
                        onSuccess = { contents -> CsvOpenResult.Loaded(contents) },
                        onFailure = { CsvOpenResult.Failed }
                    )
                }
            }
        }
    }
}
