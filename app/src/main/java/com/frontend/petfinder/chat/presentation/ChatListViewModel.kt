package com.frontend.petfinder.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.chat.data.ChatRepository
import com.frontend.petfinder.chat.data.ChatSummaryDto
import com.frontend.petfinder.core.network.SocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Lista de conversaciones privadas (dueño ↔ rescatista).
 * Reemplaza al ChatViewModel basado en comentarios de avistamientos.
 */
class ChatListViewModel : ViewModel() {

    private val _chats = MutableStateFlow<List<ChatSummaryDto>>(emptyList())
    val chats: StateFlow<List<ChatSummaryDto>> = _chats.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** Suma de no leídos — usado por el badge del bottom nav. */
    private val _unreadTotal = MutableStateFlow(0)
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    init {
        load()
        observeLiveEvents()
    }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            ChatRepository.getChats().onSuccess { list ->
                _chats.value = list.sortedByDescending { it.ultimaActividad ?: "" }
                _unreadTotal.value = list.sumOf { it.noLeidos }
            }
            _loading.value = false
        }
    }

    /**
     * Cualquier evento de chat (mensaje nuevo, conteo, aceptación/rechazo, invitación)
     * cambia la lista o los badges → recargamos para reflejar último mensaje + no leídos.
     */
    private fun observeLiveEvents() {
        viewModelScope.launch {
            merge(
                SocketManager.chatMessageFlow,
                SocketManager.chatUnreadCountFlow,
                SocketManager.chatAcceptedFlow,
                SocketManager.chatDeclinedFlow,
                SocketManager.chatInviteFlow
            ).collect { load() }
        }
    }
}
