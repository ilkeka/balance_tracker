package me.ilker.data.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal actual fun rememberPlatformCsvFileAccess(
    saveDialogTitle: String,
    openDialogTitle: String
): CsvFileAccess = remember { UnsupportedCsvFileAccess }
