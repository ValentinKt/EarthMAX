package com.earthmax.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthmax.data.chat.ChatRepository
import com.earthmax.data.model.Message
import com.earthmax.data.model.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val messageText: String = "",
    val isConnected: Boolean = false,
    val unreadCount: Int = 0
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentEventId: String? = null
    private var currentUserId: String? = null
    private var currentUserName: String? = null
    private var currentUserAvatarUrl: String? = null
    
    fun initializeChat(
        eventId: String,
        userId: String,
        userName: String,
        userAvatarUrl: String?
    ) {
        currentEventId = eventId
        currentUserId = userId
        currentUserName = userName
        currentUserAvatarUrl = userAvatarUrl
        
        loadMessages()
        subscribeToNewMessages()
        loadUnreadCount()
    }
    
    private fun loadMessages() {
        val eventId = currentEventId ?: return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        viewModelScope.launch {
            chatRepository.getMessages(eventId)
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }
    
    private fun subscribeToNewMessages() {
        val eventId = currentEventId ?: return
        
        chatRepository.subscribeToMessages(eventId)
            .onEach { newMessage ->
                val currentMessages = _uiState.value.messages.toMutableList()
                
                // Check if message already exists (avoid duplicates)
                if (!currentMessages.any { it.id == newMessage.id }) {
                    currentMessages.add(newMessage)
                    _uiState.value = _uiState.value.copy(
                        messages = currentMessages.sortedBy { it.timestamp },
                        isConnected = true
                    )
                }
            }
            .catch { error ->
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    error = error.message
                )
            }
            .launchIn(viewModelScope)
    }
    
    private fun loadUnreadCount() {
        val eventId = currentEventId ?: return
        
        viewModelScope.launch {
            val count = chatRepository.getUnreadMessageCount(eventId)
            _uiState.value = _uiState.value.copy(unreadCount = count)
        }
    }
    
    fun sendMessage(content: String = _uiState.value.messageText.trim()) {
        if (content.isBlank()) return
        
        val eventId = currentEventId ?: return
        val userId = currentUserId ?: return
        val userName = currentUserName ?: return
        
        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                eventId = eventId,
                senderId = userId,
                senderName = userName,
                senderAvatarUrl = currentUserAvatarUrl,
                content = content,
                messageType = MessageType.TEXT
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(messageText = "")
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to send message: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun updateMessageText(text: String) {
        _uiState.value = _uiState.value.copy(messageText = text)
    }
    
    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            chatRepository.markMessageAsRead(messageId)
            loadUnreadCount()
        }
    }
    
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val result = chatRepository.deleteMessage(messageId)
            result.fold(
                onSuccess = {
                    // Message will be removed via real-time subscription
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete message: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun retryConnection() {
        currentEventId?.let { eventId ->
            subscribeToNewMessages()
            loadMessages()
        }
    }
}