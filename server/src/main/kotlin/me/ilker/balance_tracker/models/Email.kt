package me.ilker.balance_tracker.models

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
value class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email must not be blank" }
        require('@' in value && value.indexOf('@') == value.lastIndexOf('@')) { "Email must contain exactly one '@'" }
        val (local, domain) = value.split("@")
        require(local.isNotBlank()) { "Email local part must not be blank" }
        require(domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')) { "Email domain must be valid" }
    }
}
