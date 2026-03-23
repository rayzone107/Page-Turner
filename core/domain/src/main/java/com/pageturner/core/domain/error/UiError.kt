package com.pageturner.core.domain.error

/**
 * Presentation-layer error shown to the user.
 * Mapped from [AppError] in ViewModels — never constructed below the VM layer.
 */
data class UiError(
    val title: String,
    val message: String,
    val isRetryable: Boolean
)
