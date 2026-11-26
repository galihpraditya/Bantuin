package id.bantuin.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.ChatMessage
import id.bantuin.app.domain.model.ChatThreadSummary
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val activeRole: String = "customer",
    val threads: List<ChatThreadSummary> = emptyList(),
    val filteredThreads: List<ChatThreadSummary> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        observeThreads()
    }

    fun observeThreads() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeJob = viewModelScope.launch {
            var streamFailed = false
            authRepository.activeRole()
                .flatMapLatest { role ->
                    _uiState.update { it.copy(activeRole = role) }
                    chatRepository.observeChatThreads(role)
                        .catch { e ->
                            streamFailed = true
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = e.message ?: "Gagal memuat daftar percakapan."
                                )
                            }
                            emit(emptyList())
                        }
                }
                .collect { threads ->
                    _uiState.update { state ->
                        state.copy(
                            threads = threads,
                            filteredThreads = filterThreads(threads, state.searchQuery),
                            isLoading = false,
                            // Jangan hapus pesan error yang baru saja ditetapkan .catch()
                            errorMessage = if (streamFailed) state.errorMessage else null
                        )
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredThreads = filterThreads(state.threads, query)
            )
        }
    }

    private fun filterThreads(threads: List<ChatThreadSummary>, query: String): List<ChatThreadSummary> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return threads
        return threads.filter {
            it.counterpartyName.contains(trimmed, ignoreCase = true) ||
                it.serviceName.contains(trimmed, ignoreCase = true) ||
                it.lastMessageText.contains(trimmed, ignoreCase = true)
        }
    }
}

data class ChatRoomUiState(
    val bookingId: String = "",
    val booking: BookingItem? = null,
    val messages: List<ChatMessage> = emptyList(),
    val currentUserId: String = "",
    val activeRole: String = "customer",
    val messageInput: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class ChatRoomViewModel(
    private val chatRepository: ChatRepository,
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatRoomUiState())
    val uiState: StateFlow<ChatRoomUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var errorObserveJob: Job? = null
    private var authJob: Job? = null

    init {
        if (authRepository != null) {
            authJob = viewModelScope.launch {
                combine(authRepository.currentUser(), authRepository.activeRole()) { user, role ->
                    (user?.id ?: "") to role
                }.collect { (userId, role) ->
                    _uiState.update { it.copy(currentUserId = userId, activeRole = role) }
                }
            }
        }
    }

    fun initialize(bookingId: String) {
        observeJob?.cancel()
        errorObserveJob?.cancel()
        _uiState.update {
            it.copy(
                bookingId = bookingId,
                isLoading = true,
                errorMessage = null
            )
        }
        errorObserveJob = viewModelScope.launch {
            chatRepository.observeChatError(bookingId).collect { error ->
                if (!error.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error) }
                }
            }
        }
        observeJob = viewModelScope.launch {
            combine(
                bookingRepository.observeBooking(bookingId),
                chatRepository.observeMessages(bookingId)
            ) { booking, messages ->
                booking to messages.sortedBy { it.createdAt }
            }
                .catch { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Gagal memuat percakapan."
                        )
                    }
                }
                .collect { (booking, sortedMessages) ->
                    _uiState.update { state ->
                        val fallbackUserId = if (state.currentUserId.isNotBlank()) {
                            state.currentUserId
                        } else if (state.activeRole == "provider") {
                            booking?.providerId.orEmpty()
                        } else {
                            booking?.customerId.orEmpty()
                        }
                        state.copy(
                            booking = booking,
                            messages = sortedMessages,
                            currentUserId = fallbackUserId,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun retryMessages() {
        val bookingId = _uiState.value.bookingId
        if (bookingId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = chatRepository.retryMessages(bookingId)) {
                is AppResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = res.message)
                }
                else -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onMessageInputChange(text: String) {
        _uiState.update {
            it.copy(
                messageInput = text.take(BantuinConfig.MAX_CHAT_MESSAGE_LENGTH),
                errorMessage = null
            )
        }
    }

    fun sendQuickReply(text: String) {
        sendMessageInternal(text)
    }

    fun sendMessage() {
        sendMessageInternal(_uiState.value.messageInput, clearInput = true)
    }

    private fun sendMessageInternal(rawText: String, clearInput: Boolean = false) {
        val bookingId = _uiState.value.bookingId
        val trimmed = rawText.trim()
        if (bookingId.isBlank()) return
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Pesan tidak boleh kosong.") }
            return
        }
        if (_uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }
            when (val res = chatRepository.sendMessage(bookingId, trimmed)) {
                is AppResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            messageInput = if (clearInput) "" else it.messageInput,
                            errorMessage = null
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            errorMessage = res.message
                        )
                    }
                }
                is AppResult.Loading -> {}
            }
        }
    }
}
