package io.anyrtc.videolive.ui.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import coil.load
import com.kongzue.dialog.v3.MessageDialog
import com.kongzue.dialog.v3.TipDialog
import com.kongzue.dialog.v3.WaitDialog
import io.anyrtc.videolive.App
import io.anyrtc.videolive.BuildConfig
import io.anyrtc.videolive.R
import io.anyrtc.videolive.databinding.FragmentHomeBinding
import io.anyrtc.videolive.databinding.FragmentMineBinding
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.SpUtil
import io.anyrtc.videolive.utils.toast
import io.anyrtc.videolive.vm.MainVM
import org.ar.rtc.RtcEngine

class MineFragment : Fragment(){

    private var _binding:FragmentMineBinding? =null
    private val binding get() = _binding!!
    private var  etName :EditText? =null
    private val mainVM: MainVM by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMineBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivIcon.load(SpUtil.get().getString(Constans.USER_ICON,""))
        binding.tvName.text = SpUtil.get().getString(Constans.USER_NAME,"")

        binding.tvYinsi.setOnClickListener {
            goH5("https://www.anyrtc.io/hide")
        }
        binding.tvRegister.setOnClickListener {
            goH5("https://console.anyrtc.io/signup")
        }
        binding.tvMianze.setOnClickListener {
            goH5("https://www.anyrtc.io/termsOfService")
        }

        binding.tvSdkVersion.text = "v ${RtcEngine.getSdkVersion()}"
        binding.tvAppVersion.text = "v ${BuildConfig.VERSION_NAME}"
        binding.tvPubTime.text = activity?.packageManager?.getApplicationInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_META_DATA)!!.metaData["releaseTime"].toString()

        binding.ivModifyName.setOnClickListener {
            showModifyNameDialog(binding.tvName.text.toString())
        }
        mainVM.observerModifyNameResult.observe(activity!!, Observer {
            if (it.code==0){
                binding.tvName.text = it.msg
                SpUtil.edit { editor ->
                    editor.putString(Constans.USER_NAME, it.msg)
                }
                TipDialog.show(activity!! as AppCompatActivity,"修改成功",TipDialog.TYPE.SUCCESS)
            }else{
                TipDialog.show(activity!! as AppCompatActivity,it.msg,TipDialog.TYPE.ERROR)
            }
        })

    }

    private fun goH5(url:String){
        startActivity(Intent().apply {
            action = "android.intent.action.VIEW"
            data = Uri.parse(url)
        })
    }

    private fun showModifyNameDialog(name:String = ""){
        MessageDialog.show(activity as AppCompatActivity,"修改昵称","最多输入 9 个字符")
            .setCancelable(true)
            .setCustomView(R.layout.layout_modify_name
            ) { dialog, v ->
                etName = v.findViewById<EditText>(R.id.et_name)
                etName?.let {
                    it.setText(name)
                    it.setSelection(name.length)
                }

            }
            .setOkButton("确定")
            .setCancelButton("取消") { baseDialog, v ->
                baseDialog.doDismiss()
                true
            }.setOnOkButtonClickListener { baseDialog, v ->
                if (etName?.text.toString().trim().isNullOrEmpty()){
                    toast("昵称不能为空")
                }else{
                    baseDialog.doDismiss()
                    WaitDialog.show(activity!! as AppCompatActivity,"正在修改...")
                    mainVM.modifyName(etName?.text.toString())
                }
                true
            }
    }

}