package io.anyrtc.videolive.sdk
import org.ar.rtm.*

abstract class RtmListener : RtmClientListener,RtmChannelListener {

    abstract fun onJoinChannelSuccess(channelId: String?)

    override fun onTokenExpired() {
    }

    override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
    }

    override fun onConnectionStateChanged(var1: Int, var2: Int) {
    }

    abstract fun onP2PMessageReceived(var1: RtmMessage?, var2: String?)

    override fun onAttributesUpdated(var1: MutableList<RtmChannelAttribute>?) {
    }

    abstract fun onChannelMessageReceived(var1: RtmMessage?, var2: RtmChannelMember?)

    override fun onMemberJoined(var1: RtmChannelMember?) {
    }

    override fun onMemberLeft(var1: RtmChannelMember?) {
    }

    override fun onMemberCountUpdated(var1: Int) {
    }
}