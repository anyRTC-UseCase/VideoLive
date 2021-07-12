package io.anyrtc.videolive.vm

import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.sdk.RtmManager
import org.ar.rtc.Constants
import org.ar.rtm.RtmChannelAttribute

class RTCLiveVM : BaseLiveVM() {

    override fun messageAcceptLine() {
        RtcManager.instance.rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        applyRequestResponse.postValue(true)
    }

    override fun messageRejectLine() {
        applyRequestResponse.postValue(false)
    }

    override fun disconnection() {
        super.disconnection()
        RtcManager.instance.rtcEngine?.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
    }

    override fun onCleared() {
        if (isHost) {
            RtmManager.instance.setChannelAttribute(
                roomId, listOf(
                    RtmChannelAttribute("musicState", "0"),
                    RtmChannelAttribute("layout", "1")
                )
            )
        } else if (isRequestingApply) {
            cancelApply()
        }
        RtcManager.instance.release()
        RtmManager.instance.leaveChannel()
        RtmManager.instance.logOut()
    }

    fun join() {
        RtcManager.instance.joinChannel(rtcToken, roomId, userId, isHost)
        RtmManager.instance.joinChannel(roomId)
    }
}