package io.anyrtc.videolive.ui.activity


import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.kongzue.dialog.v3.MessageDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.databinding.ActivityHostBinding
import io.anyrtc.videolive.databinding.RtcHostMenuSheetBinding
import io.anyrtc.videolive.databinding.RtcHostQueueSheetBinding
import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.ui.fragment.InputDialogFragment
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.Interval
import io.anyrtc.videolive.utils.toast
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewBuilderImpl
import io.anyrtc.videolive.vm.RTCLiveVM
import org.ar.rtc.Constants
import org.ar.rtc.RtcEngine
import org.ar.rtc.video.VideoCanvas
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * RTC实时直播模式的主播端
 */
class RTCHostActivity : LiveBroadcastBaseActivity() {

    private val binding by lazy { ActivityHostBinding.inflate(layoutInflater) }
    private val liveVM: RTCLiveVM by viewModels()
    private val musicAnimator: ObjectAnimator by lazy {
        ObjectAnimator.ofFloat(binding.iconMusic, "rotation", 0f, 360f).apply {
            duration = 1000
            repeatCount = -1
        }
    }

    private var waitingSwitchVideoLayoutMode = false
    private val sheetDialog: BottomSheetDialog by lazy {
        BottomSheetDialog(this).apply {
            setOnDismissListener {
                if (!waitingSwitchVideoLayoutMode)
                    return@setOnDismissListener

                binding.rlHostView.toggleLayoutMode()
                liveVM.toggleLayoutMode(binding.rlHostView.getVideoLayoutMode())
                waitingSwitchVideoLayoutMode = false
            }
        }
    }

    private val rtcHostVoiceSheetBinding: RtcHostQueueSheetBinding by lazy {
        initVoiceView()
    }

    private val hostMenuSheetBinding: RtcHostMenuSheetBinding by lazy {
        initMenuView()
    }

    private val waitingQueueAdapter by lazy {
        HandsUpListAdapter()
    }

    private val chatAdapter: ChatMessageListAdapter = ChatMessageListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val rtmToken = intent.getStringExtra(Constans.RTM_TOKEN)
        val rtcToken = intent.getStringExtra(Constans.RTC_TOKEN)
        val roomType = intent.getIntExtra(Constans.ROOM_TYPE, 0)
        val roomId = intent.getStringExtra(Constans.ROOM_ID)
        if (
            rtmToken.isNullOrEmpty() || rtcToken.isNullOrEmpty() ||
            roomId.isNullOrEmpty() || (roomType == 0)
        ) {
            toast("参数异常")
            finish()
            return
        }

        liveVM.setRoomInfo(roomId, rtmToken, rtcToken, roomType, true)
        liveVM.initSDK(this)

        val videoView = RtcEngine.CreateRendererView(this)
        addVideoView(binding.rlHostView, videoView, setupLocalVideo = true)
        //RtcManager.instance.rtcEngine?.setupLocalVideo(VideoCanvas(videoView))

        liveVM.join()

        liveVM.userJoin.observe(this) {
            val userVideoView = RtcEngine.CreateRendererView(this)
            RtcManager.instance.rtcEngine?.setupRemoteVideo(
                VideoCanvas(userVideoView, Constants.RENDER_MODE_HIDDEN, it.userId)
            )
            addVideoView(binding.rlHostView, userVideoView)
        }
        liveVM.userLeave.observe(this) {
            //remove video view
            binding.rlHostView.removeVideoView(it.userIndex)
            if (waitingQueueAdapter.acceptNum > 0 && it.userIndex >= 0) {
                waitingQueueAdapter.acceptNum--
            }
        }
        liveVM.requesterLost.observe(this) {
            if (waitingQueueAdapter.acceptNum > 0) {
                waitingQueueAdapter.acceptNum--
                if (it.waitForJoinRTC)
                    binding.rlHostView.removeVideoView(it.index)
            }
        }

        /*liveVM.densityChange.observe(this) { beforeIndex ->
            val view = when (beforeIndex) {
                0 -> hostMenuSheetBinding.normalDensity
                1 -> hostMenuSheetBinding.highDensity
                else -> hostMenuSheetBinding.ultraDensity
            }
            view.setBackgroundResource(R.drawable.checkbox_resolution_unchecked)
            view.setTextColor(Color.parseColor("#5A5A67"))
        }*/

        liveVM.onChatMessage.observe(this) {
            chatAdapter.addData(it)
        }

        liveVM.onGuestReq.observe(this) {
            if (it.isRemove) {
                waitingQueueAdapter.removeAt(it.index)

                binding.groupNum.text = waitingQueueAdapter.data.size.toString()
                if (waitingQueueAdapter.data.size == 0 && binding.groupNum.visibility == View.VISIBLE) {
                    binding.groupNum.visibility = View.GONE
                }
            } else {
                waitingQueueAdapter.addData(it)
                waitingQueueAdapter.notifyDataSetChanged()

                binding.groupNum.text = waitingQueueAdapter.data.size.toString()
                if (binding.groupNum.visibility == GONE) {
                    binding.groupNum.visibility = VISIBLE
                }
                showTips(0, String.format("%s 请求连麦", it.nickname))
            }

            rtcHostVoiceSheetBinding.run {
                val queueSize = waitingQueueAdapter.data.size
                title.text = String.format("排麦队列 %d", queueSize)
                if (queueSize == 0 && empty.visibility == GONE) {
                    empty.visibility = VISIBLE
                    queueRecycle.visibility = GONE
                } else if (queueSize > 0 && empty.visibility == VISIBLE) {
                    empty.visibility = GONE
                    queueRecycle.visibility = VISIBLE
                }
            }
        }

        liveVM.musicStateChange.observe(this) {
            when (it) {
                RtcManager.MusicState.IDEA -> {
                    binding.musicStatus.text = "点击播放音乐"
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
                    // TODO: 2021/6/21 提示播放出错
                }
                else -> {
                    // do nothing.
                }
            }
        }

        liveVM.onLayoutModeChange.observe(this) {
            DefaultVideoViewBuilderImpl.changeLayoutMode(it, binding.rlHostView)
            binding.rlHostView.setVideoLayoutMode(it)
        }
        liveVM.earMonitorFail.observe(this) {
            showTips(tipsEarMonitorFail)
        }
        liveVM.earMonitorIconSwitch.observe(this) {
            hostMenuSheetBinding.run {
                ear.setImageResource(
                    if (it) R.drawable.host_icon_ear_enabled
                    else R.drawable.host_icon_ear_disabled
                )
                earTitle.setTextColor(
                    if (it) Color.parseColor("#294BFF")
                    else Color.parseColor("#393939")
                )
            }
        }

        liveVM.onLagAndUploadLossRateChange.observe(this) {
            val lossRate = liveVM.getLossRate(it)
            val lagging = liveVM.getLagging(it)

            binding.lossRate.text = String.format("丢包率%d%%", lossRate)
            binding.lag.text = String.format("延时: %dms", lagging)
        }
        registerInternetObserve(liveVM, binding.rlHostView)

        Interval(0, 10, TimeUnit.MINUTES, 1).life(this).finish {
            showTips(R.drawable.tips_icon_warning, "体验时间已到")
            finish()
        }.start()

        initWidget()
    }

    private fun initWidget() {
        binding.run {
            iconChat.setOnClickListener {
                InputDialogFragment().show(supportFragmentManager) {
                    liveVM.sendChatMessage(it)
                }
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

            iconGroup.setOnClickListener {
                sheetDialog.setContentView(rtcHostVoiceSheetBinding.root)
                sheetDialog.show()
            }
            groupNum.text = "0"
            groupNum.visibility = GONE

            iconMenu.setOnClickListener {
                sheetDialog.setContentView(hostMenuSheetBinding.root)
                sheetDialog.show()
            }

            iconLeave.setOnClickListener {
                onBackPressed()
            }
            musicBg.setOnClickListener {
                liveVM.toggleMusicPlayStatus()
            }

            messages.run {
                layoutManager =
                    LinearLayoutManager(root.context, LinearLayoutManager.VERTICAL, false)
                adapter = chatAdapter
            }
        }
    }

    override fun lostInternet() {
        liveVM.deleteRoom()
        finish()
    }

    override fun pauseCameraMike() {
        if (!binding.iconVideo.isChecked)
            liveVM.switchVideoStatus(false)
        if (!binding.iconVoice.isChecked)
            liveVM.switchVoiceStatus(false)
    }

    override fun resumeCameraMike() {
        if (!binding.iconVideo.isChecked)
            liveVM.switchVideoStatus(true)
        if (!binding.iconVoice.isChecked)
            liveVM.switchVoiceStatus(true)
    }

    private fun initVoiceView(): RtcHostQueueSheetBinding {
        waitingQueueAdapter.addChildClickViewIds(R.id.agree, R.id.reject)
        waitingQueueAdapter.setOnItemChildClickListener { _, view, position ->
            when (view.id) {
                R.id.agree -> {
                    if (waitingQueueAdapter.acceptNum >= 3) {
                        showTips(tipsConnectionLimited)
                        return@setOnItemChildClickListener
                    }
                    val acceptedUserInfo = waitingQueueAdapter.data[position]
                    liveVM.acceptLine(acceptedUserInfo.uid)
                    if (waitingQueueAdapter.data.size == 1) {
                        sheetDialog.dismiss()
                    }
                    waitingQueueAdapter.acceptNum++
                }
                R.id.reject -> {
                    liveVM.rejectLine(waitingQueueAdapter.data[position].uid)
                }
            }
            waitingQueueAdapter.removeAt(position)

            val queueSize = waitingQueueAdapter.data.size
            binding.groupNum.text = queueSize.toString()
            rtcHostVoiceSheetBinding.title.text = String.format("排麦队列 %d", queueSize)
            if (queueSize == 0) {
                binding.groupNum.visibility = View.GONE
            }
        }

        val hostBinding = RtcHostQueueSheetBinding.inflate(layoutInflater)
        hostBinding.run {
            dismiss.setOnClickListener { sheetDialog.dismiss() }
            queueRecycle.layoutManager = LinearLayoutManager(
                binding.root.context, LinearLayoutManager.VERTICAL, false
            )
            queueRecycle.adapter = waitingQueueAdapter
        }
        return hostBinding
    }

    private fun initMenuView(): RtcHostMenuSheetBinding {
        val hostBinding = RtcHostMenuSheetBinding.inflate(layoutInflater)
        hostBinding.dismiss.setOnClickListener { sheetDialog.dismiss() }
        /*val resolutionClick = View.OnClickListener {
            it.setBackgroundResource(R.drawable.checkbox_resolution_checked)
            (it as TextView).setTextColor(Color.parseColor("#314BFF"))
            when (it.id) {
                hostBinding.normalDensity.id -> liveVM.onDensityChange(0)
                hostBinding.highDensity.id -> liveVM.onDensityChange(1)
                hostBinding.ultraDensity.id -> liveVM.onDensityChange(2)
            }
        }
        hostBinding.normalDensity.setOnClickListener(resolutionClick)
        hostBinding.highDensity.setOnClickListener(resolutionClick)
        hostBinding.ultraDensity.setOnClickListener(resolutionClick)*/

        hostBinding.cameraClick.setOnClickListener {
            liveVM.switchCamera()
        }
        hostBinding.earClick.setOnClickListener {
            liveVM.switchEarMonitor()
        }
        hostBinding.relayoutClick.setOnClickListener {
            if (binding.rlHostView.childCount > 1) {
                if (binding.rlHostView.getVideoLayoutMode()) { // true = topic mode
                    hostBinding.relayoutIcon.setImageResource(R.drawable.host_icon_relayout_topic)
                    hostBinding.relayoutTitle.text = resources.getText(R.string.relayout_topic)
                } else {
                    hostBinding.relayoutIcon.setImageResource(R.drawable.host_icon_relayout_grid)
                    hostBinding.relayoutTitle.text = resources.getText(R.string.relayout_grid)
                }
            }

            waitingSwitchVideoLayoutMode = true
            sheetDialog.dismiss()
        }

        return hostBinding
    }

    override fun onBackPressed() {
        MessageDialog.show(this, "结束直播", "是否结束直播").setOkButton("确认").setCancelButton("取消")
            .setOnOkButtonClickListener { _, _ ->
                liveVM.deleteRoom()
                super.onBackPressed()
                true
            }
    }
}