package io.anyrtc.videolive.sdk

import android.content.Context
import io.anyrtc.videolive.api.bean.CDNStreamLayoutInfo
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.SpUtil
import io.anyrtc.videolive.utils.launch
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.RtcEngine
import org.ar.rtc.VideoEncoderConfiguration
import org.ar.rtc.live.LiveTranscoding
import org.ar.rtc.mediaplayer.ARMediaPlayerKit
import org.ar.rtc.rtmp.StreamingKit
import org.ar.rtc.rtmp.internal.PushMode

class RtcManager private constructor() {

    var rtcEngine: RtcEngine? = null
    private var rtcListener: RtcListener? = null
    lateinit var streamingKit: StreamingKit
    lateinit var videoPlayer:ARMediaPlayerKit

    private var enableEarMonitor = false

    fun initRtc(ctx: Context) {
        rtcEngine = RtcEngine.create(
            ctx,
            SpUtil.get().getString(Constans.APP_ID, ""),
            RtcEvent()
        )
    }

    fun initStreamKit() {
        streamingKit = StreamingKit.createInstance()
        streamingKit.setRtcEngine(rtcEngine)
        streamingKit.setMode(PushMode.VidMix)//视频合流
    }

    fun initMediaPlayer():ARMediaPlayerKit{
        videoPlayer = ARMediaPlayerKit()
        return videoPlayer
    }

    //加入RTC房间
    fun joinChannel(channelToken: String, channelId: String, userId: String, isHost: Boolean) {
        rtcEngine?.let {
            it.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            if (isHost) {
                it.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            } else {
                it.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
            }
            it.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
            )
            it.joinChannel(channelToken, channelId, "", userId)
        }
    }

    //离开RTC房间
    fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    //本地麦克风禁止传输 false: 禁止
    fun enableLocalAudio(enable: Boolean) {
        //rtcEngine?.muteLocalAudioStream(mute)
        rtcEngine?.enableLocalAudio(enable)
    }

    //本地视频禁止传输 false: 禁止
    fun enableLocalVideo(enable: Boolean) {
        //rtcEngine?.muteLocalVideoStream(mute)
        rtcEngine?.enableLocalVideo(enable)
    }

    //播放音乐
    fun startAudioMixing(url: String) {
        rtcEngine?.startAudioMixing(url, false, false, -1)
    }

    //暂停音乐
    fun parseAudioMixing() {
        rtcEngine?.pauseAudioMixing()
    }

    //恢复音乐
    fun resumeAudioMixing() {
        rtcEngine?.resumeAudioMixing()
    }

    //停止音乐
    fun stopAudioMixing() {
        rtcEngine?.stopAudioMixing()
    }

    //打开耳反
    fun enableInEarMonitoring(enabled: Boolean): Boolean {
        if (enableEarMonitor)
            rtcEngine?.enableInEarMonitoring(enabled)
        return enableEarMonitor
    }

    //反转摄像头
    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun enableVideo() {
        rtcEngine?.enableVideo()
    }

    //设置不同分辨率编码
    fun setVideoEncodeMode(mode: VideoEncodeMode) {
        rtcEngine?.let {
            it.setVideoEncoderConfiguration(VideoEncoderConfiguration().apply {
                dimensions = when (mode) {
                    VideoEncodeMode.LOW -> {
                        VideoEncoderConfiguration.VD_640x360
                    }
                    VideoEncodeMode.MEDIUM -> {
                        VideoEncoderConfiguration.VD_840x480
                    }
                    VideoEncodeMode.HIGH -> {
                        VideoEncoderConfiguration.VD_1280x720
                    }
                }
            })
        }
    }

    // add user to cdn stream
    fun setPushTranscodingArray(
        transcodingArr: List<LiveTranscoding.TranscodingUser>,
        canvasInfo: CDNStreamLayoutInfo,
        isStreamKit: Boolean = true
    ) {
        if (isStreamKit) {
            streamKitPushLiveTranscoding(transcodingArr, canvasInfo)
            return
        }
        publishPushLiveTranscoding(transcodingArr, canvasInfo)
    }

    // 客户端推流到CDN
    private fun streamKitPushLiveTranscoding(
        transcodingArr: List<LiveTranscoding.TranscodingUser>,
        canvasInfo: CDNStreamLayoutInfo
    ) {
        streamingKit.setLiveTranscoding(LiveTranscoding().apply {
            width = canvasInfo.width
            height = canvasInfo.height
        }.apply { transcodingArr.forEach { addUser(it) } })
    }

    // 服务端推流到CDN
    private fun publishPushLiveTranscoding(
        transcodingArr: List<LiveTranscoding.TranscodingUser>,
        canvasInfo: CDNStreamLayoutInfo
    ) {
        rtcEngine?.setLiveTranscoding(LiveTranscoding().apply {
            width = canvasInfo.width
            height = canvasInfo.height
        }.apply { transcodingArr.forEach { addUser(it) } })
    }

    //销毁SDK
    fun release() {
        RtcEngine.destroy()
        rtcEngine = null
    }

    fun releaseStreamKit() {
        streamingKit.unPushStream()
        streamingKit.release()
    }

    companion object {
        val instance: RtcManager by lazy() {
            RtcManager()
        }
    }

    fun registerListener(rtcListener: RtcListener) {
        this.rtcListener = rtcListener
    }

    fun unRegisterListener() {
        this.rtcListener = null
    }

    private inner class RtcEvent : IRtcEngineEventHandler() {

        //加入频道成功
        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            launch({
                rtcListener?.onJoinChannelSuccess(channel, uid, elapsed)
            })

        }

        //连接彻底断开
        override fun onConnectionLost() {
            super.onConnectionLost()
            launch({
                rtcListener?.onConnectionLost()
            })
        }

        //有用户加入频道
        override fun onUserJoined(uid: String?, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            launch({
                rtcListener?.onUserJoined(uid, elapsed)
            })

        }

        //用户离开频道
        override fun onUserOffline(uid: String?, reason: Int) {
            super.onUserOffline(uid, reason)
            launch({
                rtcListener?.onUserOffline(uid, reason)
            })

        }

        override fun onAudioVolumeIndication(
            speakers: Array<out AudioVolumeInfo>?,
            totalVolume: Int
        ) {
            launch({
                rtcListener?.onAudioVolumeIndication(speakers, totalVolume)
            })

        }

        override fun onRemoteAudioStateChanged(
            uid: String?,
            state: Int,
            reason: Int,
            elapsed: Int
        ) {
            super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            launch({
                rtcListener?.onRemoteAudioStateChanged(uid, state, reason, elapsed)
            })

        }

        override fun onLocalAudioStateChanged(state: Int, error: Int) {
            super.onLocalAudioStateChanged(state, error)
            launch({
                rtcListener?.onLocalAudioStateChanged(state, error)
            })
        }

        override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
            super.onLocalVideoStateChanged(localVideoState, error)
            launch({
                rtcListener?.onLocalVideoStateChanged(localVideoState, error)
            })
        }

        override fun onRemoteVideoStateChanged(
            uid: String?,
            state: Int,
            reason: Int,
            elapsed: Int
        ) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            launch({
                rtcListener?.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            })
        }

        override fun onWarning(warn: Int) {
            super.onWarning(warn)
            launch({
                rtcListener?.onWarning(warn)
            })
        }


        override fun onTokenPrivilegeWillExpire(token: String?) {
            super.onTokenPrivilegeWillExpire(token)
            launch({
                rtcListener?.onTokenPrivilegeWillExpire()
            })
        }

        override fun onRtmpStreamingStateChanged(url: String?, state: Int, errCode: Int) {
            super.onRtmpStreamingStateChanged(url, state, errCode)
            launch({
                rtcListener?.onRtmpStreamingStateChanged(url, state, errCode)
            })
        }

        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            super.onAudioMixingStateChanged(state, errorCode)
            launch({
                rtcListener?.onAudioMixingStateChanged(state, errorCode)
            })
        }

        override fun onAudioRouteChanged(routing: Int) {
            enableEarMonitor = Constants.AUDIO_ROUTE_HEADSET == routing
        }

        override fun onRtcStats(stats: RtcStats?) {
            super.onRtcStats(stats)
            launch({
                rtcListener?.onRtcStats(stats)
            })
        }


    }

    enum class VideoEncodeMode {
        LOW, MEDIUM, HIGH
    }

    enum class MusicState {
        IDEA, PLAYING, PAUSE, STOP, ERROR
    }

    enum class Role {
        HOST, GUEST
    }
}