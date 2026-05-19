package com.example.uwbtest.presentation.screen.capability

import app.cash.turbine.test
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.usecase.CheckUwbCapabilityUseCase
import com.example.uwbtest.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)

class CapabilityCheckViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var checkCapability: CheckUwbCapabilityUseCase
    private lateinit var viewModel: CapabilityCheckViewModel

    @Before
    fun setUp() {
        checkCapability = mock()
        viewModel = CapabilityCheckViewModel(checkCapability)
    }

    @Test
    fun `initial state is Idle`() {
        assertThat(viewModel.uiState.value).isEqualTo(CapabilityCheckViewModel.UiState.Idle)
    }

    @Test
    fun `check - emits Loading then Success`() = runTest {
        val capability = UwbCapability(hardwarePresent = true, isAvailable = true)
        whenever(checkCapability()).thenReturn(capability)

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CapabilityCheckViewModel.UiState.Idle)
            viewModel.check()
            // Loading may be consumed quickly with UnconfinedTestDispatcher
            val nextItems = buildList {
                val item1 = awaitItem()
                add(item1)
                if (item1 !is CapabilityCheckViewModel.UiState.Success) {
                    add(awaitItem())
                }
            }
            assertThat(nextItems.last()).isEqualTo(CapabilityCheckViewModel.UiState.Success(capability))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `check - emits Loading then Error when exception thrown`() = runTest {
        whenever(checkCapability()).thenThrow(RuntimeException("boom"))

        viewModel.uiState.test {
            assertThat(awaitItem()).isEqualTo(CapabilityCheckViewModel.UiState.Idle)
            viewModel.check()
            val items = buildList {
                val item1 = awaitItem()
                add(item1)
                if (item1 !is CapabilityCheckViewModel.UiState.Error) {
                    add(awaitItem())
                }
            }
            assertThat(items.last()).isEqualTo(CapabilityCheckViewModel.UiState.Error("boom"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `check - uses unknown error message when exception message is null`() = runTest {
        whenever(checkCapability()).thenThrow(RuntimeException(null as String?))

        viewModel.check()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(CapabilityCheckViewModel.UiState.Error::class.java)
        assertThat((state as CapabilityCheckViewModel.UiState.Error).message).isEqualTo("Unknown error")
    }

    @Test
    fun `onPermissionDenied - sets Error state with permission denied message`() {
        viewModel.onPermissionDenied()

        assertThat(viewModel.uiState.value).isEqualTo(
            CapabilityCheckViewModel.UiState.Error("UWB_RANGING permission denied. Please grant it in Settings.")
        )
    }

    @Test
    fun `deviceInfo - is populated on construction`() {
        assertThat(viewModel.deviceInfo.manufacturer).isNotNull()
        assertThat(viewModel.deviceInfo.model).isNotNull()
        assertThat(viewModel.deviceInfo.androidVersion).isNotNull()
    }
}
