package me.ilker.home.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.ilker.balance_tracker.resources.Res
import me.ilker.balance_tracker.resources.nothing_yet
import me.ilker.balance_tracker.resources.start_create_transaction
import me.ilker.balance_tracker.theme.PrimaryDarkColor
import me.ilker.balance_tracker.theme.SecondaryDarkColor
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NoTransactionsContent(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            contentColor = SecondaryDarkColor,
            containerColor = PrimaryDarkColor,
            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.33f),
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.33f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.nothing_yet),
                fontSize = TextUnit(value = 18f, type = TextUnitType.Sp),
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.start_create_transaction)
            )
        }
    }
}
