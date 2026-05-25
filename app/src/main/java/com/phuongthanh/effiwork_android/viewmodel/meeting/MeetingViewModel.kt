package com.phuongthanh.effiwork_android.viewmodel.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phuongthanh.effiwork_android.api.ApiResult
import com.phuongthanh.effiwork_android.data.model.request.CreateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.request.UpdateMeetingRequest
import com.phuongthanh.effiwork_android.data.model.response.MeetingResponse
import com.phuongthanh.effiwork_android.data.model.response.MemberResponse
import com.phuongthanh.effiwork_android.data.repository.DocumentRepository
import com.phuongthanh.effiwork_android.data.repository.MeetingRepository
import com.phuongthanh.effiwork_android.data.repository.TaskRepository
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
    val organizerId: String,
    val time: String,
    val date: String,
    val participants: List<String>,
    val notes: String?,
    val meetingTime: String?
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
    object NavigateBack : MeetingEffect()
    data class MeetingCreated(val meetingId: String) : MeetingEffect()
}

data class ProjectMember(
    val userId: String,
    val fullName: String
)

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val taskRepository: TaskRepository,
    private val documentRepository: DocumentRepository
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

    private val _projectMembers = MutableStateFlow<List<ProjectMember>>(emptyList())
    val projectMembers: StateFlow<List<ProjectMember>> = _projectMembers.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _selectedMeeting = MutableStateFlow<Meeting?>(null)
    val selectedMeeting: StateFlow<Meeting?> = _selectedMeeting.asStateFlow()

    private val _editingMeeting = MutableStateFlow<Meeting?>(null)
    val editingMeeting: StateFlow<Meeting?> = _editingMeeting.asStateFlow()

    fun setProjectId(projectId: String) {
        _projectId.value = projectId
    }

    fun setCurrentUserId(userId: String) {
        _currentUserId.value = userId
    }

    fun selectTab(tab: MeetingFilterTab) {
        _selectedTab.value = tab
    }

    fun selectFormat(format: String?) {
        _selectedFormat.value = format?.uppercase()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadProjectMembers() {
        viewModelScope.launch {
            val projectIdValue = _projectId.value
            if (projectIdValue.isBlank()) return@launch

            when (val result = taskRepository.getMembers(projectIdValue)) {
                is ApiResult.Success -> {
                    val members = result.data.mapNotNull { member ->
                        member.user?.let { ProjectMember(it.id, it.fullName ?: "") }
                    }
                    _projectMembers.value = members
                }
                is ApiResult.Error -> {
                    // Handle silently or log
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    fun loadMeetings() {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value
            android.util.Log.d("MeetingDebug", "loadMeetings: projectId=$projectIdValue, formatFilter=${_selectedFormat.value}")
            if (projectIdValue.isBlank()) {
                android.util.Log.e("MeetingDebug", "Project ID is blank!")
                _uiState.value = MeetingUiState.Error("Project ID is required")
                return@launch
            }

            when (val result = meetingRepository.getMeetings(projectIdValue, _selectedFormat.value)) {
                is ApiResult.Success -> {
                    android.util.Log.d("MeetingDebug", "loadMeetings SUCCESS: ${result.data.size} meetings")
                    result.data.forEachIndexed { index, m ->
                        android.util.Log.d("MeetingDebug", "  meeting[$index]: id=${m.id}, title=${m.title}, type=${m.type}, status=${m.status}")
                        android.util.Log.d("MeetingDebug", "    organizerId=${m.organizerId}, hostUser=${m.hostUser}, hostUser.fullName=${m.hostUser?.fullName}")
                        android.util.Log.d("MeetingDebug", "    scheduledTime=${m.scheduledTime}")
                        android.util.Log.d("MeetingDebug", "    participants=${m.participants?.size}")
                    }
                    val meetings = result.data.map { it.toMeeting() }
                    _uiState.value = MeetingUiState.Success(meetings)
                }
                is ApiResult.Error -> {
                    android.util.Log.e("MeetingDebug", "loadMeetings ERROR: ${result.message}")
                    _uiState.value = MeetingUiState.Error(result.message)
                }
                is ApiResult.Loading -> {
                    _uiState.value = MeetingUiState.Loading
                }
            }
        }
    }

    fun loadMeetingDetail(meetingId: String) {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value
            android.util.Log.d("MeetingDebug", "loadMeetingDetail: projectId=$projectIdValue, meetingId=$meetingId")

            when (val result = meetingRepository.getMeetingDetail(projectIdValue, meetingId)) {
                is ApiResult.Success -> {
                    val data = result.data
                    android.util.Log.d("MeetingDebug", "loadMeetingDetail SUCCESS")
                    android.util.Log.d("MeetingDebug", "  RAW response:")
                    android.util.Log.d("MeetingDebug", "    id: ${data.id}")
                    android.util.Log.d("MeetingDebug", "    projectId: ${data.projectId}")
                    android.util.Log.d("MeetingDebug", "    title: ${data.title}")
                    android.util.Log.d("MeetingDebug", "    content: ${data.content}")
                    android.util.Log.d("MeetingDebug", "    organizerId (hostUserId): ${data.organizerId}")
                    android.util.Log.d("MeetingDebug", "    hostUser: ${data.hostUser}")
                    android.util.Log.d("MeetingDebug", "    hostUser?.fullName: ${data.hostUser?.fullName}")
                    android.util.Log.d("MeetingDebug", "    type: ${data.type}")
                    android.util.Log.d("MeetingDebug", "    scheduledTime (meetingTime): ${data.scheduledTime}")
                    android.util.Log.d("MeetingDebug", "    status: ${data.status}")
                    android.util.Log.d("MeetingDebug", "    notes (note): ${data.notes}")
                    android.util.Log.d("MeetingDebug", "    participants: ${data.participants?.size}")
                    data.participants?.forEachIndexed { index, p ->
                        android.util.Log.d("MeetingDebug", "      [$index] userId=${p.userId}, user=${p.user}, user.fullName=${p.user?.fullName}")
                    }
                    android.util.Log.d("MeetingDebug", "    attachments: ${data.attachments?.size}")

                    val meeting = result.data.toMeeting()
                    _selectedMeeting.value = meeting
                    _uiState.value = MeetingUiState.Success(listOf(meeting))
                }
                is ApiResult.Error -> {
                    android.util.Log.e("MeetingDebug", "loadMeetingDetail ERROR: ${result.message}")
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
        hostUserId: String,
        type: String,
        meetingTime: String,
        notes: String?,
        participantIds: List<String>,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value

            android.util.Log.d("MeetingDebug", "createMeeting called: projectId=$projectIdValue, title=$title, hostUserId=$hostUserId, type=$type, meetingTime=$meetingTime, participantIds=$participantIds")

            if (projectIdValue.isBlank()) {
                android.util.Log.e("MeetingDebug", "Project ID is blank!")
                _effect.emit(MeetingEffect.ShowToast("Lỗi: Project ID không hợp lệ"))
                return@launch
            }

            if (hostUserId.isBlank()) {
                android.util.Log.e("MeetingDebug", "Host user ID is blank!")
                _effect.emit(MeetingEffect.ShowToast("Lỗi: Chưa chọn người phụ trách"))
                return@launch
            }

            if (meetingTime.isBlank()) {
                android.util.Log.e("MeetingDebug", "Meeting time is blank!")
                _effect.emit(MeetingEffect.ShowToast("Lỗi: Chưa chọn thời gian họp"))
                return@launch
            }

            val request = CreateMeetingRequest(
                title = title,
                type = type,
                meetingTime = meetingTime,
                hostUserId = hostUserId,
                content = if (content.isNotBlank()) content else null,
                note = if (!notes.isNullOrBlank()) notes else null,
                participantIds = participantIds,
                attachmentDocumentIds = null
            )
            android.util.Log.d("MeetingDebug", "createMeeting request: projectId=$projectIdValue, title=${request.title}, type=${request.type}, meetingTime=${request.meetingTime}, hostUserId=${request.hostUserId}, content=${request.content}, note=${request.note}, participantIds=${request.participantIds}")

            android.util.Log.d("MeetingDebug", "Sending createMeeting request...")
            when (val result = meetingRepository.createMeeting(projectIdValue, request)) {
                is ApiResult.Success -> {
                    val meetingId = result.data.id
                    android.util.Log.d("MeetingDebug", "createMeeting SUCCESS: meetingId=$meetingId")
                    android.util.Log.d("MeetingDebug", "  response data: $result.data")
                    _effect.emit(MeetingEffect.ShowToast("Tạo cuộc họp thành công"))
                    loadMeetings()
                    onSuccess(meetingId)
                }
                is ApiResult.Error -> {
                    android.util.Log.e("MeetingDebug", "createMeeting ERROR: ${result.message}")
                    _uiState.value = MeetingUiState.Error(result.message)
                    _effect.emit(MeetingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = MeetingUiState.Loading
                }
            }
        }
    }

    private var isUploading = false

    fun updateMeeting(
        meetingId: String,
        title: String,
        content: String,
        hostUserId: String,
        type: String,
        meetingTime: String,
        notes: String?,
        participantIds: List<String>
    ) {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value

            val request = UpdateMeetingRequest(
                title = title,
                type = type,
                meetingTime = meetingTime,
                hostUserId = hostUserId,
                content = if (content.isNotBlank()) content else null,
                note = if (!notes.isNullOrBlank()) notes else null,
                participantIds = participantIds,
                attachmentDocumentIds = null
            )

            when (val result = meetingRepository.updateMeeting(projectIdValue, meetingId, request)) {
                is ApiResult.Success -> {
                    _effect.emit(MeetingEffect.ShowToast("Cập nhật cuộc họp thành công"))
                    _effect.emit(MeetingEffect.NavigateBack)
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

    fun deleteMeeting(meetingId: String) {
        viewModelScope.launch {
            _uiState.value = MeetingUiState.Loading
            val projectIdValue = _projectId.value
            android.util.Log.d("MeetingDebug", "deleteMeeting: projectId=$projectIdValue, meetingId=$meetingId")

            when (val result = meetingRepository.deleteMeeting(projectIdValue, meetingId)) {
                is ApiResult.Success -> {
                    android.util.Log.d("MeetingDebug", "deleteMeeting SUCCESS")
                    _effect.emit(MeetingEffect.ShowToast("Xóa cuộc họp thành công"))
                    loadMeetings()
                }
                is ApiResult.Error -> {
                    android.util.Log.e("MeetingDebug", "deleteMeeting ERROR: ${result.message}")
                    _uiState.value = MeetingUiState.Error(result.message)
                    _effect.emit(MeetingEffect.ShowToast(result.message))
                }
                is ApiResult.Loading -> {
                    _uiState.value = MeetingUiState.Loading
                }
            }
        }
    }

    suspend fun uploadDocument(fileName: String, fileBytes: ByteArray): ApiResult<com.phuongthanh.effiwork_android.data.model.response.DocumentResponse> {
        val projectIdValue = _projectId.value
        return documentRepository.uploadDocument(projectIdValue, fileName, fileBytes)
    }

    suspend fun attachMeetingDocument(meetingId: String, documentId: String): ApiResult<Unit> {
        val projectIdValue = _projectId.value
        return meetingRepository.attachMeetingDocument(projectIdValue, meetingId, documentId)
    }

    fun isHost(meeting: Meeting): Boolean {
        return meeting.organizerId == _currentUserId.value
    }

    fun loadMeetingForEdit(projectId: String, meetingId: String) {
        viewModelScope.launch {
            when (val result = meetingRepository.getMeetingDetail(projectId, meetingId)) {
                is ApiResult.Success -> {
                    _editingMeeting.value = result.data.toMeeting()
                }
                else -> {}
            }
        }
    }

    fun resetEditingMeeting() {
        _editingMeeting.value = null
    }

    private fun MeetingResponse.toMeeting(): Meeting {
        val dateTimeParts = (scheduledTime ?: "").split("T")
        val date = dateTimeParts.getOrNull(0)?.takeLast(5) ?: ""
        val time = dateTimeParts.getOrNull(1)?.takeLast(5) ?: ""

        // Compute status from scheduledTime since server doesn't return status
        val computedStatus = try {
            val isoTime = scheduledTime ?: ""
            val meetingMillis = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                .parse(isoTime.replace("Z", ""))?.time ?: 0
            val now = System.currentTimeMillis()
            when {
                meetingMillis < now -> MeetingStatus.ENDED
                else -> MeetingStatus.UPCOMING
            }
        } catch (e: Exception) {
            MeetingStatus.UPCOMING
        }

        android.util.Log.d("MeetingDebug", "toMeeting: id=$id, title=$title")
        android.util.Log.d("MeetingDebug", "  hostUser=${hostUser}, hostUser.fullName=${hostUser?.fullName}")
        android.util.Log.d("MeetingDebug", "  type=$type, scheduledTime=$scheduledTime")
        android.util.Log.d("MeetingDebug", "  participants: ${participants?.size}")
        participants?.forEachIndexed { index, p ->
            android.util.Log.d("MeetingDebug", "    [$index] userId=${p.userId}, user=${p.user}, user.fullName=${p.user?.fullName}")
        }

        return Meeting(
            id = id,
            title = title ?: "",
            description = content ?: "",
            format = MeetingFormat.fromString(type ?: ""),
            status = computedStatus,
            organizer = hostUser?.fullName ?: "",
            organizerId = organizerId ?: "",
            time = time,
            date = date,
            participants = participants?.map { it.user?.fullName ?: "" } ?: emptyList(),
            notes = notes,
            meetingTime = scheduledTime
        )
    }
}