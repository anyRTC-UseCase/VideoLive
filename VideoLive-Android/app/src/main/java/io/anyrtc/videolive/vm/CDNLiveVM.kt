package io.anyrtc.videolive.vm

import android.view.TextureView
import androidx.lifecycle.MutableLiveData
import io.anyrtc.videolive.api.bean.CDNStreamLayoutInfo
import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.sdk.RtmManager
import org.ar.rtc.Constants
import org.ar.rtc.live.LiveTranscoding
import org.ar.rtc.mediaplayer.ARMediaPlayerKit
import org.ar.rtc.mediaplayer.MediaPlayerObserver
import org.ar.rtc.mediaplayer.PlayerConstans
import org.ar.rtc.video.VideoCanvas
import org.ar.rtm.RtmChannelAttribute

class CDNLiveVM : BaseLiveVM() {

    private lateinit var cdnMediaPlayer: ARMediaPlayerKit

    val expiredAlert = MutableLiveData<Unit>()

    private var densityCanvasConfigArr = arrayOf(
        CDNStreamLayoutInfo.RESOLUTION_NORMAL,
        CDNStreamLayoutInfo.RESOLUTION_NORMAL,
        CDNStreamLayoutInfo.RESOLUTION_NORMAL,
        //CDNStreamLayoutInfo.RESOLUTION_HIGH,
        //CDNStreamLayoutInfo.RESOLUTION_ULTRA
    )
    private var densityCanvasConfig = densityCanvasConfigArr[0]

    /* CDN流是否为加载中(通知View显示Loading画面) */
    val streamLoadStates = MutableLiveData<Boolean>()

    private var guestLoadMediaListener: MediaPlayerObserver? = null
    override fun messageAcceptLine() {
        joinRtc(true)
        cdnMediaPlayer.mute(true)
        applyRequestResponse.postValue(true)
    }

    override fun messageRejectLine() {
        applyRequestResponse.postValue(false)
    }

    override fun onHostUserJoin(uid: String) {
        addUser(CDNStreamLayoutInfo(uid))
    }

    override fun disconnection() {
        connectedUserInfoList.clear()
        //super.disconnection()
        cdnMediaPlayer.mute(false)
        RtcManager.instance.leaveChannel()
    }

    override fun joinChannelSuccess() {
        if (isHost) {
            if (roomType == 7) {
                addUser(CDNStreamLayoutInfo("0"))
                RtcManager.instance.streamingKit.pushStream(cdnUrl)
            } else if (roomType == 8) {
                addUser(CDNStreamLayoutInfo(userId))
                RtcManager.instance.rtcEngine?.addPublishStreamUrl(cdnUrl, true)
            }
        }
    }

    /*override fun densityChange(index: Int) {
        densityCanvasConfig = densityCanvasConfigArr[resolutionSelectedIndex]
        RtcManager.instance.setPushTranscodingArray(
            transcodingUserArray,
            densityCanvasConfig,
            roomType == 7
        )
    }*/

    fun resetHostViewState(): Boolean {
        val hostInfoIndex = getConnListIndex(hostId)
        if (hostInfoIndex == -1) {
            return false
        }

        val value = connectedUserInfoList[hostInfoIndex]
        value.isCloseCamera = false
        value.isMute = false
        value.index = hostInfoIndex
        cameraMicrophoneStateChange.value = value

        switchVideoStatus(true)
        switchVoiceStatus(true)

        connectedUserInfoList.clear()
        return true
    }

    fun loadCDNHostStream(textureView: TextureView) {
        cdnMediaPlayer = RtcManager.instance.initMediaPlayer().apply {
            open(cdnUrl, 0)
            setView(textureView)
            play()
            if (!isHost) {
                guestLoadMediaListener = object : MediaPlayerObserver {
                    override fun onPlayerStateChanged(
                        p0: PlayerConstans.MediaPlayerState?,
                        p1: PlayerConstans.MediaPlayerError?
                    ) {
                        if (p0 == null) {
                            return
                        }
                        when (p0) {
                            PlayerConstans.MediaPlayerState.PLAYER_STATE_OPENING -> {
                                streamLoadStates.postValue(true)
                            }
                            PlayerConstans.MediaPlayerState.PLAYER_STATE_PLAYING -> {
                                streamLoadStates.postValue(false)
                            }
                            else -> {
                                // do nothing.
                            }
                        }
                    }

                    override fun onPositionChanged(p0: Long) {
                    }

                    override fun onPlayerEvent(p0: PlayerConstans.MediaPlayerEvent?) {
                    }

                    override fun onMetaData(
                        p0: PlayerConstans.MediaPlayerMetadataType?,
                        p1: ByteArray?
                    ) {
                    }
                }
                registerPlayerObserver(guestLoadMediaListener)
            }
        }
    }

    fun rtcSetupRemoteVideo(textureView: TextureView, uid: String) {
        RtcManager.instance.rtcEngine?.setupRemoteVideo(
            VideoCanvas(textureView, Constants.RENDER_MODE_HIDDEN, uid)
        )
    }

    fun joinRtc(host: Boolean = isHost) {
        RtcManager.instance.joinChannel(rtcToken, roomId, userId, host)
    }

    fun joinRtm() {
        RtmManager.instance.joinChannel(roomId)
        if (!isHost) RtmManager.instance.subscribePeersOnlineStatus(hostId)
    }

    // mode: true = topic, false = equally divided
    override fun onToggleLayoutMode(mode: Boolean) {
        isLayoutTopicMode = mode
        calcPosition(CALC_TOGGLE)
    }

    private fun addUser(cdnUserLayoutInfo: CDNStreamLayoutInfo) {
        calcPosition(CALC_ADD, cdnUserLayoutInfo)
    }

    private fun removeUser(uid: String) {
        transcodingUserArray.find { it.uid == uid }?.let { transcodingUserArray.remove(it) }
        calcPosition(CALC_REMOVE)
    }

    private fun calcPosition(calcMode: Int, cdnUserLayoutInfo: CDNStreamLayoutInfo? = null) {
        if ((calcMode == CALC_ADD && cdnUserLayoutInfo == null) || (calcMode != CALC_ADD && transcodingUserArray.isEmpty())) {
            return
        }

        if (transcodingUserArray.isEmpty() && calcMode == CALC_ADD) {
            transcodingUserArray.add(LiveTranscoding.TranscodingUser().apply {
                uid = cdnUserLayoutInfo!!.uid
                x = 0
                y = 0
                width = densityCanvasConfig.width
                height = densityCanvasConfig.height
            })
            RtcManager.instance.setPushTranscodingArray(
                transcodingUserArray,
                densityCanvasConfig,
                roomType == 7
            )
            return
        }

        if (transcodingUserArray.size == 1 || isLayoutTopicMode) {
            transcodingUserArray[0].apply {
                x = 0
                y = 0
                width = densityCanvasConfig.width
                height = densityCanvasConfig.height
            }
            if (transcodingUserArray.size == 1 && calcMode != CALC_ADD) {
                RtcManager.instance.setPushTranscodingArray(
                    transcodingUserArray,
                    densityCanvasConfig,
                    roomType == 7
                )
                return
            }
        }

        if (isLayoutTopicMode) {
            val padding =
                (densityCanvasConfig.height * CDNStreamLayoutInfo.TOPIC_MULTIPLE_PADDING).toInt()
            val topPadding = (densityCanvasConfig.height * CDNStreamLayoutInfo.TOP_PADDING).toInt()

            val nWidth = (densityCanvasConfig.width * CDNStreamLayoutInfo.TOPIC_WIDTH).toInt()
            val nHeight = (nWidth * CDNStreamLayoutInfo.HEIGHT_PERCENT).toInt()

            for (i in 1 until transcodingUserArray.size) {
                val userLayoutInfo = transcodingUserArray[i]
                userLayoutInfo.run {
                    x = densityCanvasConfig.width - padding - nWidth
                    y = topPadding + (i - 1) * nHeight + (i - 1) * padding
                    width = nWidth
                    height = nHeight
                }
            }

            if (calcMode == CALC_ADD) transcodingUserArray.add(
                LiveTranscoding.TranscodingUser().apply {
                    uid = cdnUserLayoutInfo!!.uid
                    x = densityCanvasConfig.width - padding - nWidth
                    y =
                        topPadding + (transcodingUserArray.size - 1) * nHeight + (transcodingUserArray.size - 1) * padding
                    width = nWidth
                    height = nHeight
                })
        } else {
            //val topPadding = (densityCanvasConfig.height * CDNStreamLayoutInfo.TOP_PADDING).toInt()
            val topPadding = 0

            val nWidth = densityCanvasConfig.width.shr(1)
            val nHeight = (nWidth * CDNStreamLayoutInfo.HEIGHT_PERCENT).toInt()

            var posOffset = 0
            var pos = 0
            if (transcodingUserArray.size == calcMode) {
                posOffset = 2
                pos++

                transcodingUserArray[0].run {
                    x = densityCanvasConfig.width.shr(1) - nWidth.shr(1)
                    y = topPadding
                    width = nWidth
                    height = nHeight
                }
            }

            for (i in pos until transcodingUserArray.size) {
                val topFloor = posOffset / 2
                val leftFloor = posOffset % 2
                transcodingUserArray[i].run {
                    x = leftFloor * nWidth + leftFloor * CDNStreamLayoutInfo.MULTIPLE_PADDING
                    y =
                        topFloor * CDNStreamLayoutInfo.MULTIPLE_PADDING + topFloor * nHeight + topPadding
                    width = nWidth
                    height = nHeight
                }
                posOffset++
            }

            if (calcMode == CALC_ADD) {
                val topFloor = posOffset / 2
                val leftFloor = posOffset % 2
                val nLeft = leftFloor * nWidth + leftFloor * CDNStreamLayoutInfo.MULTIPLE_PADDING
                val nTop =
                    topFloor * CDNStreamLayoutInfo.MULTIPLE_PADDING + topFloor * nHeight + topPadding

                transcodingUserArray.add(LiveTranscoding.TranscodingUser().apply {
                    uid = cdnUserLayoutInfo!!.uid
                    x = nLeft
                    y = nTop
                    width = nWidth
                    height = nHeight
                })
            }
        }

        RtcManager.instance.setPushTranscodingArray(
            transcodingUserArray,
            densityCanvasConfig,
            roomType == 7
        )
    }

    override fun onCleared() {
        if (isHost) {
            RtcManager.instance.run {
                if (roomType == 7) releaseStreamKit()
            }
            RtmManager.instance.setChannelAttribute(
                roomId, listOf(
                    RtmChannelAttribute("musicState", "0"),
                    RtmChannelAttribute("layout", "1")
                )
            )
        } else {
            if (isRequestingApply) {
                cancelApply()
            }
            //RtcManager.instance.videoPlayer.unRegisterPlayerObserver(guestLoadMediaListener)
            if (roomType == 7 || roomType == 8) {
                RtcManager.instance.videoPlayer.run {
                    stop()
                    unRegisterPlayerObserver(guestLoadMediaListener)
                    destroy()
                }
            }
        }
        RtcManager.instance.release()
        RtmManager.instance.leaveChannel()
        RtmManager.instance.logOut()
    }

    override fun userOffline(uid: String) {
        removeUser(uid)
    }

    override fun onExpired() {
        expiredAlert.postValue(Unit)
    }
}