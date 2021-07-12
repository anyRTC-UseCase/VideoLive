package io.anyrtc.videolive.api.bean

data class UserInfoBean(
    val data: UserInfoMenu
)

data class UserInfoMenu(
    val avatar: String,
    val userName: String
)