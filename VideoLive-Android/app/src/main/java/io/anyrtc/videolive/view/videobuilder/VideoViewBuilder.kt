package io.anyrtc.videolive.view.videobuilder

import android.view.LayoutInflater

interface VideoViewBuilder<T : VideoViewParent, D : VideoViewModel> {

    fun buildVideoViewParent(viewData: T, layoutInflater: LayoutInflater): T
    fun setVideoViewStates(videoViewParent: T, info: D)
}