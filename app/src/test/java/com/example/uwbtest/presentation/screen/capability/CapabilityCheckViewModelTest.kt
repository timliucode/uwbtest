package com.example.uwbtest.presentation.screen.capability

import app.cash.turbine.test
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.model.UwbCapabilityStore
import com.example.uwbtest.domain.usecase.CheckUwbCapabilityUseCase
import com.example.uwbtest.util.MainDispatcherExtension
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CapabilityCheckViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherRule = MainDispatcherExtension()

    private lateinit var checkCapability: CheckUwbCapabilityUseCase
    private lateinit var capabilityStore: UwbCapabilityStore
    private lateinit var viewModel: CapabilityCheckViewModel

    @BeforeEach
    fun setUp() {
        checkCapability = mock()
        capabilityStore = UwbCapabilityStore()
        viewModel = CapabilityCheckViewModel(checkCapability, capabilityStore)
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
    fun `check - success writes capability to store`() = runTest {
        val capability = UwbCapability(hardwarePresent = true, isAvailable = true)
        whenever(checkCapability()).thenReturn(capability)

        viewModel.check()

        assertThat(capabilityStore.lastCapability).isEqualTo(capability)
    }

    @Test
    fun `onPermissionDenied - sets PermissionDenied state`() {
        viewModel.onPermissionDenied()

        assertThat(viewModel.uiState.value)
            .isEqualTo(CapabilityCheckViewModel.UiState.PermissionDenied)
    }

    @Test
    fun `deviceInfo - is populated on construction`() {
        assertThat(viewModel.deviceInfo.manufacturer).isNotNull()
        assertThat(viewModel.deviceInfo.model).isNotNull()
        assertThat(viewModel.deviceInfo.androidVersion).isNotNull()
    }
}
