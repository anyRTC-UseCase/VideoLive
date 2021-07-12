package io.anyrtc.videolive.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.TextureView
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gyf.immersionbar.ImmersionBar
import com.kongzue.dialog.v3.TipDialog
import com.kongzue.dialog.v3.WaitDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.sdk.RtcManager
import io.anyrtc.videolive.utils.Interval
import io.anyrtc.videolive.utils.ScreenUtils
import io.anyrtc.videolive.utils.launch
import io.anyrtc.videolive.view.AnyVideosLayout
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewBuilderImpl
import io.anyrtc.videolive.view.videobuilder.DefaultVideoViewParent
import io.anyrtc.videolive.vm.BaseLiveVM
import kotlinx.coroutines.delay
import org.ar.rtc.video.VideoCanvas
import java.util.concurrent.TimeUnit

abstract class LiveBroadcastBaseActivity : AppCompatActivity() {

    protected val tipsCameraEnabled = 0
    protected val tipsCameraDisabled = 1
    protected val tipsMikeEnabled = 2
    protected val tipsMikeDisabled = 3
    protected val tipsEarMonitorFail = 4
    protected val tipsConnectionLimited = 5

    private var loadingDialog: TipDialog? = null
    private var hostInterval: Interval? = null
    private var selfInterval: Interval? = null

    private val tipsIconArr = arrayOf(
        R.drawable.tips_icon_camera_enabled,
        R.drawable.tips_icon_camera_disabled,
        R.drawable.tips_icon_mike_enabled,
        R.drawable.tips_icon_mike_disabled,
        R.drawable.tips_icon_headset,
        R.drawable.tips_icon_warning
    )
    private val tipsTitleArr = arrayOf(
        R.string.camera_enabled,
        R.string.camera_disabled,
        R.string.mike_enabled,
        R.string.mike_disabled,
        R.string.plug_wired_headset,
        R.string.conn_limited
    )

    override fun onDestroy() {
        ScreenUtils.resetScreen(this)
        refreshRoomList()
        super.onDestroy()
    }

    protected fun showTips(defType: Int) {
        showTips(tipsIconArr[defType], resources.getString(tipsTitleArr[defType]))
    }

    @SuppressLint("InflateParams")
    protected fun showTips(drawableRes: Int, content: String) {
        val mToast = Toast(this)
        val view = layoutInflater.inflate(R.layout.toast_tips, null)
        val textView = view.findViewById<TextView>(R.id.tips)
        textView.setCompoundDrawablesWithIntrinsicBounds(drawableRes, 0, 0, 0)
        textView.text = content

        mToast.duration = Toast.LENGTH_SHORT
        mToast.setGravity(Gravity.CENTER, 0, 0)
        mToast.view = view
        mToast.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ScreenUtils.adapterScreen(this, 375, false)
        ImmersionBar.with(this).init()
    }

    protected abstract fun lostInternet()

    private fun refreshRoomList() {
        val intent = intent.also { it.putExtra("refresh", 1) }
        setResult(1, intent)
    }

    protected fun registerInternetObserve(vm: BaseLiveVM, rlHostView: AnyVideosLayout) {
        vm.selfLost.observe(this) {
            if (loadingDialog == null) {
                loadingDialog = WaitDialog.show(this, "连接中")
                loadingDialog!!.cancelable = false
            }

            if (it) {
                loadingDialog?.showNoAutoDismiss()

                if (selfInterval == null) {
                    selfInterval = Interval(0, 12, TimeUnit.SECONDS, 1).life(this).finish {
                        hostInterval?.stop()
                        selfInterval?.stop()
                        hostInterval = null
                        selfInterval = null

                        showTips(R.drawable.tips_icon_warning, "连接已断开")
                        lostInternet()
                    }
                    selfInterval!!.start()
                }
            } else {
                loadingDialog?.doDismiss()
                loadingDialog = null

                selfInterval?.stop()
                selfInterval = null
            }
        }

        if (!vm.isHost) {
            vm.leaveCountDown.observe(this) {
                if (it) {
                    hostInterval = Interval(0, 12, TimeUnit.SECONDS, 1).life(this).finish {
                        showTips(R.drawable.tips_icon_warning, "主播已离开房间")
                        lostInternet()
                    }
                    hostInterval!!.start()
                } else {
                    hostInterval?.stop()
                    hostInterval = null
                }
            }

            vm.hostLost.observe(this) {
                showTips(R.drawable.tips_icon_warning, "主播已离开房间")
                lostInternet()
            }
        }

        vm.cameraMicrophoneStateChange.observe(this) {
            if (it.index < 0 || it.index >= rlHostView.childCount) {
                // throw IndexOutOfRange
                return@observe
            }

            val childView = DefaultVideoViewParent(rlHostView.getChildAt(it.index), it.index)
            DefaultVideoViewBuilderImpl.setVideoViewStates(childView, it)
        }
    }

    @SuppressLint("InflateParams")
    protected fun addVideoView(
        rlHostView: AnyVideosLayout,
        textureView: TextureView,
        yourself: Boolean = false,
        setupLocalVideo: Boolean = false
    ): DefaultVideoViewParent {
        val layoutParent = DefaultVideoViewBuilderImpl.buildVideoViewParent(
            DefaultVideoViewParent(
                textureView,
                rlHostView.childCount,
                yourself
            ), layoutInflater
        )
        if (rlHostView.childCount < 1) {
            layoutParent.view.findViewById<View>(R.id.video_mask)
                .setBackgroundResource(R.color.black)
        }
        rlHostView.addVideoView(layoutParent.view)

        if (setupLocalVideo) launch({
            delay(500)
            RtcManager.instance.rtcEngine?.setupLocalVideo(VideoCanvas(textureView))
        })
        return layoutParent
    }

    override fun onPause() {
        pauseCameraMike()
        super.onPause()
    }

    override fun onResume() {
        resumeCameraMike()
        super.onResume()
    }

    abstract fun pauseCameraMike()
    abstract fun resumeCameraMike()
}