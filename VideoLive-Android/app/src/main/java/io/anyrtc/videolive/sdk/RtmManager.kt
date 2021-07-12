package io.anyrtc.videolive.sdk

import androidx.annotation.Nullable
import io.anyrtc.videolive.App
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.SpUtil
import io.anyrtc.videolive.utils.launch
import org.ar.rtm.*


class RtmManager private constructor() {

    private var rtmClient: RtmClient? = null

    private var rtmCallBack: RtmListener? = null

    private var rtmChannel: RtmChannel? = null

    var isLoginSuccess = false

    fun initRtm() {
        rtmClient = RtmClient.createInstance(
            App.app.applicationContext,
            SpUtil.get().getString(Constans.APP_ID, "").toString(),
            RtmEvent()
        )
    }

    fun registerListener(rtmCallBack: RtmListener) {
        this.rtmCallBack = rtmCallBack
    }

    fun unregisterListener() {
        this.rtmCallBack = null
    }

    //发送p2p消息
    fun sendPeerMessage(userId: String, json: String) {
        rtmClient?.let {
            it.sendMessageToPeer(
                userId,
                it.createMessage(json),
                SendMessageOptions().apply { enableOfflineMessaging = true },
                null
            )
        }
    }

    //订阅某个人的在线状态
    fun subscribePeersOnlineStatus(uid: String) {
        rtmClient?.let {
            val queryArray = setOf(uid)
            it.subscribePeersOnlineStatus(queryArray, null)
        }
    }

    //发送频道消息
    fun sendChannelMessage(json: String) {
        rtmChannel?.sendMessage(rtmClient?.createMessage(json), object : ResultCallback<Void> {
            override fun onSuccess(var1: Void?) {
            }

            override fun onFailure(var1: ErrorInfo?) {
            }

        })
    }

    fun getMembers(callback: (List<RtmChannelMember>?) -> Unit) {
        rtmChannel?.getMembers(object : ResultCallback<List<RtmChannelMember>> {
            override fun onSuccess(var1: List<RtmChannelMember>?) {
                callback.invoke(var1)
            }

            override fun onFailure(var1: ErrorInfo?) {
            }
        })
    }

    // set attributes
    fun setChannelAttribute(channelId: String, list: List<RtmChannelAttribute>) {
        rtmClient?.addOrUpdateChannelAttributes(
            channelId,
            list,
            ChannelAttributeOptions(true),
            null
        )
    }

    //登录rtm
    fun login(token: String, userId: String, @Nullable callback: ResultCallback<Void>) {
        logOut()// 防止断网未能释放资源导致加入失败

        rtmClient?.login(token, userId, object : ResultCallback<Void> {
            override fun onSuccess(var1: Void?) {
                callback.onSuccess(var1)
                isLoginSuccess = true
            }

            override fun onFailure(var1: ErrorInfo?) {
                callback.onFailure(var1)
                isLoginSuccess = false
            }

        })
    }

    //登出
    fun logOut() {
        rtmClient?.logout(null)
    }

    //加入频道
    fun joinChannel(channelId: String) {
        rtmChannel?.leave(null)// 防止断网未能释放资源导致加入失败

        rtmChannel = rtmClient?.createChannel(channelId, ChannelEvent())
        rtmChannel?.join(object : ResultCallback<Void> {
            override fun onSuccess(var1: Void?) {
                launch({
                    rtmCallBack?.onJoinChannelSuccess(channelId)
                })
            }

            override fun onFailure(var1: ErrorInfo?) {
            }
        })
    }

    fun leaveChannel() {
        rtmChannel?.let {
            it.leave(null)
            it.release()
        }
    }

    fun release() {
        rtmClient?.release()
        rtmClient = null
    }

    private inner class RtmEvent : RtmClientListener {

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            launch({
                rtmCallBack?.onConnectionStateChanged(state, reason)
            })

        }

        override fun onMessageReceived(var1: RtmMessage?, var2: String?) {
            launch({
                rtmCallBack?.onP2PMessageReceived(var1, var2)
            })

        }

        override fun onTokenExpired() {

        }

        override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
            launch({
                rtmCallBack?.onPeersOnlineStatusChanged(var1)
            })

        }
    }

    private inner class ChannelEvent : RtmChannelListener {
        override fun onMemberCountUpdated(var1: Int) {
            launch({
                rtmCallBack?.onMemberCountUpdated(var1)
            })
        }

        override fun onAttributesUpdated(var1: MutableList<RtmChannelAttribute>?) {
            launch({
                rtmCallBack?.onAttributesUpdated(var1)
            })
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: RtmChannelMember?) {
            launch({
                rtmCallBack?.onChannelMessageReceived(var1, var2)
            })

        }

        override fun onMemberJoined(var1: RtmChannelMember?) {
            launch({
                rtmCallBack?.onMemberJoined(var1)
            })

        }

        override fun onMemberLeft(var1: RtmChannelMember?) {
            launch({
                rtmCallBack?.onMemberLeft(var1)
            })

        }
    }


    companion object {
        val instance: RtmManager by lazy() {
            RtmManager()
        }
    }

}