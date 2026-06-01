package com.frontend.petfinder.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.PetFinderApp
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.sightings.data.SightingCommentDto
import com.frontend.petfinder.sightings.data.SightingsRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class SightingThreadViewModel : ViewModel() {

    private var avistamientoId = ""
    private var inferredRescatistaId = ""

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    private val _comments = MutableStateFlow<List<SightingCommentDto>>(emptyList())
    val comments: StateFlow<List<SightingCommentDto>> = _comments.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    fun load(avistamientoId: String, rescatistaUsuarioId: String) {
        if (this.avistamientoId == avistamientoId) return
        this.avistamientoId = avistamientoId
        this.inferredRescatistaId = rescatistaUsuarioId

        viewModelScope.launch {
            _currentUserId.value = PetFinderApp.sessionManager.getUserId().first()
            SightingsRepository.markAsRead(avistamientoId)

            _loading.value = true
            SightingsRepository.getComments(avistamientoId).fold(
                onSuccess = { comments ->
                    _comments.value = comments
                    detectRole(comments, rescatistaUsuarioId)
                },
                onFailure = { _error.value = "No se pudieron cargar los mensajes" }
            )
            _loading.value = false
            observeSocketEvents()
        }
    }

    private fun detectRole(comments: List<SightingCommentDto>, hintRescatistaId: String) {
        val myId = _currentUserId.value ?: return

        val iOwnerByReply = comments.any { it.autorUsuarioId == myId && it.replyToUserId != null }
        val isRescuerByReceivedReply = comments.any { it.replyToUserId == myId }

        val isOwner = when {
            iOwnerByReply -> true
            isRescuerByReceivedReply -> false
            hintRescatistaId.isNotBlank() -> myId != hintRescatistaId
            else -> false
        }
        _isOwner.value = isOwner

        if (isOwner && inferredRescatistaId.isBlank()) {
            val rescatistaId = comments
                .filter { it.autorUsuarioId != null && it.autorUsuarioId != myId && it.replyToUserId == null }
                .map { it.autorUsuarioId }
                .firstOrNull()
            if (rescatistaId != null) inferredRescatistaId = rescatistaId
        }
    }

    // mensaje es nullable — se puede enviar solo foto sin texto
    fun sendMessage(
        mensaje: String? = null,
        foto: MultipartBody.Part? = null,
        lat: Double? = null,
        lng: Double? = null
    ) {
        if (mensaje.isNullOrBlank() && foto == null) return
        val handler = CoroutineExceptionHandler { _, _ ->
            _error.value = "Error inesperado al enviar"
            _sending.value = false
        }
        viewModelScope.launch(handler) {
            _sending.value = true
            val replyToUserId = if (_isOwner.value && inferredRescatistaId.isNotBlank()) {
                inferredRescatistaId
            } else null
            val gpsLat = if (foto != null) lat else null
            val gpsLng = if (foto != null) lng else null

            SightingsRepository.addComment(
                avistamientoId, mensaje, gpsLat, gpsLng, replyToUserId, foto
            ).fold(
                onSuccess = { comment ->
                    _comments.update { current ->
                        if (current.any { it.comentarioId == comment.comentarioId }) current
                        else current + comment
                    }
                    _error.value = null
                },
                onFailure = { _error.value = "No se pudo enviar el mensaje" }
            )
            _sending.value = false
        }
    }

    fun clearError() { _error.value = null }

    private fun observeSocketEvents() {
        viewModelScope.launch {
            SocketManager.sightingCommentFlow.collect { event ->
                if (event.avistamientoId != avistamientoId) return@collect
                val newComment = SightingCommentDto(
                    comentarioId = event.comentarioId,
                    avistamientoId = event.avistamientoId,
                    autorUsuarioId = null,
                    replyToUserId = null,
                    mensaje = event.mensaje,
                    fotoUrl = event.fotoUrl,
                    lat = event.lat,
                    lng = event.lng,
                    creadoEl = event.creadoEl,
                    autor = null
                )
                _comments.update { current ->
                    if (current.any { it.comentarioId == event.comentarioId }) current
                    else current + newComment
                }
                SightingsRepository.markAsRead(avistamientoId)
            }
        }
    }
}
