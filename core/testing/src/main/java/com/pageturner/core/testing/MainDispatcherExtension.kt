package com.pageturner.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension that installs a [StandardTestDispatcher] as the main dispatcher
 * before each test and resets it afterward.
 *
 * Usage via @RegisterExtension for access to the dispatcher instance:
 * ```kotlin
 * @ExtendWith(MockKExtension::class)
 * class MyViewModelTest {
 *     @JvmField
 *     @RegisterExtension
 *     val mainDispatcherExtension = MainDispatcherExtension()
 *     private val testDispatcher get() = mainDispatcherExtension.testDispatcher
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
}
