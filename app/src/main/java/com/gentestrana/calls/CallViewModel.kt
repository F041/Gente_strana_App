package com.gentestrana.calls

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallUiState(
    val call: Call? = null,
    val callId: String? = null,
    val chatId: String? = null,
    val callerName: String = "",
    val callerPhoto: String = "",
    val isOutgoing: Boolean = false,
    val isIncoming: Boolean = false,
    val isActive: Boolean = false,
    val isEnded: Boolean = false,
    val statusMessage: String = "",
    val formattedDuration: String = "0:00",
    val isAudioMuted: Boolean = false,
    val isVideoEnabled: Boolean = false,
    val remoteUid: Int? = null,
    val warningMessage: String? = null
)

class CallViewModel(
    private val repository: CallRepository = CallRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var agoraManager: AgoraManager? = null
    private var callObservationJob: Job? = null
    private var durationTimerJob: Job? = null
    private var tokenRenewalJob: Job? = null
    private var ringingTimeoutJob: Job? = null

    fun initOutgoingCall(context: Context, chatId: String, otherUserName: String, otherUserPhoto: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        agoraManager = AgoraManager(context)

        _uiState.value = CallUiState(
            chatId = chatId,
            callerName = otherUserName,
            callerPhoto = otherUserPhoto,
            isOutgoing = true,
            statusMessage = "Chiamata in corso..."
        )

        viewModelScope.launch {
            try {
                val callId = repository.startCall(chatId)
                _uiState.value = _uiState.value.copy(callId = callId)
                observeCallDocument(callId, currentUserId)

                // Timeout per chiamata persa se non risponde in 10s
                ringingTimeoutJob = viewModelScope.launch {
                    delay(10_000)
                    if (_uiState.value.isOutgoing && !_uiState.value.isActive) {
                        repository.markMissedCall(callId)
                        endCallWithStatus("Nessuna risposta")
                    }
                }
            } catch (e: Exception) {
                Log.e("CallViewModel", "Failed to start call: ${e.message}", e)
                endCallWithStatus(e.message ?: "Errore avvio chiamata")
            }
        }
    }

    fun initIncomingCall(context: Context, call: Call) {
        agoraManager = AgoraManager(context)
        _uiState.value = CallUiState(
            call = call,
            callId = call.id,
            chatId = call.chatId,
            callerName = call.callerName,
            callerPhoto = call.callerPhoto,
            isIncoming = true,
            statusMessage = "ti sta chiamando"
        )
        observeCallDocument(call.id, auth.currentUser?.uid ?: "")
    }

    fun answerCall() {
        val callId = _uiState.value.callId ?: return
        viewModelScope.launch {
            try {
                repository.answerCall(callId)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error answering call: ${e.message}", e)
                endCallWithStatus(e.message ?: "Errore nella risposta")
            }
        }
    }

    fun declineCall() {
        val callId = _uiState.value.callId ?: return
        viewModelScope.launch {
            try {
                repository.declineCall(callId)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error declining call: ${e.message}")
            } finally {
                endCallWithStatus("Chiamata rifiutata")
            }
        }
    }

    fun cancelCall() {
        val callId = _uiState.value.callId ?: return
        viewModelScope.launch {
            try {
                repository.cancelCall(callId)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error cancelling call: ${e.message}")
            } finally {
                endCallWithStatus("Chiamata annullata")
            }
        }
    }

    fun endCall() {
        val callId = _uiState.value.callId ?: return
        viewModelScope.launch {
            try {
                repository.endCall(callId)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Error ending call: ${e.message}")
            } finally {
                endCallWithStatus("Chiamata terminata")
            }
        }
    }

    fun toggleAudioMute() {
        val muted = agoraManager?.toggleAudioMute() ?: false
        _uiState.value = _uiState.value.copy(isAudioMuted = muted)
    }

    fun toggleVideo() {
        val enabled = agoraManager?.toggleVideo() ?: false
        _uiState.value = _uiState.value.copy(isVideoEnabled = enabled)
    }

    fun getAgoraManager(): AgoraManager? = agoraManager

    private fun observeCallDocument(callId: String, currentUserId: String) {
        callObservationJob?.cancel()
        callObservationJob = viewModelScope.launch {
            repository.observeCall(callId).collect { call ->
                if (call == null) {
                    if (!_uiState.value.isEnded) {
                        endCallWithStatus("Chiamata terminata")
                    }
                    return@collect
                }

                _uiState.value = _uiState.value.copy(call = call)

                when (call.status) {
                    CallStatus.ACTIVE -> {
                        ringingTimeoutJob?.cancel()
                        if (!_uiState.value.isActive) {
                            joinAgoraChannel(callId)
                            startDurationTimer(call.answeredAt?.toDate()?.time ?: System.currentTimeMillis())
                        }
                    }
                    CallStatus.DECLINED -> endCallWithStatus("Chiamata rifiutata")
                    CallStatus.CANCELLED -> endCallWithStatus("Chiamata annullata")
                    CallStatus.MISSED -> endCallWithStatus("Nessuna risposta")
                    CallStatus.ENDED -> endCallWithStatus("Chiamata terminata")
                    CallStatus.RINGING -> {}
                }
            }
        }
    }

    private fun joinAgoraChannel(callId: String) {
        val myUid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val tokenResult = repository.getCallToken(callId)
                agoraManager?.init(tokenResult.appId)
                agoraManager?.onUserJoined = { remoteUid ->
                    _uiState.value = _uiState.value.copy(remoteUid = remoteUid)
                }
                agoraManager?.onUserOffline = {
                    _uiState.value = _uiState.value.copy(remoteUid = null)
                }
                agoraManager?.onTokenPrivilegeWillExpire = {
                    renewAgoraToken(callId)
                }

                agoraManager?.joinChannel(
                    token = tokenResult.token,
                    channelId = callId,
                    userAccount = myUid
                )

                _uiState.value = _uiState.value.copy(
                    isActive = true,
                    isOutgoing = false,
                    isIncoming = false,
                    statusMessage = "In chiamata"
                )

                scheduleTokenRenewal(callId, tokenResult.expiresAt)
            } catch (e: Exception) {
                Log.e("CallViewModel", "Failed to get call token or join channel: ${e.message}", e)
                endCallWithStatus(e.message ?: "Errore connessione audio")
            }
        }
    }

    private fun scheduleTokenRenewal(callId: String, expiresAtMs: Long) {
        tokenRenewalJob?.cancel()
        tokenRenewalJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val timeUntilRenewal = (expiresAtMs - now) - 60_000
            if (timeUntilRenewal > 0) {
                delay(timeUntilRenewal)
            }
            renewAgoraToken(callId)
        }
    }

    private fun renewAgoraToken(callId: String) {
        viewModelScope.launch {
            try {
                val tokenResult = repository.getCallToken(callId)
                agoraManager?.renewToken(tokenResult.token)
                scheduleTokenRenewal(callId, tokenResult.expiresAt)
            } catch (e: Exception) {
                Log.w("CallViewModel", "Token renewal failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    warningMessage = "La chiamata finirà tra un minuto: minuti finiti."
                )
            }
        }
    }

    private fun startDurationTimer(startTimeMs: Long) {
        durationTimerJob?.cancel()
        durationTimerJob = viewModelScope.launch {
            while (_uiState.value.isActive) {
                val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000).coerceAtLeast(0)
                val minutes = durationSec / 60
                val seconds = durationSec % 60
                val formatted = if (minutes >= 60) {
                    val hours = minutes / 60
                    val remMin = minutes % 60
                    String.format("%d:%02d:%02d", hours, remMin, seconds)
                } else {
                    String.format("%d:%02d", minutes, seconds)
                }
                _uiState.value = _uiState.value.copy(formattedDuration = formatted)
                delay(1000)
            }
        }
    }

    private fun endCallWithStatus(message: String) {
        durationTimerJob?.cancel()
        tokenRenewalJob?.cancel()
        callObservationJob?.cancel()
        ringingTimeoutJob?.cancel()

        agoraManager?.leaveChannel()
        agoraManager?.destroy()
        agoraManager = null

        _uiState.value = _uiState.value.copy(
            isActive = false,
            isIncoming = false,
            isOutgoing = false,
            isEnded = true,
            statusMessage = message
        )
    }

    override fun onCleared() {
        super.onCleared()
        durationTimerJob?.cancel()
        tokenRenewalJob?.cancel()
        callObservationJob?.cancel()
        ringingTimeoutJob?.cancel()

        agoraManager?.destroy()
        agoraManager = null
    }
}
