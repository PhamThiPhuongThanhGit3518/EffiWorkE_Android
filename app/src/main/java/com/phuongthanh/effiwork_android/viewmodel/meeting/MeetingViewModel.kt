package com.phuongthanh.effiwork_android.viewmodel.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.CreateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.response.MeetingResponse
import com.phuongthanh.effiwork_android.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MeetingUiState {
    object Idle : MeetingUiState()
    object Loading : MeetingUiState()
    data class Success(val meetings: List<Meeting>) : MeetingUiState()
    data class Error(val message: String) : MeetingUiState()
}

data class Meeting(
    val id: String,
    val title: String,
    val description: String,
    val format: MeetingFormat,
    val status: MeetingStatus,
    val organizer: String,
    val time: String,
    val date: String,
    val participants: List<String>
)

enum class MeetingFormat(val displayName: String) {
    ONLINE("Online"),
    OFFLINE("Offline");

    companion object {
        fun fromString(value: String): MeetingFormat {
            return when (value.lowercase()) {
                "online" -> ONLINE
                "offline" -> OFFLINE
                else -> ONLINE
            }
        }
    }
}

enum class MeetingStatus(val displayName: String) {
    UPCOMING("Sắp diễn ra"),
    ONGOING("Đang diễn ra"),
    ENDED("Đã kết thúc"),
    CANCELLED("Đã hủy");

    companion object {
        fun fromString(value: String): MeetingStatus {
            return when (value.lowercase()) {
                "upcoming", "sắp diễn ra" -> UPCOMING
                "ongoing", "đang diễn ra" -> ONGOING
                "ended", "đã kết thúc" -> ENDED
                "cancelled", "đã hủy" -> CANCELLED
                else -> UPCOMING
            }
        }
    }
}

enum class MeetingFilterTab {
    ALL,
    JOINED
}

sealed class MeetingEffect {
    data class ShowToast(val message: String) : MeetingEffect()
}

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeetingUiState>(MeetingUiState.Idle)
    val uiState: StateFlow<MeetingUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(MeetingFilterTab.ALL)
    val selectedTab: StateFlow<MeetingFilterTab> = _selectedTab.asStateFlow()

    private val _selectedFormat = MutableStateFlow<String?>(null)
    val selectedFormat: StateFlow<String?> = _selectedFormat.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _projectId = MutableStateFlow("")
    val projectId: StateFlow<String> = _projectId.asStateFlow()

    private val _effect = MutableSharedFlow<MeetingEffect>()
    val effect: SharedFlow<MeetingEffect> = _effect.asSharedFlow()

    fun setProjectId(projectId: String) {
        _projectId.value = projectId
    }

    fun selectTab(tab: MeetingFilterTab) {
        _selectedTab.value = tab
    }

    fun selectFormat(format: String?) {
        _selectedFormat.value = format
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) {
                _uiState.value = MeetingUiState.Error("Project ID is required")
                return@launch
            }

            when (val result = meetingRepository.getMeetings(projectIdValue, _selectedFormat.value)) {
                is ApiResult.Success -> {
                    val meetings = result.data.map { it.toMeeting() }
                    _uiState.value = MeetingUiState.Success(meetings)
                }
                is ApiResult.Error -> {
                    _uiState.value = MeetingUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = MeetingUiState.Loading
                }
            }
        }
    }

    fun createMeeting(
        title: String,
        content: String,
        organizerId: String,
        format: String,
        scheduledTime: String,
        notes: String?,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value

            val request = CreateMeetingRequest(
                projectId = projectIdValue,
                title = title,
                content = content,
                organizerId = organizerId,
                format = format,
                scheduledTime = scheduledTime,
                notes = notes,
                participantIds = participantIds
            )

            when (val result = meetingRepository.createMeeting(projectIdValue, request)) {
                is ApiResult.Success -> {
                    _effect.emit(MeetingEffect.ShowToast("Tạo cuộc họp thành công"))
                    loadMeetings()
                }
                is ApiResult.Error -> {
                    _uiState.value = MeetingUiState.Error(result.message)
                    _effect.emit(MeetingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = MeetingUiState.Loading
                }
            }
        }
    }

    private fun MeetingResponse.toMeeting(): Meeting {
        val dateTimeParts = (scheduledTime ?: "").split("T")
        val date = dateTimeParts.getOrNull(0)?.takeLast(5) ?: ""
        val time = dateTimeParts.getOrNull(1)?.takeLast(5) ?: ""

        return Meeting(
            id = id,
            title = title ?: "",
            description = content ?: "",
            format = MeetingFormat.fromString(format ?: ""),
            status = MeetingStatus.fromString(status ?: ""),
            organizer = organizerName ?: "",
            time = time,
            date = date,
            participants = participants?.map { it.userName ?: "" } ?: emptyList()
        )
    }
}