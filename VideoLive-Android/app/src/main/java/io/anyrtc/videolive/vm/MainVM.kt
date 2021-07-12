package io.anyrtc.videolive.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.anyrtc.videolive.api.ServerManager
import io.anyrtc.videolive.api.bean.ErrorInfo
import io.anyrtc.videolive.api.bean.RoomListBean
import io.anyrtc.videolive.sdk.RtmManager
import io.anyrtc.videolive.utils.Constans
import io.anyrtc.videolive.utils.SpUtil
import kotlinx.coroutines.launch
import org.json.JSONObject
import rxhttp.awaitResult

class MainVM : ViewModel() {

    val observerRoomList = MutableLiveData<MutableList<RoomListBean.DataBean.ListBean>>()
    val observerSignResult = MutableLiveData<ErrorInfo>()
    val observerModifyNameResult = MutableLiveData<ErrorInfo>()
    private var isLoginSuccess = false

    fun getRoomList(){
        viewModelScope.launch {
           ServerManager.instance.getRoomList(1,1000,this).awaitResult {roomListBean ->
                if (roomListBean.code==0){
                    observerRoomList.value= roomListBean.data.list
                }else if (roomListBean.code==1054){
                    observerRoomList.value= arrayListOf()
                }
            }.onFailure {
               observerSignResult.value = ErrorInfo(it.message!!,-1)
            }

        }
    }

    fun login() {
        viewModelScope.launch {
            val uid = SpUtil.get().getString(Constans.USER_ID, "")
            if (uid.isNullOrEmpty()){
                ServerManager.instance.signUp(this).awaitResult {signUpBean ->
                    if (signUpBean.code == 0) {
                        SpUtil.edit {
                            it.putString(Constans.USER_ID, signUpBean.data.uid)
                            it.putString(Constans.USER_NAME, signUpBean.data.userName)
                        }
                        signIn(signUpBean.data.uid)
                    }else{
                        observerSignResult.value = ErrorInfo(signUpBean.msg,signUpBean.code)
                    }
                }.onFailure {
                    observerSignResult.value = ErrorInfo(it.message!!,-1)
                }

            }else{
                signIn(uid)
            }
        }
    }
    private fun signIn(uid:String){
        viewModelScope.launch {
            ServerManager.instance.signIn(uid, this).awaitResult{
                if (it.code == 0) {
                    SpUtil.edit {sp ->
                        sp.putString(Constans.HTTP_TOKEN, it.data.userToken)
                        sp.putString(Constans.USER_ICON, it.data.avatar)
                        sp.putString(Constans.APP_ID,it.data.appid)
                    }
                    isLoginSuccess = true
                    RtmManager.instance.initRtm()
                    observerSignResult.value = ErrorInfo("",0)
                }else{
                    observerSignResult.value = ErrorInfo(it.msg,it.code)
                }
            }.onFailure {
                observerSignResult.value = ErrorInfo(it.message!!,-1)
            }

        }

    }

    fun isLoginSuccess():Boolean{
        return isLoginSuccess
    }

    fun modifyName(name:String){
        viewModelScope.launch {
            ServerManager.instance.modifyName(name,this).awaitResult {
               if (it.code==0){
                   observerModifyNameResult.value = ErrorInfo(it.data.userName,0)
               }else{
                   observerModifyNameResult.value = ErrorInfo(it.msg,-1)
               }
            }.onFailure {
                observerModifyNameResult.value = ErrorInfo(it.message!!,-1)
            }
        }
    }


}