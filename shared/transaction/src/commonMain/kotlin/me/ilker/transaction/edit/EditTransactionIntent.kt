package me.ilker.transaction.edit

import me.ilker.core.Intent

sealed interface EditTransactionIntent: Intent {
    data object DeleteTransaction : EditTransactionIntent
}
