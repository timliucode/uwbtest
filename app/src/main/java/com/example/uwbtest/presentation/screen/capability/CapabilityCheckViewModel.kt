package com.example.uwbtest.presentation.screen.capability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.uwbtest.domain.model.UwbCapability
import com.example.uwbtest.domain.usecase.CheckUwbCapabilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * CapabilityCheckScreen 的 ViewModel。
 * 負責觸發 UWB 能力檢查並將結果暴露給 UI。
 */
@HiltViewModel
class CapabilityCheckViewModel @Inject constructor(
    private val checkCapability: CheckUwbCapabilityUseCase,
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Success(val capability: UwbCapability) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** 由 PermissionHandler 確認權限授予後呼叫 */
    fun check() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val capability = checkCapability()
                _uiState.value = UiState.Success(capability)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** 使用者拒絕權限後呼叫 */
    fun onPermissionDenied() {
        _uiState.value = UiState.Error("UWB_RANGING permission denied. Please grant it in Settings.")
    }
}
