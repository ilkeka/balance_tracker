package me.ilker.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.receiveAsFlow
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.back
import me.ilker.balance_tracker.resources.data_export_button
import me.ilker.balance_tracker.resources.data_export_description
import me.ilker.balance_tracker.resources.data_export_empty
import me.ilker.balance_tracker.resources.data_export_failed
import me.ilker.balance_tracker.resources.data_export_saved
import me.ilker.balance_tracker.resources.data_export_title
import me.ilker.balance_tracker.resources.data_import_button
import me.ilker.balance_tracker.resources.data_import_description
import me.ilker.balance_tracker.resources.data_import_empty
import me.ilker.balance_tracker.resources.data_import_failed
import me.ilker.balance_tracker.resources.data_import_invalid_rows
import me.ilker.balance_tracker.resources.data_import_result
import me.ilker.balance_tracker.resources.data_import_title
import me.ilker.balance_tracker.resources.data_import_unreadable
import me.ilker.balance_tracker.resources.data_intro
import me.ilker.balance_tracker.resources.data_not_supported
import me.ilker.balance_tracker.resources.data_title
import me.ilker.data.file.CsvOpenResult
import me.ilker.data.file.CsvSaveResult
import me.ilker.data.file.rememberCsvFileAccess
import org.jetbrains.compose.resources.stringResource

@Composable
fun DataRoute(
    manager: DataManager,
    onBack: () -> Unit
) {
    val state by manager.state.collectAsStateWithLifecycle()
    val fileAccess = rememberCsvFileAccess()

    // The manager decides what has to happen; this only performs the dialog and reports back, so a
    // cancelled picker leaves the state exactly as it was.
    LaunchedEffect(manager) {
        manager.sideEffect.receiveAsFlow().collect { effect ->
            when (effect) {
                is DataSideEffect.SaveCsv -> when (fileAccess.save(effect.fileName, effect.contents)) {
                    CsvSaveResult.Saved -> manager.sendIntent(DataIntent.ExportSaved(effect.transactionCount))
                    CsvSaveResult.Cancelled -> manager.sendIntent(DataIntent.ExportCancelled)
                    CsvSaveResult.Failed -> manager.sendIntent(DataIntent.ExportFailed)
                }

                DataSideEffect.OpenCsv -> when (val result = fileAccess.open()) {
                    is CsvOpenResult.Loaded -> manager.sendIntent(DataIntent.ImportCsv(result.contents))
                    CsvOpenResult.Cancelled -> manager.sendIntent(DataIntent.ImportCancelled)
                    CsvOpenResult.Failed -> manager.sendIntent(DataIntent.ImportFailed)
                }
            }
        }
    }

    DataView(
        state = state,
        isSupported = fileAccess.isSupported,
        onExport = { manager.sendIntent(DataIntent.Export) },
        onImport = { manager.sendIntent(DataIntent.Import) },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataView(
    state: DataState,
    isSupported: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { DataTopBar(onBack = onBack) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(Res.string.data_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isSupported) {
                item { NotSupportedCard() }
            }

            item {
                ActionCard(
                    icon = Icons.Rounded.FileUpload,
                    title = stringResource(Res.string.data_export_title),
                    description = stringResource(Res.string.data_export_description),
                    buttonLabel = stringResource(Res.string.data_export_button),
                    enabled = isSupported && !state.isBusy,
                    onClick = onExport
                )
            }

            item {
                ActionCard(
                    icon = Icons.Rounded.FileDownload,
                    title = stringResource(Res.string.data_import_title),
                    description = stringResource(Res.string.data_import_description),
                    buttonLabel = stringResource(Res.string.data_import_button),
                    enabled = isSupported && !state.isBusy,
                    onClick = onImport
                )
            }

            if (state.isBusy) {
                item {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }
            }

            state.outcome?.let { outcome ->
                item { OutcomeCard(outcome = outcome) }
            }
        }
    }
}

@Composable
private fun DataTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .padding(top = 48.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(Res.string.back)
            )
        }

        Spacer(Modifier.width(4.dp))

        Text(
            text = stringResource(Res.string.data_title),
            fontSize = TextUnit(value = 24f, type = TextUnitType.Sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun NotSupportedCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DataCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text(
            text = stringResource(Res.string.data_not_supported),
            modifier = Modifier
                .fillMaxWidth()
                .padding(DataCardPadding),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DataCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DataCardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onClick,
                enabled = enabled,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = buttonLabel)
            }
        }
    }
}

@Composable
private fun OutcomeCard(outcome: DataOutcome) {
    val isError = when (outcome) {
        DataOutcome.ExportFailed, DataOutcome.ImportFailed, DataOutcome.ImportUnreadable -> true
        else -> false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = DataCardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DataCardPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = outcome.primaryMessage(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (outcome is DataOutcome.Imported && outcome.invalidRows > 0) {
                Text(
                    text = stringResource(
                        Res.string.data_import_invalid_rows,
                        outcome.invalidRows
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DataOutcome.primaryMessage(): String = when (this) {
    is DataOutcome.Exported -> stringResource(Res.string.data_export_saved, transactionCount)
    DataOutcome.ExportEmpty -> stringResource(Res.string.data_export_empty)
    DataOutcome.ExportFailed -> stringResource(Res.string.data_export_failed)
    is DataOutcome.Imported -> stringResource(Res.string.data_import_result, imported, skipped)
    DataOutcome.ImportEmpty -> stringResource(Res.string.data_import_empty)
    DataOutcome.ImportUnreadable -> stringResource(Res.string.data_import_unreadable)
    DataOutcome.ImportFailed -> stringResource(Res.string.data_import_failed)
}

private val DataCardShape = RoundedCornerShape(24.dp)
private val DataCardPadding = 20.dp
