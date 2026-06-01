package com.frontend.petfinder.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frontend.petfinder.core.network.SocketManager
import com.frontend.petfinder.sightings.data.MyParticipationDto
import com.frontend.petfinder.sightings.data.SightingThreadDto
import com.frontend.petfinder.sightings.data.SightingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _myPetsThreads = MutableStateFlow<List<SightingThreadDto>>(emptyList())
    val myPetsThreads: StateFlow<List<SightingThreadDto>> = _myPetsThreads.asStateFlow()

    private val _myParticipations = MutableStateFlow<List<MyParticipationDto>>(emptyList())
    val myParticipations: StateFlow<List<MyParticipationDto>> = _myParticipations.asStateFlow()

    private val _unreadTotal = MutableStateFlow(0)
    val unreadTotal: StateFlow<Int> = _unreadTotal.asStateFlow()

    private val _unreadOwner = MutableStateFlow(0)
    val unreadOwner: StateFlow<Int> = _unreadOwner.asStateFlow()

    private val _unreadRescuer = MutableStateFlow(0)
    val unreadRescuer: StateFlow<Int> = _unreadRescuer.asStateFlow()

    private val _isLoadingThreads = MutableStateFlow(false)
    val isLoadingThreads: StateFlow<Boolean> = _isLoadingThreads.asStateFlow()

    private val _isLoadingParticipations = MutableStateFlow(false)
    val isLoadingParticipations: StateFlow<Boolean> = _isLoadingParticipations.asStateFlow()

    init {
        loadAll()
        observeNewComments()
    }

    fun loadAll() {
        loadMyPetsThreads()
        loadMyParticipations()
        loadUnreadCount()
    }

    fun loadMyPetsThreads() {
        viewModelScope.launch {
            _isLoadingThreads.value = true
            SightingsRepository.getMyPetsThreads().onSuccess { _myPetsThreads.value = it }
            _isLoadingThreads.value = false
        }
    }

    fun loadMyParticipations() {
        viewModelScope.launch {
            _isLoadingParticipations.value = true
            SightingsRepository.getMyParticipations().onSuccess { _myParticipations.value = it }
            _isLoadingParticipations.value = false
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            SightingsRepository.getUnreadCount().onSuccess { dto ->
                _unreadTotal.value = dto.total
                _unreadOwner.value = dto.comoDueno
                _unreadRescuer.value = dto.comoRescatista
            }
        }
    }

    // Cuando se abre un hilo y se llama markAsRead, refrescamos el badge
    fun onThreadOpened() {
        loadUnreadCount()
    }

    private fun observeNewComments() {
        viewModelScope.launch {
            SocketManager.sightingCommentFlow.collect {
                // Nuevo comentario → badge sube; refrescar conteo y listas
                loadUnreadCount()
                loadMyPetsThreads()
                loadMyParticipations()
            }
        }
    }
}
