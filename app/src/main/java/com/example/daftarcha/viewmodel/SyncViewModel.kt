package com.example.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daftarcha.data.sync.FirestoreSyncManager
import com.example.daftarcha.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: FirestoreSyncManager
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = syncManager.syncStatus

    private val _lastSyncTimeFormatted = MutableStateFlow(syncManager.getFormattedLastSync())
    val lastSyncTimeFormatted: StateFlow<String> = _lastSyncTimeFormatted.asStateFlow()

    fun triggerSync() {
        viewModelScope.launch {
            syncManager.syncAll()
            _lastSyncTimeFormatted.value = syncManager.getFormattedLastSync()
        }
    }

    fun refreshLastSyncTime() {
        _lastSyncTimeFormatted.value = syncManager.getFormattedLastSync()
    }
}
