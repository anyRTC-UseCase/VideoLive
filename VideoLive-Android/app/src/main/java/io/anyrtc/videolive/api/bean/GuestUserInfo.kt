package io.anyrtc.videolive.api.bean

import io.anyrtc.videolive.view.videobuilder.VideoViewModel

data class GuestUserInfo(
    val uid: String,
    val avatar: String,
    val nickname: String,
    var isMute: Boolean = false,
    var isCloseCamera: Boolean = false,
    var isRemove: Boolean = false,
    var waitForJoinRTC: Boolean = true,
    var index: Int = 0
) : VideoViewModel