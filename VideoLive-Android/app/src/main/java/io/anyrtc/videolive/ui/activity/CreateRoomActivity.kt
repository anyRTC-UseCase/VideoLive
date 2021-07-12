package io.anyrtc.videolive.ui.activity

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.imageview.ShapeableImageView
import com.kongzue.dialog.v3.CustomDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.ServerManager
import io.anyrtc.videolive.databinding.ActivityCreateRoomBinding
import io.anyrtc.videolive.sdk.RtmManager
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.Interval
import io.anyrtc.videolive.utils.SpUtil
import io.anyrtc.videolive.utils.toast
import kotlinx.coroutines.launch
import org.ar.rtm.ErrorInfo
import org.ar.rtm.ResultCallback
import rxhttp.awaitResult
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CreateRoomActivity : BaseActivity() {
    private val binding by lazy { ActivityCreateRoomBinding.inflate(layoutInflater) }
    private var roomType = 6
    private var loadingDialog: CustomDialog? = null

    private var creating = false
        @Synchronized
        set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.btnCreate.setOnClickListener {
            if (binding.etTopic.text.isEmpty()) {
                toast("请输入房间主题")
            } else {
                createRoom()
            }
        }
        binding.rgMode.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_rtc -> roomType = 6
                R.id.rb_local -> roomType = 7
                R.id.rb_server -> roomType = 8
            }
        }
    }


    private fun createRoom() {//整个页面只有1个接口 也不需要保存什么数据 就不写viewModel了
        if (creating)
            return

        creating = true
        Interval(0, 3, TimeUnit.SECONDS, 1).life(this).finish {
            creating = false
        }.start()

        lifecycleScope.launch {
            showLoading()
            ServerManager.instance.createRoom(roomType, binding.etTopic.text.toString(), this)
                .awaitResult { result ->
                    if (result.code == 0) {//创建成功
                        lifecycleScope.launch {
                            val rtmResult = loginRtm(
                                result.data.rtmToken,
                                SpUtil.get().getString(Constans.USER_ID, "").toString()
                            )
                            if (rtmResult) {
                                loadingDialog?.doDismiss()
                                startActivity(Intent().apply {
                                    putExtra(Constans.RTC_TOKEN, result.data.rtcToken)
                                    putExtra(Constans.RTM_TOKEN, result.data.rtmToken)
                                    putExtra(Constans.ROOM_TYPE, roomType)
                                    putExtra(Constans.ROOM_ID, result.data.roomId)
                                    when (roomType) {
                                        6 -> {//RTC实时直播
                                            setClass(
                                                this@CreateRoomActivity,
                                                RTCHostActivity::class.java
                                            )
                                        }
                                        7, 8 -> {//客户端推流到CDN
                                            putExtra(Constans.PUSH_URL, result.data.pushUrl)
                                            setClass(
                                                this@CreateRoomActivity,
                                                CDNHostActivity::class.java
                                            )
                                        }
                                        /*8->{//服务端推流到CDN
                                        }*/
                                    }
                                })
                            } else {
                                loadingDialog?.doDismiss()
                                toast("登录RTM失败")
                            }
                        }
                    } else {
                        loadingDialog?.doDismiss()
                        toast(result.msg)
                    }
                }.onFailure {
                    loadingDialog?.doDismiss()
                    toast(it.message!!)
                }
        }
    }

    private suspend fun loginRtm(token: String, userId: String) = suspendCoroutine<Boolean> {
        RtmManager.instance.login(token,
            userId, object :
                ResultCallback<Void> {
                override fun onSuccess(var1: Void?) {
                    it.resume(true)
                }

                override fun onFailure(var1: ErrorInfo?) {
                    it.resume(false)
                }
            })
    }

    override fun onPause() {
        super.onPause()
        creating = false
    }

    private fun showLoading() {
        var animator: ObjectAnimator? = null
        loadingDialog = CustomDialog.build(
            this as AppCompatActivity,
            R.layout.layout_loading
        ) { _, v ->
            val ivIcon = v?.findViewById<ShapeableImageView>(R.id.iv_icon)
            val tvTip = v?.findViewById<TextView>(R.id.tv_tip)
            tvTip?.text = "正在创建..."
            animator = ObjectAnimator.ofFloat(ivIcon, "rotation", 0f, 360f).apply {
                duration = 1000
                repeatCount = -1
                start()
            }
        }.setCancelable(false).setFullScreen(true).setOnDismissListener {
            animator?.cancel()
        }
        loadingDialog?.show()
    }
}