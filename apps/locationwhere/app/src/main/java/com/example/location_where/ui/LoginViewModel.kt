package com.example.location_where.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.location_where.api.ApiService
import com.example.location_where.data.LoginRequest
import com.example.location_where.utils.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginUiState>()
    val loginState: LiveData<LoginUiState> = _loginState

    private var failCount = 0
    private var lockUntil: Long = 0

    fun login(employeeCode: String, password: String, androidId: String) {
        if (System.currentTimeMillis() < lockUntil) {
            _loginState.value = LoginUiState.Error("অধিকবার চেষ্টার কারণে ৫ মিনিট লক।")
            return
        }

        _loginState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                val response = apiService.login(LoginRequest(employeeCode, password, androidId))
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        tokenManager.saveTokens(data.accessToken, data.refreshToken)
                        _loginState.value = LoginUiState.Success
                        failCount = 0
                    }
                } else {
                    failCount++
                    if (failCount >= 5) {
                        lockUntil = System.currentTimeMillis() + 5 * 60 * 1000
                        _loginState.value = LoginUiState.Error("৫ বার ব্যর্থ। ৫ মিনিট লক করা হয়েছে।")
                    } else {
                        _loginState.value = LoginUiState.Error(response.body()?.error ?: "লগইন ব্যর্থ হয়েছে।")
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error("ইন্টারনেট সংযোগ নেই বা সার্ভার সমস্যা।")
            }
        }
    }
}

sealed class LoginUiState {
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
