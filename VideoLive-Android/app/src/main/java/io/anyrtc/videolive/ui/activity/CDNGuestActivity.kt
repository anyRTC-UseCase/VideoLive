package io.anyrtc.videolive.ui.activity

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kongzue.dialog.v3.MessageDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.ServerManager
import io.anyrtc.videolive.databinding.ActivityGuestBinding
import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.ui.fragment.InputDialogFragment
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.toast
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewBuilderImpl
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewParent
import io.anyrtc.videolive.vm.CDNLiveVM
import kotlinx.coroutines.launch
import org.ar.rtc.RtcEngine
import org.webrtc.RendererCommon
import org.webrtc.TextureViewRenderer
import rxhttp.awaitResult
import java.util.*

class CDNGuestActivity : LiveBroadcastBaseActivity() {

    private val binding by lazy { ActivityGuestBinding.inflate(layoutInflater) }
    private val liveVM: CDNLiveVM by viewModels()
    private val musicAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.iconMusic, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = -1
        }
    }

    //private var waitingAccept = false

    private var loadingViewAnim: Animation? = null
    private var firstVideoParent: DefaultVideoViewParent? = null
    private lateinit var cdnTextureView: TextureView

    private val chatAdapter: ChatMessageListAdapter = ChatMessageListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val rtmToken = intent.getStringExtra(Constans.RTM_TOKEN)
        val rtcToken = intent.getStringExtra(Constans.RTC_TOKEN)
        val roomType = intent.getIntExtra(Constans.ROOM_TYPE, 0)
        val roomId = intent.getStringExtra(Constans.ROOM_ID)
        val hostId = intent.getStringExtra(Constans.HOST_ID)
        val pullUrl = intent.getStringExtra(Constans.PULL_URL)
        if (
            rtmToken.isNullOrEmpty() || rtcToken.isNullOrEmpty()
            || roomId.isNullOrEmpty() || (roomType == 0)
            || hostId.isNullOrEmpty() || pullUrl.isNullOrEmpty()
        ) {
            toast("参数异常")
            finish()
            return
        }

        liveVM.setRoomInfo(
            roomId,
            rtmToken,
            rtcToken,
            roomType,
            isHost = false,
            cdnUrl = pullUrl,
            hostId = hostId
        )
        liveVM.initSDK(this)
        liveVM.joinRtm()

        cdnTextureView = RtcEngine.CreateRendererView(this@CDNGuestActivity)
        (cdnTextureView as TextureViewRenderer).setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        liveVM.loadCDNHostStream(cdnTextureView)
        firstVideoParent =
            addVideoView(binding.rlHostView, cdnTextureView)

        liveVM.userJoin.observe(this) {
            val videoView = RtcEngine.CreateRendererView(this)
            liveVM.rtcSetupRemoteVideo(videoView, it.userId)

            if (it.userId == hostId) {
                firstVideoParent?.let { layoutParent ->
                    val frame = layoutParent.view.findViewById<ViewGroup>(R.id.video_parent)
                    frame.removeAllViews()
                    frame.addView(videoView)
                }
                return@observe
            }
            addVideoView(binding.rlHostView, videoView)
        }
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
                //RtcManager.instance.rtcEngine?.setupLocalVideo(VideoCanvas(clientTexture))
                addVideoView(
                    binding.rlHostView,
                    clientTexture,
                    yourself = true,
                    setupLocalVideo = true
                )

                /* 显示上行数据 */
                registerUploadState()
                binding.internetState.visibility = View.VISIBLE

                binding.apply.text = "下麦"
                binding.connModeGroup.visibility = View.VISIBLE
                liveVM.addSelfGuestInfo()
            } else {
                binding.apply.text = "上麦"
                toast("主播拒绝了您的申请")
            }
        }

        liveVM.streamLoadStates.observe(this) {
            if (it) {
                if (loadingViewAnim == null) {
                    loadingViewAnim =
                        AnimationUtils.loadAnimation(this, R.anim.loading_anim).also { anim ->
                            binding.loadingIcon.startAnimation(anim)
                        }
                    binding.loadingGroup.visibility = View.VISIBLE
                }
            } else {
                binding.loadingGroup.visibility = View.GONE
                binding.loadingIcon.clearAnimation()
                loadingViewAnim = null
            }
        }

        liveVM.onLayoutModeChange.observe(this) {
            DefaultVideoViewBuilderImpl.changeLayoutMode(it, binding.rlHostView)
            binding.rlHostView.setVideoLayoutMode(it)
        }

        binding.internetState.visibility = View.GONE
        binding.lossRate.setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.host_icon_upload, 0, 0, 0
        )
        ContextCompat.getColor(this, R.color.internet_state_upload_color).let { color ->
            binding.lossRate.setTextColor(color)
            binding.lag.setTextColor(color)
        }
        registerInternetObserve(liveVM, binding.rlHostView)

        initWidget()
    }

    private fun initWidget() {
        binding.run {
            /* 默认显示加载中 */
            loadingGroup.visibility = View.VISIBLE

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
                if (loadingGroup.visibility == View.VISIBLE) {
                    return@setOnClickListener
                }
                if (liveVM.isVideoOnline) {
                    liveVM.disconnection()
                    binding.connModeGroup.visibility = View.GONE
                    liveVM.isVideoOnline = false
                    apply.text = "上麦"

                    firstVideoParent?.let {
                        val videoParent = it.view.findViewById<RelativeLayout>(R.id.video_parent)
                        liveVM.resetHostViewState()
                        binding.run {
                            iconVideo.isChecked = false
                            iconVoice.isChecked = false
                        }

                        videoParent.removeAllViews()
                        videoParent.addView(cdnTextureView, RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            addRule(RelativeLayout.CENTER_IN_PARENT)
                        })

                        val firstChild = binding.rlHostView.getChildAt(0)
                        binding.rlHostView.removeAllViews()
                        binding.rlHostView.addVideoView(firstChild)
                    }

                    /* 隐藏上行数据 */
                    unregisterUploadState()
                    binding.internetState.visibility = View.GONE

                    return@setOnClickListener
                }

                if (liveVM.isRequestingApply) {
                    liveVM.cancelApply()
                    apply.text = "上麦"
                } else {
                    liveVM.applyLine()
                    apply.text = "取消申请"
                }
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

    private fun registerUploadState() {
        liveVM.onLagAndUploadLossRateChange.observe(this) {
            val lossRate = liveVM.getLossRate(it)
            val lagging = liveVM.getLagging(it)

            binding.lossRate.text = String.format("丢包率%d%%", lossRate)
            binding.lag.text = String.format("延时: %dms", lagging)
        }
    }

    private fun unregisterUploadState() {
        liveVM.onLagAndUploadLossRateChange.removeObservers(this)
    }

    override fun onBackPressed() {
        if (liveVM.isVideoOnline) {
            MessageDialog.show(this@CDNGuestActivity, "您在麦上", "是否确认退出直播间")
                .setOkButton("确认").setCancelButton("取消")
                .setOnOkButtonClickListener { _, _ ->
                    liveVM.disconnection()
                    leaveChannel()
                    super.onBackPressed()
                    true
                }
            return
        }
        leaveChannel()
        super.onBackPressed()
    }

    override fun lostInternet() {
        if (liveVM.isVideoOnline)
            liveVM.disconnection()

        leaveChannel()
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

    private fun leaveChannel() {
        lifecycleScope.launch {
            ServerManager.instance.leaveRoom(
                intent.getStringExtra(Constans.HOST_ID) ?: "",
                this
            ).awaitResult()
        }
    }
}