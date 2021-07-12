package io.anyrtc.videolive.view.videobuilder

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import coil.load
import com.google.android.material.imageview.ShapeableImageView
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.bean.GuestUserInfo
import io.anyrtc.videolive.view.AnyVideosLayout

object DefaultVideoViewBuilderImpl : VideoViewBuilder<DefaultVideoViewParent, GuestUserInfo> {

    @SuppressLint("InflateParams")
    override fun buildVideoViewParent(
        viewData: DefaultVideoViewParent,
        layoutInflater: LayoutInflater
    ): DefaultVideoViewParent {
        val layoutParent = layoutInflater.inflate(R.layout.layout_parent_texture, null)// view parent must be null.
        val videoParent = layoutParent.findViewById<RelativeLayout>(R.id.video_parent)
        if (viewData.yourself) videoParent.tag = true
        videoParent.addView(viewData.view, RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        })
        viewData.view = layoutParent
        return viewData
    }

    override fun setVideoViewStates(videoViewParent: DefaultVideoViewParent, info: GuestUserInfo) {
        val layoutParent = videoViewParent.view
        val videoMaskView = layoutParent.findViewById<View>(R.id.video_mask)
        val avatarView = layoutParent.findViewById<ShapeableImageView>(R.id.avatar)
        val nicknameView = layoutParent.findViewById<TextView>(R.id.nickname)
        val iconMicrophone = layoutParent.findViewById<View>(R.id.icon_microphone)

        if (info.isCloseCamera) {
            videoMaskView.visibility = View.VISIBLE
            avatarView.visibility = View.VISIBLE
            nicknameView.visibility = View.VISIBLE

            avatarView.load(info.avatar)
            nicknameView.text = info.nickname
        } else {
            videoMaskView.visibility = View.GONE
            avatarView.visibility = View.GONE
            nicknameView.visibility = View.GONE
        }

        iconMicrophone.visibility = if (info.isMute) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    fun getVideoView(
        videoViewList: List<DefaultVideoViewParent>,
        targetIndex: Int
    ): DefaultVideoViewParent? {
        var listIndex = -1
        videoViewList.forEachIndexed { i, it ->
            if (it.inGroupIndex == targetIndex) {
                listIndex = i
                return@forEachIndexed
            }
        }

        return if (listIndex == -1) null else videoViewList[listIndex]
    }

    fun getYourself(videoViewList: List<DefaultVideoViewParent>): DefaultVideoViewParent? {
        var t: DefaultVideoViewParent? = null
        videoViewList.forEach {
            if (it.yourself)
                t = it
        }
        return t
    }

    fun changeLayoutMode(mode: Boolean, viewGroup: AnyVideosLayout) {
        if (mode && viewGroup.childCount > 1) {// topic mode
            val mask = viewGroup.getChildAt(0).findViewById<View>(R.id.video_mask)
            mask.setBackgroundResource(R.color.black)
        } else if (viewGroup.childCount > 1) {
            val mask = viewGroup.getChildAt(0).findViewById<View>(R.id.video_mask)
            mask.setBackgroundResource(R.drawable.shape_texture_parent_bg)
        }
    }
}