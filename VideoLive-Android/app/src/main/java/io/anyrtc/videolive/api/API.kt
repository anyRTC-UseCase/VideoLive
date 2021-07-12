package io.anyrtc.videolive.api

import io.anyrtc.videolive.BuildConfig
import rxhttp.wrapper.annotation.DefaultDomain

object API {

    @DefaultDomain
    /*@JvmField
    val BASE_API = if (!BuildConfig.DEBUG) {
        "http://192.168.1.115:12680/arapi/arlive/v1/"
    } else {
        "http://arlive.agrtc.cn:12680/arapi/arlive/v1/"
    }*/
    const val BASE_API = "http://arlive.agrtc.cn:12680/arapi/arlive/v1/"

    const val SIGN_UP = "user/signUp"

    const val SIGN_IN = "user/signIn"

    const val GET_ROOM_LIST = "user/getVidRoomList"

    const val CREATE_ROOM = "user/addRoom" //6:视频RTC实时直播,7:视频客户端推流到CDN,8:视频服务端推流到CDN

    const val DELETE_ROOM = "user/deleteRoom"

    const val JOIN_ROOM = "user/joinRoom"

    const val LEAVE_ROOM = "user/updateV2UserLeaveTs"

    const val MODIFY_NAME = "user/updateUserName"

    const val GET_MUSIC = "user/getMusicList"

    const val UPDATE_MUSIC_STATES = "user/updateMusicState"

    const val GET_ROOM_MUSIC_INFO = "user/getRoomMusicInfo"

    const val GET_USER_INFO = "user/getUserInfo"
}