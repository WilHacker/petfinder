package com.frontend.petfinder.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.chat.data.ChatDetailDto
import com.frontend.petfinder.chat.data.ChatEstado
import com.frontend.petfinder.chat.data.ChatMessageAutorDto
import com.frontend.petfinder.chat.data.ChatMessageDto
import com.frontend.petfinder.chat.data.ChatParticipantDto
import com.frontend.petfinder.chat.data.ChatRepository
import com.frontend.petfinder.core.network.ChatMessageEvent
import com.frontend.petfinder.core.network.SocketManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class ConversationViewModel : ViewModel() {

    private var conversacionId = ""

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _detail = MutableStateFlow<ChatDetailDto?>(null)
    val detail: StateFlow<ChatDetailDto?> = _detail.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageDto>>(emptyList())
    val messages: StateFlow<List<ChatMessageDto>> = _messages.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** El otro participante (para el encabezado): el que NO soy yo. */
    val otroParticipante: StateFlow<ChatParticipantDto?> = MutableStateFlow(null)

    fun load(conversacionId: String) {
        if (this.conversacionId == conversacionId) return
        this.conversacionId = conversacionId

        viewModelScope.launch {
            _currentUserId.value = PetFinderApp.sessionManager.getUserId().first()
            _loading.value = true

            ChatRepository.getChatDetail(conversacionId).onSuccess { _detail.value = it }
            ChatRepository.getMessages(conversacionId).fold(
                onSuccess = { _messages.value = it },
                onFailure = { _error.value = "No se pudieron cargar los mensajes" }
            )
            _loading.value = false

            // Si el chat está activo, marcar como leído
            if (_detail.value?.estado == ChatEstado.ACEPTADA) {
                ChatRepository.markAsRead(conversacionId)
            }
            observeSocket()
        }
    }

    /** True si soy el rescatista y la invitación sigue pendiente (puedo aceptar/rechazar). */
    val esInvitacionPendienteParaMi: Boolean
        get() {
            val d = _detail.value ?: return false
            return d.estado == ChatEstado.PENDIENTE &&
                    d.rescatista?.usuarioId == _currentUserId.value
        }

    fun isMe(message: ChatMessageDto): Boolean =
        message.autorUsuarioId != null && message.autorUsuarioId == _currentUserId.value

    fun headerParticipant(): ChatParticipantDto? {
        val d = _detail.value ?: return null
        val myId = _currentUserId.value
        return if (d.dueno?.usuarioId == myId) d.rescatista else d.dueno
    }

    fun accept() {
        viewModelScope.launch {
            ChatRepository.acceptChat(conversacionId).fold(
                onSuccess = { reloadDetail(); ChatRepository.markAsRead(conversacionId) },
                onFailure = { _error.value = it.message }
            )
        }
    }

    fun decline(onDeclined: () -> Unit) {
        viewModelScope.launch {
            ChatRepository.declineChat(conversacionId).fold(
                onSuccess = { onDeclined() },
                onFailure = { _error.value = it.message }
            )
        }
    }

    private suspend fun reloadDetail() {
        ChatRepository.getChatDetail(conversacionId).onSuccess { _detail.value = it }
    }

    fun sendMessage(
        contenido: String? = null,
        foto: MultipartBody.Part? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        if (contenido.isNullOrBlank() && foto == null && (lat == null || lng == null)) return
        val handler = CoroutineExceptionHandler { _, _ ->
            _error.value = "Error inesperado al enviar"
            _sending.value = false
        }
        viewModelScope.launch(handler) {
            _sending.value = true
            ChatRepository.sendMessage(conversacionId, contenido, lat, lng, foto).fold(
                onSuccess = { msg -> appendUnique(msg); _error.value = null },
                onFailure = { _error.value = it.message ?: "No se pudo enviar el mensaje" }
            )
            _sending.value = false
        }
    }

    fun clearError() { _error.value = null }

    private var socketObserved = false

    private fun observeSocket() {
        if (socketObserved) return
        socketObserved = true

        // Mensajes nuevos en vivo
        viewModelScope.launch {
            SocketManager.chatMessageFlow.collect { event ->
                if (event.conversacionId != conversacionId) return@collect
                appendUnique(event.toDto())
                ChatRepository.markAsRead(conversacionId)
            }
        }

        // El rescatista aceptó → el dueño en espera se desbloquea al instante
        viewModelScope.launch {
            SocketManager.chatAcceptedFlow.collect { event ->
                if (event.conversacionId != conversacionId) return@collect
                reloadDetail()
                ChatRepository.markAsRead(conversacionId)
            }
        }

        // El rescatista rechazó → reflejar estado rechazado
        viewModelScope.launch {
            SocketManager.chatDeclinedFlow.collect { event ->
                if (event.conversacionId != conversacionId) return@collect
                reloadDetail()
            }
        }
    }

    private fun appendUnique(msg: ChatMessageDto) {
        _messages.update { current ->
            if (current.any { it.mensajeId == msg.mensajeId }) current else current + msg
        }
    }

    private fun ChatMessageEvent.toDto() = ChatMessageDto(
        mensajeId = mensajeId,
        conversacionId = conversacionId,
        autorUsuarioId = autorUsuarioId,
        contenido = contenido,
        fotoUrl = fotoUrl,
        lat = lat,
        lng = lng,
        creadoEl = creadoEl,
        leidoEl = null,
        autor = autorNombre?.let { ChatMessageAutorDto(nombre = it, fotoPerfilUrl = autorFotoUrl) }
    )
}
