package me.ilker.sync.platform

/**
 * The name this device publishes to nearby peers.
 *
 * The user-visible name is preferred over the model, so two phones of the same model can still be
 * told apart. Targets without a user-assigned device name fall back to their model.
 */
internal expect fun deviceDisplayName(): String

/** The model shown as the subtitle of this device in another device's picker. */
internal expect fun deviceModel(): String
