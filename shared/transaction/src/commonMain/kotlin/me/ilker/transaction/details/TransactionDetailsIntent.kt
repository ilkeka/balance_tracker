package me.ilker.transaction.details

import me.ilker.core.Intent

sealed interface TransactionDetailsIntent: Intent {
    data object DeleteTransaction : TransactionDetailsIntent
}
