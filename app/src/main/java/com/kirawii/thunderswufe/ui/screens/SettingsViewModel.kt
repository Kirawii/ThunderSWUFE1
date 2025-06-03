package com.kirawii.thunderswufe.ui.viewmodels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirawii.thunderswufe.ThunderApplication
import com.kirawii.thunderswufe.data.preferences.RoomInfo
import com.kirawii.thunderswufe.data.preferences.UserPreferencesManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private val _roomNoFlow = MutableStateFlow(userPreferencesManager.getRoomInfo().roomNo)
    val roomNoFlow: StateFlow<String> = _roomNoFlow.asStateFlow()

    private val _uiState = mutableStateOf(SettingsUiState())
    val uiState: State<SettingsUiState> get() = _uiState

    init {
        refreshUiState()
    }

    fun refreshUiState() {
        val threshold = userPreferencesManager.getLowBalanceThreshold()
        val roomInfo = userPreferencesManager.getRoomInfo()
        val notificationEnabled = userPreferencesManager.isNotificationEnabled()
        _uiState.value = SettingsUiState(
            threshold = threshold.toString(),
            roomNo = roomInfo.roomNo,
            buildingNo = roomInfo.buildingNo,
            areaNo = roomInfo.areaNo,
            notificationEnabled = notificationEnabled
        )
    }

    fun updateThreshold(newThreshold: String) {
        userPreferencesManager.setLowBalanceThreshold(newThreshold.toDoubleOrNull() ?: 10.0)
        refreshUiState()
    }

    fun updateRoomNo(newRoomNo: String) {
        val currentInfo = userPreferencesManager.getRoomInfo()
        currentInfo.roomNo = newRoomNo
        userPreferencesManager.setRoomInfo(currentInfo)
        _roomNoFlow.value = newRoomNo
        refreshUiState()
    }

    fun updateBuildingNo(newBuildingNo: String) {
        val currentInfo = userPreferencesManager.getRoomInfo()
        currentInfo.buildingNo = newBuildingNo
        userPreferencesManager.setRoomInfo(currentInfo)
        refreshUiState()
    }

    fun updateAreaNo(newAreaNo: String) {
        val currentInfo = userPreferencesManager.getRoomInfo()
        currentInfo.areaNo = newAreaNo
        userPreferencesManager.setRoomInfo(currentInfo)
        refreshUiState()
    }


    fun updateNotificationEnabled(enabled: Boolean) {
        userPreferencesManager.setNotificationEnabled(enabled)
        refreshUiState()
    }

    fun saveAllSettings(
        threshold: String,
        roomNo: String,
        buildingNo: String,
        areaNo: String
    ) {
        userPreferencesManager.setLowBalanceThreshold(threshold.toDoubleOrNull() ?: 10.0)
        userPreferencesManager.setRoomInfo(
            RoomInfo(roomNo, buildingNo, areaNo)
        )
        refreshUiState()
    }
}

class SettingsViewModelFactory(private val application: ThunderApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(application.userPreferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}