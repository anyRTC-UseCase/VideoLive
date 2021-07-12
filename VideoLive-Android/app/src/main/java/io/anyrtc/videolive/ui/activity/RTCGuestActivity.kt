package io.anyrtc.videolive.ui.activity

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.kongzue.dialog.v3.MessageDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.databinding.ActivityGuestBinding
import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.ui.fragment.InputDialogFragment
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.toast
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewBuilderImpl
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewParent
import io.anyrtc.videolive.vm.RTCLiveVM
import org.ar.rtc.Constants
import org.ar.rtc.RtcEngine
import org.ar.rtc.video.VideoCanvas
import java.util.*

class RTCGuestActivity : LiveBroadcastBaseActivity() {

    private val binding by lazy { ActivityGuestBinding.inflate(layoutInflater) }
    private val liveVM: RTCLiveVM by viewModels()
    private val musicAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.iconMusic, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = -1
        }
    }

    private var waitingAccept = false
    private var firstVideoParent: DefaultVideoViewParent? = null

    private val chatAdapter: ChatMessageListAdapter = ChatMessageListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val rtmToken = intent.getStringExtra(Constans.RTM_TOKEN)
        val rtcToken = intent.getStringExtra(Constans.RTC_TOKEN)
        val roomType = intent.getIntExtra(Constans.ROOM_TYPE, 0)
        val roomId = intent.getStringExtra(Constans.ROOM_ID)
        val hostId = intent.getStringExtra(Constans.HOST_ID)
        if (
            rtmToken.isNullOrEmpty() || rtcToken.isNullOrEmpty()
            || roomId.isNullOrEmpty() || (roomType == 0)
            || hostId == null
        ) {
            toast("参数异常")
            finish()
            return
        }

        liveVM.setRoomInfo(roomId, rtmToken, rtcToken, roomType, false, hostId = hostId)
        liveVM.initSDK(this)
        liveVM.join()

        initFirstTextureView()

        liveVM.userJoin.observe(this, {
            val videoView = RtcEngine.CreateRendererView(this)
            RtcManager.instance.rtcEngine?.setupRemoteVideo(
                VideoCanvas(
                    videoView,
                    Constants.RENDER_MODE_HIDDEN,
                    it.userId
                )
            )

            if (it.userId == hostId) firstVideoParent?.let { layoutParent ->
                val frame = layoutParent.view.findViewById<ViewGroup>(R.id.video_parent)
                frame.removeAllViews()
                frame.addView(videoView)
            } else addVideoView(
                binding.rlHostView,
                videoView
            )
        })
        liveVM.userLeave.observe(this) {
            binding.rlHostView.removeVideoView(it.userIndex)
        }
        liveVM.requesterLost.observe(this) {
            if (it.waitForJoinRTC)
                binding.rlHostView.removeVideoView(it.index)
        }
        liveVM.musicStateChange.observe(this, {
            when (it) {
                RtcManager.MusicState.IDEA -> {
                    binding.musicStatus.text = "主播未播放音乐"
                    musicAnimator.cancel()
                }
                RtcManager.MusicState.PLAYING -> {
                    binding.musicStatus.text = "音乐播放中"
                    musicAnimator.start()
                }
                RtcManager.MusicState.PAUSE -> {
                    binding.musicStatus.text = "音乐已暂停"
                    musicAnimator.pause()
                }
                RtcManager.MusicState.ERROR -> {
                    toast("获取主播音乐信息失败")
                }
                else -> {
                    // do nothing.
                }
            }
        })

        liveVM.onChatMessage.observe(this) {
            chatAdapter.addData(it)
        }

        // apply callback
        liveVM.applyRequestResponse.observe(this) {
            if (it) {
                val clientTexture = RtcEngine.CreateRendererView(this)
                addVideoView(
                    binding.rlHostView,
                    clientTexture,
                    yourself = true,
                    setupLocalVideo = true
                )
                binding.apply.text = "下麦"
                binding.connModeGroup.visibility = View.VISIBLE
                liveVM.addSelfGuestInfo()

                // 改为监听上行
                ContextCompat.getColor(this, R.color.internet_state_upload_color).let { color ->
                    binding.lossRate.setTextColor(color)
                    binding.lag.setTextColor(color)
                    binding.lossRate.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.host_icon_upload, 0, 0, 0
                    )
                }
                liveVM.onLagAndUploadLossRateChange.observe(this@RTCGuestActivity) { state ->
                    val lossRate = liveVM.getLossRate(state)
                    val lagging = liveVM.getLagging(state)

                    binding.lossRate.text = String.format("丢包率%d%%", lossRate)
                    binding.lag.text = String.format("延时: %dms", lagging)
                }
                liveVM.onLagAndDownloadLossRateChange.removeObservers(this@RTCGuestActivity)
            } else {
                binding.apply.text = "上麦"
                toast("主播拒绝了您的申请")
            }
            waitingAccept = false
        }

        liveVM.onLayoutModeChange.observe(this) {
            DefaultVideoViewBuilderImpl.changeLayoutMode(it, binding.rlHostView)
            binding.rlHostView.setVideoLayoutMode(it)
        }
        liveVM.onLagAndDownloadLossRateChange.observe(this) {
            val lossRate = liveVM.getLossRate(it)
            val lagging = liveVM.getLagging(it)

            binding.lossRate.text = String.format("丢包率%d%%", lossRate)
            binding.lag.text = String.format("延时: %dms", lagging)
        }
        registerInternetObserve(liveVM, binding.rlHostView)

        initWidget()
    }

    private fun initWidget() {
        binding.run {
            iconChat.setOnClickListener {
                InputDialogFragment().show(supportFragmentManager) {
                    liveVM.sendChatMessage(it)
                }
            }
            iconSwitch.setOnClickListener {
                liveVM.switchCamera()
            }
            iconVideo.setOnClickListener {
                liveVM.switchVideoStatus(!iconVideo.isChecked)
                liveVM.switchSelfCameraStates(iconVideo.isChecked)
                showTips(if (!iconVideo.isChecked) tipsCameraEnabled else tipsCameraDisabled)
            }
            iconVoice.setOnClickListener {
                liveVM.switchVoiceStatus(!iconVoice.isChecked)
                liveVM.switchSelfMikeStates(iconVoice.isChecked)
                showTips(if (!iconVoice.isChecked) tipsMikeEnabled else tipsMikeDisabled)
            }
            apply.setOnClickListener {
                if (liveVM.isVideoOnline) {
                    liveVM.disconnection()
                    binding.connModeGroup.visibility = View.GONE
                    liveVM.isVideoOnline = false
                    apply.text = "上麦"

                    /* 改为监听下行 */
                    ContextCompat.getColor(it.context, R.color.internet_state_download_color)
                        .let { color ->
                            binding.lossRate.setTextColor(color)
                            binding.lag.setTextColor(color)
                            binding.lossRate.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.host_icon_download, 0, 0, 0
                            )
                        }
                    liveVM.onLagAndDownloadLossRateChange.observe(this@RTCGuestActivity) { state ->
                        val lossRate = liveVM.getLossRate(state)
                        val lagging = liveVM.getLagging(state)

                        binding.lossRate.text = String.format("丢包率%d%%", lossRate)
                        binding.lag.text = String.format("延时: %dms", lagging)
                    }
                    liveVM.onLagAndUploadLossRateChange.removeObservers(this@RTCGuestActivity)

                    return@setOnClickListener
                }

                if (waitingAccept) {
                    liveVM.cancelApply()
                    apply.text = "上麦"
                } else {
                    liveVM.applyLine()
                    apply.text = "取消申请"
                }

                waitingAccept = !waitingAccept
            }
            iconFinish.setOnClickListener {
                onBackPressed()
            }

            messages.run {
                layoutManager =
                    LinearLayoutManager(root.context, LinearLayoutManager.VERTICAL, false)
                adapter = chatAdapter
            }
        }
    }

    private fun initFirstTextureView() {
        firstVideoParent = addVideoView(binding.rlHostView, TextureView(this))
    }

    override fun onBackPressed() {
        if (liveVM.isVideoOnline) {
            MessageDialog.show(this, "您在麦上", "是否确认退出直播间")
                .setOkButton("确认").setCancelButton("取消")
                .setOnOkButtonClickListener { _, _ ->
                    liveVM.disconnection()
                    super.onBackPressed()
                    true
                }
            return
        }
        liveVM.disconnection()
        super.onBackPressed()
    }

    override fun lostInternet() {
        liveVM.disconnection()
        finish()
    }

    override fun pauseCameraMike() {
        if (!liveVM.isVideoOnline)
            return
        if (!binding.iconVideo.isChecked)
            liveVM.switchVideoStatus(false)
        if (!binding.iconVoice.isChecked)
            liveVM.switchVoiceStatus(false)
    }

    override fun resumeCameraMike() {
        if (!liveVM.isVideoOnline)
            return
        if (!binding.iconVideo.isChecked)
            liveVM.switchVideoStatus(true)
        if (!binding.iconVoice.isChecked)
            liveVM.switchVoiceStatus(true)
    }
}