package com.example.uwbtest.presentation.screen.oob

import androidx.lifecycle.SavedStateHandle
import com.example.uwbtest.domain.model.UwbDeviceInfo
import com.example.uwbtest.domain.model.UwbRole
import com.example.uwbtest.domain.usecase.GetLocalAddressUseCase
import com.example.uwbtest.util.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class OobExchangeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getLocalAddress: GetLocalAddressUseCase

    private val controllerDeviceInfo = UwbDeviceInfo(
        localAddress = byteArrayOf(0xA1.toByte(), 0xB2.toByte()),
        role = UwbRole.Controller,
        channelNumber = 9,
        preambleIndex = 10,
    )

    private val controleeDeviceInfo = UwbDeviceInfo(
        localAddress = byteArrayOf(0xC3.toByte(), 0xD4.toByte()),
        role = UwbRole.Controlee,
    )

    @Before
    fun setUp() {
        getLocalAddress = mock()
    }

    private fun buildViewModel(isController: Boolean = true): OobExchangeViewModel {
        return OobExchangeViewModel(
            savedStateHandle = SavedStateHandle(mapOf("isController" to isController)),
            getLocalAddress = getLocalAddress,
        )
    }

    @Test
    fun `isController - reads true from SavedStateHandle`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel(isController = true)
        assertThat(vm.isController).isTrue()
    }

    @Test
    fun `isController - reads false from SavedStateHandle`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controleeDeviceInfo))
        val vm = buildViewModel(isController = false)
        assertThat(vm.isController).isFalse()
    }

    @Test
    fun `init - on success sets localDeviceInfo and clears loading`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.success(controllerDeviceInfo))

        val vm = buildViewModel(isController = true)

        assertThat(vm.uiState.value.isLoadingAddress).isFalse()
        assertThat(vm.uiState.value.localDeviceInfo).isEqualTo(controllerDeviceInfo)
        assertThat(vm.uiState.value.addressError).isNull()
    }

    @Test
    fun `init - controller overrides channel and preamble from device info`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.success(controllerDeviceInfo))

        val vm = buildViewModel(isController = true)

        assertThat(vm.uiState.value.channelNumber).isEqualTo("9")
        assertThat(vm.uiState.value.preambleIndex).isEqualTo("10")
    }

    @Test
    fun `init - controlee keeps default channel and preamble`() = runTest {
        whenever(getLocalAddress(false)).thenReturn(Result.success(controleeDeviceInfo))

        val vm = buildViewModel(isController = false)

        // controleeDeviceInfo has null channel/preamble → keeps default values
        assertThat(vm.uiState.value.channelNumber).isEqualTo("9")
        assertThat(vm.uiState.value.preambleIndex).isEqualTo("10")
    }

    @Test
    fun `init - on failure sets addressError and clears loading`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.failure(IOException("nope")))

        val vm = buildViewModel(isController = true)

        assertThat(vm.uiState.value.isLoadingAddress).isFalse()
        assertThat(vm.uiState.value.addressError).isEqualTo("無法取得本機地址：nope")
        assertThat(vm.uiState.value.localDeviceInfo).isNull()
    }

    @Test
    fun `onPeerAddressChanged - uppercases input`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onPeerAddressChanged("a1:b2")

        assertThat(vm.uiState.value.peerAddressHex).isEqualTo("A1:B2")
    }

    @Test
    fun `onPeerAddressChanged - filters non-hex non-colon chars`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onPeerAddressChanged("A1:ZZ")

        assertThat(vm.uiState.value.peerAddressHex).isEqualTo("A1:")
    }

    @Test
    fun `onPeerAddressChanged - sets error for invalid format`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onPeerAddressChanged("A1:B2:C3")

        assertThat(vm.uiState.value.peerAddressError).isNotNull()
    }

    @Test
    fun `onPeerAddressChanged - clears error for valid format`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onPeerAddressChanged("A1:B2:C3")
        assertThat(vm.uiState.value.peerAddressError).isNotNull()

        vm.onPeerAddressChanged("A1:B2")
        assertThat(vm.uiState.value.peerAddressError).isNull()
    }

    @Test
    fun `onPeerAddressChanged - clears error for empty input`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onPeerAddressChanged("A1:B2:C3")
        vm.onPeerAddressChanged("")

        assertThat(vm.uiState.value.peerAddressError).isNull()
    }

    @Test
    fun `onChannelChanged - filters non-digit chars`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onChannelChanged("9abc")

        assertThat(vm.uiState.value.channelNumber).isEqualTo("9")
    }

    @Test
    fun `onSessionKeyChanged - uppercases input`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onSessionKeyChanged("0102030405060708")

        assertThat(vm.uiState.value.sessionKeyHex).isEqualTo("0102030405060708")
    }

    @Test
    fun `onSessionKeyChanged - sets error when not 16 chars`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onSessionKeyChanged("0102")

        assertThat(vm.uiState.value.sessionKeyError).isNotNull()
    }

    @Test
    fun `onSessionKeyChanged - clears error when exactly 16 hex chars`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onSessionKeyChanged("0102")
        vm.onSessionKeyChanged("0102030405060708")

        assertThat(vm.uiState.value.sessionKeyError).isNull()
    }

    @Test
    fun `onSessionKeyChanged - clears error for empty input`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onSessionKeyChanged("0102")
        vm.onSessionKeyChanged("")

        assertThat(vm.uiState.value.sessionKeyError).isNull()
    }

    @Test
    fun `onReverseBytesChanged - updates state`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel()

        vm.onReverseBytesChanged(true)
        assertThat(vm.uiState.value.reverseBytes).isTrue()

        vm.onReverseBytesChanged(false)
        assertThat(vm.uiState.value.reverseBytes).isFalse()
    }

    @Test
    fun `canProceed - false when localDeviceInfo is null`() = runTest {
        whenever(getLocalAddress(any())).thenReturn(Result.failure(IOException("err")))
        val vm = buildViewModel()

        assertThat(vm.uiState.value.canProceed).isFalse()
    }

    @Test
    fun `canProceed - true when all fields valid`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel(isController = true)

        vm.onPeerAddressChanged("A1:B2")
        vm.onSessionKeyChanged("0102030405060708")

        assertThat(vm.uiState.value.canProceed).isTrue()
    }

    @Test
    fun `buildOobParams - constructs correctly from state`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel(isController = true)

        vm.onPeerAddressChanged("A1:B2")
        vm.onSessionKeyChanged("0102030405060708")
        vm.onReverseBytesChanged(false)

        val params = vm.buildOobParams()

        assertThat(params.channelNumber).isEqualTo(9)
        assertThat(params.preambleIndex).isEqualTo(10)
        assertThat(params.sessionKeyHex).isEqualTo("0102030405060708")
        assertThat(params.reverseBytes).isFalse()
    }

    @Test
    fun `buildOobParams - parses peerAddressHex to byte array correctly`() = runTest {
        whenever(getLocalAddress(true)).thenReturn(Result.success(controllerDeviceInfo))
        val vm = buildViewModel(isController = true)

        vm.onPeerAddressChanged("A1:B2")
        val params = vm.buildOobParams()

        assertThat(params.peerAddress).isEqualTo(byteArrayOf(0xA1.toByte(), 0xB2.toByte()))
    }
}
