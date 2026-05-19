package com.example.uwbtest.domain.usecase

import app.cash.turbine.test
import com.example.uwbtest.domain.model.OobParams
import com.example.uwbtest.domain.model.RangingState
import com.example.uwbtest.domain.repository.UwbRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StartRangingUseCaseTest {

    private lateinit var repository: UwbRepository
    private lateinit var useCase: StartRangingUseCase

    private val testParams = OobParams(
        peerAddress = byteArrayOf(0x01, 0x02),
        channelNumber = 9,
        preambleIndex = 10,
        sessionKeyHex = "0102030405060708",
    )

    @Before
    fun setUp() {
        repository = mock()
        useCase = StartRangingUseCase(repository)
    }

    @Test
    fun `invoke passes OobParams to repository`() = runTest {
        whenever(repository.startRanging(testParams)).thenReturn(flowOf())

        useCase(testParams)

        verify(repository).startRanging(testParams)
    }

    @Test
    fun `invoke returns same flow items as repository`() = runTest {
        val flow = flowOf(RangingState.Idle, RangingState.Initializing)
        whenever(repository.startRanging(testParams)).thenReturn(flow)

        useCase(testParams).test {
            assertThat(awaitItem()).isEqualTo(RangingState.Idle)
            assertThat(awaitItem()).isEqualTo(RangingState.Initializing)
            awaitComplete()
        }
    }

    @Test
    fun `invoke returns Active states from repository`() = runTest {
        val active = RangingState.Active(1.5f, 30.0f, 5.0f, 1000L)
        whenever(repository.startRanging(testParams)).thenReturn(flowOf(active))

        useCase(testParams).test {
            assertThat(awaitItem()).isEqualTo(active)
            awaitComplete()
        }
    }
}
