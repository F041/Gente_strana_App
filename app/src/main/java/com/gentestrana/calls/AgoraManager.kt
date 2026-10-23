package com.gentestrana.calls

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class AgoraManager(private val context: Context) {

    private var rtcEngine: RtcEngine? = null
    var isAudioMuted: Boolean = false
        private set
    var isVideoEnabled: Boolean = false
        private set
    var remoteUid: Int? = null
        private set

    var onUserJoined: ((Int) -> Unit)? = null
    var onUserOffline: ((Int) -> Unit)? = null
    var onTokenPrivilegeWillExpire: (() -> Unit)? = null

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d("AgoraManager", "Remote user joined: $uid")
            remoteUid = uid
            onUserJoined?.invoke(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d("AgoraManager", "Remote user offline: $uid, reason: $reason")
            if (remoteUid == uid) remoteUid = null
            onUserOffline?.invoke(uid)
        }

        override fun onTokenPrivilegeWillExpire(token: String?) {
            Log.w("AgoraManager", "Token privilege will expire soon")
            onTokenPrivilegeWillExpire?.invoke()
        }

        override fun onRequestToken() {
            Log.w("AgoraManager", "Request new token from server")
            onTokenPrivilegeWillExpire?.invoke()
        }
    }

    fun init(appId: String) {
        if (rtcEngine != null) return
        try {
            val config = RtcEngineConfig()
            config.mContext = context.applicationContext
            config.mAppId = appId
            config.mEventHandler = eventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            rtcEngine = RtcEngine.create(config)
            rtcEngine?.enableAudio()
        } catch (e: Exception) {
            Log.e("AgoraManager", "Failed to init Agora RtcEngine", e)
        }
    }

    fun joinChannel(token: String, channelId: String, userAccount: String) {
        rtcEngine?.joinChannelWithUserAccount(token, channelId, userAccount)
    }

    fun renewToken(newToken: String) {
        rtcEngine?.renewToken(newToken)
    }

    fun toggleAudioMute(): Boolean {
        isAudioMuted = !isAudioMuted
        rtcEngine?.muteLocalAudioStream(isAudioMuted)
        return isAudioMuted
    }

    fun toggleVideo(): Boolean {
        isVideoEnabled = !isVideoEnabled
        if (isVideoEnabled) {
            rtcEngine?.enableVideo()
            rtcEngine?.startPreview()
        } else {
            rtcEngine?.stopPreview()
            rtcEngine?.disableVideo()
        }
        return isVideoEnabled
    }

    fun setupLocalVideoView(surfaceView: SurfaceView) {
        val canvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        rtcEngine?.setupLocalVideo(canvas)
    }

    fun setupRemoteVideoView(surfaceView: SurfaceView, uid: Int) {
        val canvas = VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        rtcEngine?.setupRemoteVideo(canvas)
    }

    fun leaveChannel() {
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        remoteUid = null
    }

    fun destroy() {
        leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}
