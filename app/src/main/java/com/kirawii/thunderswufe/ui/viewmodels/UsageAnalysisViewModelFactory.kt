package com.kirawii.thunderswufe.ui.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class UsageAnalysisViewModelFactory(
    private val application: Application,
    private val settingsViewModel: SettingsViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UsageAnalysisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UsageAnalysisViewModel(application, settingsViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
