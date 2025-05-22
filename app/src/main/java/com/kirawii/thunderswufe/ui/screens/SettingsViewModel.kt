package com.kirawii.thunderswufe.ui.viewmodels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.preferences.RoomInfo
import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
data class SettingsUiState(
    val threshold: String = "10.0",
    val roomNo: String = "",
    val buildingNo: String = "",
    val areaNo: String = "",
    val notificationEnabled: Boolean = true
)

class SettingsViewModel(
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        userPreferencesManager.lowBalanceThreshold
            .combine(userPreferencesManager.roomInfo) { threshold, roomInfo -> threshold to roomInfo }
            .combine(userPreferencesManager.notificationEnabled) { pair, notifEnabled -> Triple(pair.first, pair.second, notifEnabled) }
            .map { (threshold, roomInfo, notifEnabled) ->
                SettingsUiState(
                    threshold = threshold.toString(),
                    roomNo = roomInfo.roomNo,
                    buildingNo = roomInfo.buildingNo,
                    areaNo = roomInfo.areaNo,
                    notificationEnabled = notifEnabled
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsUiState() // 初始状态
            )

    fun updateThreshold(newThreshold: String) {
        viewModelScope.launch {
            userPreferencesManager.updateLowBalanceThreshold(newThreshold.toDoubleOrNull() ?: 10.0)
        }
    }

    fun updateRoomNo(newRoomNo: String) {
        viewModelScope.launch {
            val currentInfo = userPreferencesManager.roomInfo.first() // 获取当前值
            userPreferencesManager.updateRoomInfo(currentInfo.copy(roomNo = newRoomNo))
        }
    }
    // ... 类似地为 buildingNo, areaNo 创建更新函数 ...

    fun updateBuildingNo(newBuildingNo: String) {
        viewModelScope.launch {
            val currentInfo = userPreferencesManager.roomInfo.first()
            userPreferencesManager.updateRoomInfo(currentInfo.copy(buildingNo = newBuildingNo))
        }
    }

    fun updateAreaNo(newAreaNo: String) {
        viewModelScope.launch {
            val currentInfo = userPreferencesManager.roomInfo.first()
            userPreferencesManager.updateRoomInfo(currentInfo.copy(areaNo = newAreaNo))
        }
    }


    fun updateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesManager.updateNotificationEnabled(enabled)
        }
    }

    fun saveAllSettings(
        threshold: String,
        roomNo: String,
        buildingNo: String,
        areaNo: String
        // notificationEnabled 通过其 Switch 的 onCheckedChange 直接更新了
    ) {
        viewModelScope.launch {
            userPreferencesManager.updateLowBalanceThreshold(threshold.toDoubleOrNull() ?: 10.0)
            userPreferencesManager.updateRoomInfo(
                RoomInfo(
                    roomNo = roomNo,
                    buildingNo = buildingNo,
                    areaNo = areaNo
                )
            )
            // 如果 notificationEnabled 也想在点击保存时才更新，则在此处更新
        }
    }
}

// ViewModel Factory (如果 ViewModel 构造函数有参数且不使用 Hilt)
class SettingsViewModelFactory(private val application: ThunderApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application.userPreferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}