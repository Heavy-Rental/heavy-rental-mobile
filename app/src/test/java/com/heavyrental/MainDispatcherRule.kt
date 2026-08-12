package com.heavyrental

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Points `Dispatchers.Main` (used by `viewModelScope`) at an [UnconfinedTestDispatcher] for the
 * duration of a test, so coroutines launched by [AppViewModel][com.heavyrental.viewmodel.AppViewModel]
 * run eagerly/synchronously instead of on a real Android main looper — no manual scheduler pumping
 * needed for the simple, non-delaying coroutines this app launches.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
