package io.anyrtc.videolive.view.videobuilder

import android.view.View

data class DefaultVideoViewParent(
    var view: View,
    val inGroupIndex: Int,
    val yourself: Boolean = false
) : VideoViewParent