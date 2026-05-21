package com.example.uwbtest.domain.usecase

import com.example.uwbtest.domain.model.UwbDeviceInfo
import com.example.uwbtest.domain.model.UwbRole
import com.example.uwbtest.domain.repository.UwbRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

class GetLocalAddressUseCaseTest {

    private lateinit var repository: UwbRepository
    private lateinit var useCase: GetLocalAddressUseCase

    @BeforeEach
    fun setUp() {
        repository = mock()
        useCase = GetLocalAddressUseCase(repository)
    }

    @Test
    fun `invoke with isController=true passes true to repository`() = runTest {
        val deviceInfo = UwbDeviceInfo(byteArrayOf(0x01, 0x02), UwbRole.Controller, 9, 10)
        whenever(repository.getLocalDeviceInfo(true)).thenReturn(Result.success(deviceInfo))

        useCase(isController = true)

        verify(repository).getLocalDeviceInfo(true)
    }

    @Test
    fun `invoke with isController=false passes false to repository`() = runTest {
        val deviceInfo = UwbDeviceInfo(byteArrayOf(0x03, 0x04), UwbRole.Controlee)
        whenever(repository.getLocalDeviceInfo(false)).thenReturn(Result.success(deviceInfo))

        useCase(isController = false)

        verify(repository).getLocalDeviceInfo(false)
    }

    @Test
    fun `invoke returns success from repository`() = runTest {
        val deviceInfo = UwbDeviceInfo(byteArrayOf(0x01, 0x02), UwbRole.Controller, 9, 10)
        whenever(repository.getLocalDeviceInfo(true)).thenReturn(Result.success(deviceInfo))

        val result = useCase(isController = true)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(deviceInfo)
    }

    @Test
    fun `invoke returns failure from repository`() = runTest {
        val error = IOException("UWB not ready")
        whenever(repository.getLocalDeviceInfo(true)).thenReturn(Result.failure(error))

        val result = useCase(isController = true)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IOException::class.java)
        assertThat(result.exceptionOrNull()?.message).isEqualTo("UWB not ready")
    }
}
