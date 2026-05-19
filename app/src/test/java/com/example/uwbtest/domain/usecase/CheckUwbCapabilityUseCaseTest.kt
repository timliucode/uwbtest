package com.example.uwbtest.domain.usecase

import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.repository.UwbRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CheckUwbCapabilityUseCaseTest {

    private lateinit var repository: UwbRepository
    private lateinit var useCase: CheckUwbCapabilityUseCase

    @Before
    fun setUp() {
        repository = mock()
        useCase = CheckUwbCapabilityUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository and returns result`() = runTest {
        val expected = UwbCapability(hardwarePresent = true, isAvailable = true)
        whenever(repository.checkCapability()).thenReturn(expected)

        val result = useCase()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `invoke propagates exception from repository`() = runTest {
        whenever(repository.checkCapability()).thenThrow(SecurityException("permission denied"))

        val thrown = runCatching { useCase() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(SecurityException::class.java)
        assertThat(thrown?.message).isEqualTo("permission denied")
    }
}
