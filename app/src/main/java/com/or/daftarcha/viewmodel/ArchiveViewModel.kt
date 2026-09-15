package com.or.daftarcha.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.or.daftarcha.data.dao.ProjectDao
import com.or.daftarcha.data.model.Project
import com.or.daftarcha.data.sync.FirestoreSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Определяет, какой диалог открыт на экране Архива
enum class ArchiveDialog {
    NONE,
    DELETE,
    RESTORE
}

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val projectDao: ProjectDao,
    private val syncManager: FirestoreSyncManager
) : ViewModel() {

    // Поток архивированных проектов
    val archivedProjects: StateFlow<List<Project>> = projectDao.getArchivedProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Управление диалогами ---
    private val _dialogState = MutableStateFlow(ArchiveDialog.NONE)
    val dialogState: StateFlow<ArchiveDialog> = _dialogState.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    fun openDialog(dialog: ArchiveDialog, project: Project) {
        _selectedProject.value = project
        _dialogState.value = dialog
    }

    fun dismissDialog() {
        _selectedProject.value = null
        _dialogState.value = ArchiveDialog.NONE
    }

    // --- Функции ---
    fun restoreSelectedProject() {
        _selectedProject.value?.let { project ->
            viewModelScope.launch {
                projectDao.restoreProject(project.id)
                dismissDialog()
            }
        }
    }

    fun deleteSelectedProject() {
        _selectedProject.value?.let { project ->
            viewModelScope.launch {
                projectDao.deleteProjectById(project.id)
                syncManager.deleteProjectFromCloud(project.id)
                dismissDialog()
            }
        }
    }
}