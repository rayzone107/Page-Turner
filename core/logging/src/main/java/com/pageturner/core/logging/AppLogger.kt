package com.pageturner.core.logging

/**
 * Application-wide logging interface.
 *
 * All modules log through this interface so the underlying destination
 * (Timber in debug, Crashlytics in release, nothing in tests) is controlled
 * from a single point: the Hilt binding in [LoggingModule] and the tree
 * planted in the Application class.
 *
 * Callers pass an explicit [tag] so log output is filterable by module.
 */
interface AppLogger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable)
    fun e(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable)
}
