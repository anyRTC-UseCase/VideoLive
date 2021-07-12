package io.anyrtc.videolive.api

import android.os.Build
import io.anyrtc.videolive.api.bean.*
import io.anyrtc.videolive.utils.getRandomName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import rxhttp.async
import rxhttp.awaitResult
import rxhttp.toClass
import rxhttp.toStr
import rxhttp.wrapper.param.RxHttp

class ServerManager private constructor() {


    suspend fun signUp(scope: CoroutineScope): Deferred<SignUpBean> {
        return RxHttp.postJson(API.SIGN_UP)
            .add("sex", 0)
            .add("userName", getRandomName()).toClass<SignUpBean>().async(scope)
    }

    suspend fun signIn(uid: String, scope: CoroutineScope): Deferred<SignInBean> {
        return RxHttp.postJson(API.SIGN_IN)
            .add("cType", 1)
            //.add("pkg", "org.ar.videolive")//先用之前的
            .add("pkg", "org.ar.videolive")//先用之前的
            .add("uid", uid).toClass<SignInBean>().async(scope)
    }

    suspend fun getRoomList(
        pageNum: Int,
        pageSize: Int,
        scope: CoroutineScope
    ): Deferred<RoomListBean> {
        return RxHttp.postJson(API.GET_ROOM_LIST)
            .add("pageNum", pageNum)
            .add("pageSize", pageSize)
            .toClass<RoomListBean>().async(scope)
    }

    suspend fun createRoom(
        type: Int,
        roomTitle: String,
        scope: CoroutineScope
    ): Deferred<CreateRoomBean> {
        return RxHttp.postJson(API.CREATE_ROOM)
            .add("cType", 1)
            .add("pkg", "org.ar.videolive")//先用之前的
            .add("rType", type)
            .add("roomName", roomTitle)
            .toClass<CreateRoomBean>().async(scope)
    }

    suspend fun deleteRoom(roomId: String) {
        RxHttp.postJson(API.DELETE_ROOM)
            .add("roomId", roomId)
            .toStr().awaitResult { }
    }

    suspend fun joinRoom(
        roomId: String,
        roomType: String,
        scope: CoroutineScope
    ): Deferred<JoinRoomBean> {
        return RxHttp.postJson(API.JOIN_ROOM)
            .add("roomId", roomId)
            .add("cType", 1)
            .add("pkg", "org.ar.videolive")
            .add("rType", roomType)
            .toClass<JoinRoomBean>().async(scope)
    }

    suspend fun leaveRoom(
        roomId: String,
        scope: CoroutineScope
    ): Deferred<LeaveRoomBean> {
        return RxHttp.postJson(API.LEAVE_ROOM)
            .add("roomId", roomId)
            //.add("pkg", "org.ar.videolive")
            //.add("rType", roomType)
            .toClass<LeaveRoomBean>().async(scope)
    }

    suspend fun modifyName(name: String, scope: CoroutineScope): Deferred<ModifyNameBean> {
        return RxHttp.postJson(API.MODIFY_NAME)
            .add("userName", name)
            .toClass<ModifyNameBean>().async(scope)
    }

    suspend fun getMusicList(scope: CoroutineScope): Deferred<MusicBean> {
        return RxHttp.get(API.GET_MUSIC).toClass<MusicBean>().async(scope)
    }

    suspend fun updateMusicStatus(
        state: Int,
        roomId: String,
        scope: CoroutineScope
    ): Deferred<String> {
        return RxHttp.postJson(API.UPDATE_MUSIC_STATES)
            .add("musicState", state)
            .add("roomId", roomId)
            .toStr().async(scope)
    }

    suspend fun getRoomMusicInfo(roomId: String, scope: CoroutineScope): Deferred<RoomMusicInfo> {
        return RxHttp.postJson(API.GET_ROOM_MUSIC_INFO)
            .add("roomId", roomId)
            .toClass<RoomMusicInfo>().async(scope)
    }

    suspend fun getUserInfo(uid: String, scope: CoroutineScope): Deferred<UserInfoBean> {
        return RxHttp.postJson(API.GET_USER_INFO)
            .add("uid", uid)
            .toClass<UserInfoBean>().async(scope)
    }

    companion object {
        val instance: ServerManager by lazy {
            ServerManager()
        }
    }
}