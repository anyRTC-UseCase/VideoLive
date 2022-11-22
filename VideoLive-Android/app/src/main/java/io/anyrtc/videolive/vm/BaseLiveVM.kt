package io.anyrtc.videolive.vm

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyrtc.videolive.api.ServerManager
import io.anyrtc.videolive.api.bean.*
import io.anyrtc.videolive.sdk.*
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.SpUtil
import io.anyrtc.videolive.utils.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ar.rtc.Constants
import org.ar.rtc.IRtcEngineEventHandler
import org.ar.rtc.live.LiveTranscoding
import org.ar.rtm.RtmChannelAttribute
import org.ar.rtm.RtmChannelMember
import org.ar.rtm.RtmMessage
import org.json.JSONObject
import rxhttp.awaitResult
import java.util.*

abstract class BaseLiveVM : ViewModel() {

    protected var roomId = ""
    protected var rtmToken = ""
    protected var rtcToken = ""
    protected var roomType = 0
    val userId = SpUtil.get().getString(Constans.USER_ID, "").toString()
    private val userAvatar = SpUtil.get().getString(Constans.USER_ICON, "").toString()
    private val userNickname = SpUtil.get().getString(Constans.USER_NAME, "").toString()
    var isHost = false
        protected set

    protected var hostId = ""
    protected var cdnUrl = ""

    var isVideoOnline = false

    /* 上行丢包 + 网络延迟 */
    val onLagAndUploadLossRateChange = MutableLiveData<Int>()

    /* 下行丢包 + 网络延迟 */
    val onLagAndDownloadLossRateChange = MutableLiveData<Int>()

    /*
    摄像头是否开启、麦克风是否开启、耳反是否开启
     */
    //private var localVideoEnabled = true
    //private var localVoiceEnabled = true
    private var earMonitorEnabled = false

    /* true为大小屏、false为等分 */
    protected var isLayoutTopicMode = true

    /* 用户连麦、挂断连麦 */
    val userJoin = MutableLiveData<RtcMember>()
    val userLeave = MutableLiveData<RtcMember>()
    val requesterLost = MutableLiveData<GuestUserInfo>()

    /* 频道销毁(主播离开)、用户发送聊天消息 */
    //val channelDestroy = MutableLiveData<Unit>()
    val onChatMessage = MutableLiveData<ChatMessageData>()

    /* 请求连麦状态回调(主播点击同意/取消) */
    val applyRequestResponse = MutableLiveData<Boolean>()

    /* 修改分辨率时 */
    val densityChange = MutableLiveData<Int>()

    /* 音乐播放相关状态 */
    val musicStateChange = MutableLiveData<RtcManager.MusicState>()
    private var musicStage = RtcManager.MusicState.IDEA //初始状态，播完完成

    /* 麦克风/摄像头，开启/关闭时 */
    val cameraMicrophoneStateChange = MutableLiveData<GuestUserInfo>()

    /* 切换等分屏/大小屏布局时 */
    val onLayoutModeChange = MutableLiveData<Boolean>()

    /* 耳反开启失败(默认直接提示插入耳机) */
    val earMonitorFail = MutableLiveData<Unit>()
    val earMonitorIconSwitch = MutableLiveData<Boolean>()

    /* 记录上一次选中的分辨率(索引) */
    protected var resolutionSelectedIndex = 0

    /* 分辨率常量 */
    private val densityArr = arrayOf(
        RtcManager.VideoEncodeMode.LOW,
        RtcManager.VideoEncodeMode.MEDIUM,
        RtcManager.VideoEncodeMode.HIGH,
    )

    private val bitrateArr = arrayOf(
        500, 800, 1200
    )

    /* 当主播接收到游客连麦请求 */
    val onGuestReq = MutableLiveData<GuestUserInfo>()

    /* 记录主播和上麦游客(主播第一位) */
    protected val connectedUserInfoList = LinkedList<GuestUserInfo>()

    /* 记录所有游客连麦请求 */
    val guestReqConnInfoList = LinkedList<GuestUserInfo>()

    /* 用户离开倒计时, ture = 开始倒计时、false = 重置倒计时 */
    val leaveCountDown = MutableLiveData<Boolean>()
    val hostLost = MutableLiveData<Unit>()
    val selfLost = MutableLiveData<Boolean>()

    private var musicList = mutableListOf<MusicBean.DataBean>()

    // record queue uid to index
    //protected val guestReqUidToIndex = LinkedHashSet<String>()

    // record CDN joined users
    protected val transcodingUserArray = mutableListOf<LiveTranscoding.TranscodingUser>()

    // record when guest request 'apply'
    var isRequestingApply = false
        protected set

    protected var isDisconnect = false //是否离线了

    protected companion object {
        const val CALC_REMOVE = 3
        const val CALC_ADD = 2
        const val CALC_TOGGLE = 3

        /* 丢包 & 网络延迟 */
        const val LOSS_RATE_OFFSET = 20
        const val LOSS_RATE_FILTER = 0x0FF00000
        const val LAGGING_FILTER = 0x000FFFFF
    }

    abstract fun messageAcceptLine()
    abstract fun messageRejectLine()

    protected open fun onUserJoin(uid: String) {
        userJoin.value = RtcMember(uid)
        if (isHost) {
            val index = getLinkedUserIndex(guestReqConnInfoList, uid)
            val data = guestReqConnInfoList.removeAt(index)
            data.waitForJoinRTC = false
            connectedUserInfoList.add(data)

            onHostUserJoin(uid)
            return
        }
        getUserInfo(uid) {
            if (uid == hostId) {
                connectedUserInfoList.addFirst(
                    GuestUserInfo(
                        uid,
                        it.data.avatar,
                        it.data.userName
                    )
                )
            } else {
                connectedUserInfoList.add(
                    GuestUserInfo(
                        uid,
                        it.data.avatar,
                        it.data.userName
                    )
                )
            }
        }
    }

    protected open fun onHostUserJoin(uid: String) {
    }

    protected open fun userOffline(uid: String) {
    }

    protected open fun joinChannelSuccess() {
    }

    protected open fun onToggleLayoutMode(mode: Boolean) {
    }

    protected open fun densityChange(index: Int) {
    }

    protected open fun onExpired() {
    }

    private fun reconnectedInternet() {
        RtmManager.instance.getMembers {
            if (it == null || it.find { elem -> elem.userId == hostId } == null)
                hostLost.postValue(Unit)
        }
    }

    private fun checkUserExists(uid: String, callback: (exists: Boolean) -> Unit) {
        RtmManager.instance.getMembers {
            callback.invoke(!(it == null || it.find { elem -> elem.userId == uid } == null))
        }
    }

    fun onDensityChange(index: Int) {
        if (index == resolutionSelectedIndex)
            return

        RtcManager.instance.setVideoEncodeMode(densityArr[index], bitrateArr[index])
        // 发送上一个选中的索引
        densityChange.value = resolutionSelectedIndex
        resolutionSelectedIndex = index

        densityChange(index)
    }

    fun switchVideoStatus(enable: Boolean) {
        RtcManager.instance.enableLocalVideo(enable)
        //localVideoEnabled = !localVideoEnabled
    }

    fun switchVoiceStatus(enable: Boolean) {
        RtcManager.instance.enableLocalAudio(enable)
        //localVoiceEnabled = !localVoiceEnabled
    }

    fun switchEarMonitor() {
        val enableEarMonitor = RtcManager.instance.enableInEarMonitoring(!earMonitorEnabled)
        if (!enableEarMonitor) {
            earMonitorFail.value = Unit
            return
        }
        earMonitorEnabled = !earMonitorEnabled
        earMonitorIconSwitch.value = earMonitorEnabled
    }

    fun setRoomInfo(
        roomId: String,
        rtmToken: String,
        rtcToken: String,
        roomType: Int,
        isHost: Boolean,
        cdnUrl: String = "",
        hostId: String = ""
    ) {
        this.roomId = roomId
        this.rtcToken = rtcToken
        this.rtmToken = rtmToken
        this.roomType = roomType
        this.isHost = isHost
        this.cdnUrl = cdnUrl
        this.hostId = hostId
        if (isHost) {
            getMusicList()
        }
    }

    fun initSDK(ctx: Context) {
        RtcManager.instance.initRtc(ctx)
        RtcManager.instance.enableVideo()
        if (isHost) {
            if (roomType == 7) {
                RtcManager.instance.initStreamKit()
            }
            connectedUserInfoList.add(GuestUserInfo(userId, userAvatar, userNickname))
        }
        RtcManager.instance.registerListener(RtcEvent())
        RtmManager.instance.registerListener(RtmEvent())
    }

    fun switchCamera() {
        RtcManager.instance.switchCamera()
    }

    // guest
    fun applyLine() {
        RtmManager.instance.sendPeerMessage(
            hostId,
            "{\"cmd\": \"apply\", \"avatar\": \"$userAvatar\", \"userName\": \"$userNickname\"}"
        )
        isRequestingApply = true
    }

    fun cancelApply() {
        RtmManager.instance.sendPeerMessage(
            hostId,
            "{\"cmd\": \"cancelApply\"}"
        )
        isRequestingApply = false
    }

    open fun disconnection() {
        val index = getConnListIndex(userId)
        if (index != -1) {
            connectedUserInfoList.removeAt(index)
        }
        userLeave.value = RtcMember(userId, index)
    }

    // host
    fun rejectLine(uid: String) {
        RtmManager.instance.sendPeerMessage(
            uid,
            "{\"cmd\": \"rejectLine\"}"
        )

        val userIndex = getLinkedUserIndex(guestReqConnInfoList, uid)
        if (-1 == userIndex)
            return
        guestReqConnInfoList.removeAt(userIndex)
    }

    fun acceptLine(uid: String) {
        RtmManager.instance.sendPeerMessage(
            uid,
            "{\"cmd\": \"acceptLine\"}"
        )
    }

    fun deleteRoom() {
        if (isHost) viewModelScope.launch {
            ServerManager.instance.deleteRoom(roomId)
            RtmManager.instance.sendChannelMessage("{\"cmd\": \"exit\"}")
        }
    }

    private fun getMusicList() {
        viewModelScope.launch {
            ServerManager.instance.getMusicList(this).awaitResult {
                musicList = it.data
            }.onFailure {
            }
        }
    }

    fun toggleMusicPlayStatus() {
        when (musicStage) {
            RtcManager.MusicState.IDEA -> {
                if (musicList.size == 0) {
                    musicStateChange.value = RtcManager.MusicState.ERROR
                    musicStage = RtcManager.MusicState.IDEA
                } else {
                    RtcManager.instance.startAudioMixing(musicList[0].musicUrl)
                    musicStateChange.value = RtcManager.MusicState.PLAYING
                    musicStage = RtcManager.MusicState.PLAYING
                    RtcManager.instance.rtcEngine?.adjustAudioMixingVolume(40)
                    RtmManager.instance.setChannelAttribute(
                        roomId,
                        listOf(RtmChannelAttribute("musicState", "1"))
                    )
                }
            }
            RtcManager.MusicState.PLAYING -> {
                RtcManager.instance.parseAudioMixing()
                musicStateChange.value = RtcManager.MusicState.PAUSE
                musicStage = RtcManager.MusicState.PAUSE
                RtmManager.instance.setChannelAttribute(
                    roomId,
                    listOf(RtmChannelAttribute("musicState", "2"))
                )
            }
            RtcManager.MusicState.PAUSE -> {
                RtcManager.instance.resumeAudioMixing()
                musicStateChange.value = RtcManager.MusicState.PLAYING
                musicStage = RtcManager.MusicState.PLAYING
                RtmManager.instance.setChannelAttribute(
                    roomId,
                    listOf(RtmChannelAttribute("musicState", "1"))
                )
            }
            else -> {
                // do nothing.
            }
        }
    }

    // mode: true = topic, false = equally divided
    fun toggleLayoutMode(mode: Boolean) {
        RtmManager.instance.setChannelAttribute(
            roomId,
            listOf(RtmChannelAttribute("layout", if (mode) "1" else "2"))
        )
        onToggleLayoutMode(mode)
    }

    fun sendChatMessage(content: String) {
        RtmManager.instance.sendChannelMessage(
            "{\"cmd\": \"msg\", \"content\": \"$content\", \"userName\": \"$userNickname\"}"
        )
        onChatMessage.value = ChatMessageData(userNickname, content, true)
    }

    fun addSelfGuestInfo() {
        connectedUserInfoList.add(
            GuestUserInfo(
                userId,
                userAvatar,
                userNickname
            )
        )
    }

    fun switchSelfCameraStates(disable: Boolean) {
        val selfIndex = getConnListIndex(userId)
        if (selfIndex < 0) return

        val selfData = connectedUserInfoList[selfIndex]
        selfData.isCloseCamera = disable
        selfData.index = selfIndex
        cameraMicrophoneStateChange.value = selfData
    }

    fun switchSelfMikeStates(disable: Boolean) {
        val selfIndex = getConnListIndex(userId)
        if (selfIndex < 0) return

        val selfData = connectedUserInfoList[selfIndex]
        selfData.isMute = disable
        selfData.index = selfIndex
        cameraMicrophoneStateChange.value = selfData
    }

    private fun getUserInfo(uid: String, callback: (UserInfoBean) -> Unit) {
        viewModelScope.launch {
            ServerManager.instance.getUserInfo(uid, this).awaitResult(callback)
        }
    }

    protected fun mixLossRate(target: Int, value: Int): Int {
        return target or (value and LOSS_RATE_FILTER.shr(LOSS_RATE_OFFSET)).shl(LOSS_RATE_OFFSET)
    }
    //private fun mixLagging(target: Int, value: Int) = target or (value and LAGGING_FILTER)

    fun getLossRate(target: Int): Int {
        return (target and LOSS_RATE_FILTER).shr(LOSS_RATE_OFFSET)
    }

    fun getLagging(target: Int): Int {
        return target and LAGGING_FILTER
    }

    protected fun getConnListIndex(targetUid: String): Int {
        return getLinkedUserIndex(connectedUserInfoList, targetUid)
    }

    protected fun getLinkedUserIndex(list: LinkedList<GuestUserInfo>, targetUid: String): Int {
        var foundIndex = -1
        list.forEachIndexed { index, guestReqConn ->
            if (guestReqConn.uid == targetUid) {
                foundIndex = index
                return@forEachIndexed
            }
        }
        return foundIndex
    }

    private inner class RtcEvent : RtcListener() {

        override fun onJoinChannelSuccess(channel: String?, uid: String?, elapsed: Int) {
            joinChannelSuccess()
        }

        override fun onUserJoined(uid: String?, elapsed: Int) {
            onUserJoin(uid!!)
        }

        override fun onUserOffline(uid: String?, reason: Int) {
            if (uid == null) return

            //这里判断是不是主播 是的话直接退出
            if (!isHost && uid == hostId) {
                leaveCountDown.postValue(false)
                hostLost.postValue(Unit)
                return
            }

            val index = getConnListIndex(uid)
            if (index != -1) {
                connectedUserInfoList.removeAt(index)
            }
            userLeave.value = RtcMember(uid, index)
            userOffline(uid)
        }

        override fun onAudioVolumeIndication(
            speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
            totalVolume: Int
        ) {
        }

        override fun onRemoteAudioStateChanged(
            uid: String?,
            state: Int,
            reason: Int,
            elapsed: Int
        ) {
            if (uid == null) {
                return
            }

            val index = getConnListIndex(uid)
            if (index == -1) {
                return
            }
            when (reason) {
                Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED -> {
                    val value = connectedUserInfoList[index]
                    value.isMute = true
                    value.index = index
                    cameraMicrophoneStateChange.value = value
                }
                Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED -> {
                    val value = connectedUserInfoList[index]
                    value.isMute = false
                    value.index = index
                    cameraMicrophoneStateChange.value = value
                }
            }
        }

        override fun onRemoteVideoStateChanged(
            uid: String?,
            state: Int,
            reason: Int,
            elapsed: Int
        ) {
            if (uid == null) {
                return
            }

            val index = getConnListIndex(uid)
            if (index == -1) {
                return
            }
            when (reason) {
                Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED -> {
                    val value = connectedUserInfoList[index]
                    value.isCloseCamera = true
                    value.index = index
                    cameraMicrophoneStateChange.value = value
                }
                Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED -> {
                    val value = connectedUserInfoList[index]
                    value.isCloseCamera = false
                    value.index = index
                    cameraMicrophoneStateChange.value = value
                }
            }
        }

        override fun onLocalVideoStateChanged(state: Int, reason: Int) {
        }

        /* 当前通话统计回调。 该回调在通话中每两秒触发一次。 */
        override fun onRtcStats(rtcStats: IRtcEngineEventHandler.RtcStats?) {
            if (rtcStats == null)
                return

            val uploadLossRate = rtcStats.txPacketLossRate // 上行丢包
            val downloadLossRate = rtcStats.rxPacketLossRate // 下行丢包
            val lagging = rtcStats.gatewayRtt // 网络延迟

            /*  自定义方法，方便回调传输 */
            val uploadLagging = mixLossRate(lagging, uploadLossRate)
            val downloadLagging = mixLossRate(lagging, downloadLossRate)

            /* 回调方法 */
            onLagAndUploadLossRateChange.postValue(uploadLagging) // 上行丢包和延迟
            onLagAndDownloadLossRateChange.postValue(downloadLagging) // 下行丢包和延迟
        }

        override fun onRtmpStreamingStateChanged(url: String?, state: Int, errCode: Int) {
        }

        override fun onLocalAudioStateChanged(state: Int, reason: Int) {
        }

        override fun onTokenPrivilegeWillExpire() {
            onExpired()
        }

        override fun onWarning(code: Int) {
        }

        override fun onConnectionLost() {
        }

        //SDK没实现这个 ？？
        override fun onAudioMixingStateChanged(state: Int, errorCode: Int) {
            //Log.d("onAudioMix", "state=${state} reason=${errorCode}")
        }
    }

    private inner class RtmEvent : RtmListener() {

        override fun onConnectionStateChanged(var1: Int, var2: Int) {
            when (var1) {
                4 -> {//离线
                    isDisconnect = true
                    selfLost.postValue(true)
                }
                3 -> {
                    if (isDisconnect) { //恢复
                        isDisconnect = false
                        selfLost.postValue(false)
                        launch({
                            delay(1000)
                            reconnectedInternet()
                        })
                    }
                }
            }
        }

        override fun onJoinChannelSuccess(channelId: String?) {
            if (isHost) RtmManager.instance.setChannelAttribute(
                roomId, listOf(
                    RtmChannelAttribute("musicState", "0"),
                    RtmChannelAttribute("layout", "1")
                )
            )
        }

        override fun onAttributesUpdated(var1: MutableList<RtmChannelAttribute>?) {
            var1?.forEach { elem ->
                when (elem.key) {
                    "musicState" -> {
                        when (elem.value) {
                            "0" -> {
                                musicStateChange.value = RtcManager.MusicState.IDEA
                            }
                            "1" -> {
                                musicStateChange.value = RtcManager.MusicState.PLAYING
                            }
                            else -> {
                                musicStateChange.value = RtcManager.MusicState.PAUSE
                            }
                        }
                    }
                    "layout" -> {
                        onLayoutModeChange.value = elem.value == "1"
                    }
                }
            }
        }

        override fun onP2PMessageReceived(var1: RtmMessage?, var2: String?) {
            var1 ?: return
            val userId = var2 ?: return
            val jsonOBJ = JSONObject(var1.text)
            when (jsonOBJ.get("cmd")) {
                // host receive
                "apply" -> {
                    val avatar = jsonOBJ.getString("avatar") as String
                    val nickname = jsonOBJ.get("userName") as String

                    checkUserExists(userId) { exists ->
                        val userIndex = getLinkedUserIndex(guestReqConnInfoList, userId)
                        if (!exists) {
                            if (-1 != userIndex)
                                guestReqConnInfoList.removeAt(userIndex)
                            return@checkUserExists
                        }

                        if (-1 != userIndex)
                            return@checkUserExists

                        val reqValue = GuestUserInfo(userId, avatar, nickname)
                        onGuestReq.postValue(reqValue)
                        guestReqConnInfoList.add(reqValue)
                    }
                }
                "cancelApply" -> {
                    val findIndex = getLinkedUserIndex(guestReqConnInfoList, userId)
                    if (-1 == findIndex) {
                        return
                    }

                    val removedInfo = guestReqConnInfoList.removeAt(findIndex)
                    if (removedInfo.waitForJoinRTC) {
                        requesterLost.postValue(removedInfo.apply { index = findIndex })
                    }
                    onGuestReq.value = removedInfo.apply {
                        isRemove = true
                        index = findIndex
                    }
                }
                // guest receive
                "acceptLine" -> {
                    isVideoOnline = true
                    if (!isRequestingApply) {
                        return
                    }

                    messageAcceptLine()
                    isRequestingApply = false
                }
                "rejectLine" -> {
                    isVideoOnline = false
                    messageRejectLine()
                    isRequestingApply = false
                }
            }
            /*
            申请连麦：cmd ：apply
            取消申请：cmd:cancelApply
            同意：cmd:acceptLine
            拒绝：cmd:rejectLine
             */
        }

        override fun onChannelMessageReceived(var1: RtmMessage?, var2: RtmChannelMember?) {
            var1?.let {
                val jsonOBJ = JSONObject(it.text)
                when (jsonOBJ.getString("cmd")) {
                    "msg" -> {
                        val content = jsonOBJ.getString("content") ?: ""
                        val fromNickname = jsonOBJ.getString("userName") ?: "NULL"
                        if (content == "") return

                        onChatMessage.value = ChatMessageData(fromNickname, content, false)
                    }
                    "exit" -> {
                        hostLost.postValue(Unit)
                    }
                }
            }
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: String?) {
        }

        override fun onMessageReceived(var1: RtmMessage?, var2: RtmChannelMember?) {
        }

        override fun onPeersOnlineStatusChanged(var1: MutableMap<String, Int>?) {
            if (var1 == null) return
            leaveCountDown.postValue(var1[hostId] != 0) //1 = 主播中途离线
        }

        override fun onMemberLeft(var1: RtmChannelMember?) {
            if (var1 == null) return
            if (!isHost && var1.userId == hostId) {
                leaveCountDown.postValue(true)
                return
            }

            // 判断等待列表里是否有此id, 如果有则通知View删除此item
            val reqIndex = getLinkedUserIndex(guestReqConnInfoList, var1.userId)
            if (-1 != reqIndex) {
                onGuestReq.postValue(guestReqConnInfoList[reqIndex].apply {
                    isRemove = true
                    index = reqIndex
                })
            }

            // 检测是否已同意此ID上麦，是则通知View计数器-1
            val findIndex = getConnListIndex(var1.userId)
            // 这里判断是否已经调用UserJoined, 是则无需处理，交由joined逻辑处理
            if (-1 == findIndex || connectedUserInfoList[findIndex].waitForJoinRTC) {
                if (-1 != findIndex) {
                    requesterLost.postValue(connectedUserInfoList[findIndex].apply {
                        index = findIndex
                    })
                    connectedUserInfoList.removeAt(findIndex)
                }
                return
            }

            // 这里说明用户已经成功调用userJoined
            // 由于等待UserLeave还需一会延迟，故这里先通知View层删除断线ID
            connectedUserInfoList.removeAt(findIndex)
            userLeave.postValue(RtcMember(var1.userId, findIndex))
        }

        override fun onMemberJoined(var1: RtmChannelMember?) {
            if (var1 == null) return
            if (!isHost && var1.userId == hostId) {
                leaveCountDown.postValue(false)
            }
        }
    }
}