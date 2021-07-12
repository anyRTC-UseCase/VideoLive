package io.anyrtc.videolive.ui.fragment

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.imageview.ShapeableImageView
import com.kongzue.dialog.v3.CustomDialog
import com.kongzue.dialog.v3.TipDialog
import com.kongzue.dialog.v3.WaitDialog
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.ServerManager
import io.anyrtc.videolive.databinding.FragmentHomeBinding
import io.anyrtc.videolive.sdk.RtmManager
import io.anyrtc.videolive.ui.RoomListAdapter
import io.anyrtc.videolive.ui.activity.CDNGuestActivity
import io.anyrtc.videolive.ui.activity.CreateRoomActivity
import io.anyrtc.videolive.ui.activity.RTCGuestActivity
import io.anyrtc.videolive.utils.*
import io.anyrtc.videolive.vm.MainVM
import io.anyrtc.videolive.weight.Spacing
import io.anyrtc.videolive.weight.SpacingItemDecoration
import kotlinx.coroutines.launch
import org.ar.rtm.ErrorInfo
import org.ar.rtm.ResultCallback
import rxhttp.awaitResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val mainVM: MainVM by viewModels()
    private val roomListAdapter: RoomListAdapter by lazy { RoomListAdapter() }
    private var loadingDialog: CustomDialog? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvList.layoutManager =
            GridLayoutManager(activity, 2, GridLayoutManager.VERTICAL, false)
        binding.rvList.addItemDecoration(SpacingItemDecoration(Spacing().apply {
            horizontal = 15f.dp2px()
            vertical = 15f.dp2px()
            edges = Rect(15f.dp2px(), 15f.dp2px(), 15f.dp2px(), 15f.dp2px())
        }))
        binding.rvList.adapter = roomListAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            WaitDialog.show(activity as AppCompatActivity, "正在登录...")
            mainVM.login()
        }

        mainVM.observerSignResult.observe(activity!!, Observer {
            if (it.code == 0) {
                TipDialog.show(activity as AppCompatActivity, "登录成功", TipDialog.TYPE.SUCCESS)
                mainVM.getRoomList()
            } else {
                if (binding.smartRefresh.isRefreshing) {
                    binding.smartRefresh.finishRefresh()
                }
                TipDialog.show(activity as AppCompatActivity, it.msg, TipDialog.TYPE.ERROR)
            }
        })

        mainVM.observerRoomList.observe(activity!!, {
            binding.smartRefresh.finishRefresh()
            roomListAdapter.setList(it)
            roomListAdapter.setEmptyView(R.layout.layout_no_room)
        })


        binding.btnCreateRoom.throttleClick {
            activity?.go(CreateRoomActivity::class.java)
        }

        binding.smartRefresh.setOnRefreshListener {
            if (mainVM.isLoginSuccess()) {
                mainVM.getRoomList()
            } else {
                binding.smartRefresh.finishRefresh()
                TipDialog.show(
                    activity as AppCompatActivity,
                    "登录未成功，正在尝试重新登录",
                    TipDialog.TYPE.ERROR
                )
                viewLifecycleOwner.lifecycleScope.launch {
                    mainVM.login()
                }
            }
        }

        roomListAdapter.setOnItemClickListener { _, _, position ->
            showLoading()
            viewLifecycleOwner.lifecycleScope.launch {
                ServerManager.instance.joinRoom(
                    roomListAdapter.data[position].roomId,
                    roomListAdapter.data[position].roomType,
                    this
                ).awaitResult { result ->
                    if (result.code == 0) {
                        val userId = SpUtil.get().getString(Constans.USER_ID, "")!!
                        launch {
                            val loginSuccess = loginRtm(result.data.room.rtmToken, userId)
                            loadingDialog?.doDismiss()
                            if (loginSuccess) startActivityForResult(Intent().apply {
                                putExtra(Constans.RTC_TOKEN, result.data.room.rtcToken)
                                putExtra(Constans.RTM_TOKEN, result.data.room.rtmToken)
                                putExtra(Constans.ROOM_TYPE, result.data.room.getrType())
                                putExtra(Constans.ROOM_ID, roomListAdapter.data[position].roomId)
                                putExtra(Constans.HOST_ID, result.data.room.ower.uid)
                                when (roomListAdapter.data[position].roomType) {
                                    "6" -> {//RTC实时直播
                                        setClass(activity!!, RTCGuestActivity::class.java)
                                    }
                                    "7", "8" -> {//客户端/服务端推流到CDN
                                        putExtra(Constans.PULL_URL, result.data.pullRtmpUrl)
                                        setClass(activity!!, CDNGuestActivity::class.java)
                                    }
                                }
                            }, 0) else {
                                toast("登录失败，请检查网络")
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
    }

    override fun onResume() {
        super.onResume()
        binding.smartRefresh.setStateRefresh(true)
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


    private fun showLoading() {
        var animator: ObjectAnimator? = null
        loadingDialog = CustomDialog.build(
            activity as AppCompatActivity,
            R.layout.layout_loading
        ) { _, v ->
            val ivIcon = v?.findViewById<ShapeableImageView>(R.id.iv_icon)
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