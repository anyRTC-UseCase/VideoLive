package io.anyrtc.videolive.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

class ChatRecyclerView
@JvmOverloads
constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RecyclerView(ctx, attrs, defStyleAttr) {

    override fun getTopFadingEdgeStrength(): Float {
        return super.getTopFadingEdgeStrength()
    }

    override fun getBottomFadingEdgeStrength() = 0.0f
}