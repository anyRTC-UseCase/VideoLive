package io.anyrtc.videolive.api.bean

import io.anyrtc.videolive.view.videobuilder.VideoViewModel

data class ChatMessageData(
    val nickname: String,
    val content: String,
    val yourself: Boolean
) : VideoViewModel