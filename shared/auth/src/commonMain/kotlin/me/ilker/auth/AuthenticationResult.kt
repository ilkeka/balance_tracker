package me.ilker.auth

enum class AuthenticationResult {
    Failed,
    InvalidCredentials,
    InvalidInput,
    RateLimited
}