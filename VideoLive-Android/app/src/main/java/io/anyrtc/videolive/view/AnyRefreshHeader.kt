package io.anyrtc.videolive.view

import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshKernel
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import io.anyrtc.videolive.R
import io.anyrtc.videolive.utils.dp2px

class AnyRefreshHeader @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): LinearLayout(context, attrs, defStyleAttr),RefreshHeader {

    private lateinit var ivLogo:ImageView
    private var animator : ObjectAnimator? = null

    init {
        initView(context)
    }

    private fun initView(context: Context){
        gravity = Gravity.CENTER
        ivLogo = ImageView(context)
        ivLogo.setImageResource(R.drawable.img_refresh_logo)
        addView(ivLogo,29f.dp2px(),29f.dp2px())
        animator = ObjectAnimator.ofFloat(ivLogo,"rotation",0f,360f).apply {
            duration = 500
            repeatCount =-1
        }
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState
    ) {
    }

    override fun getView(): View {
        return this
    }

    override fun getSpinnerStyle(): SpinnerStyle {
        return SpinnerStyle.Translate
    }

    override fun setPrimaryColors(vararg colors: Int) {
    }

    override fun onInitialized(kernel: RefreshKernel, height: Int, maxDragHeight: Int) {
    }

    override fun onMoving(
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        height: Int,
        maxDragHeight: Int
    ) {
    }

    override fun onReleased(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
    }

    override fun onStartAnimator(refreshLayout: RefreshLayout, height: Int, maxDragHeight: Int) {
        animator?.start()
    }

    override fun onFinish(refreshLayout: RefreshLayout, success: Boolean): Int {
        animator?.cancel()
        return 500
    }

    override fun onHorizontalDrag(percentX: Float, offsetX: Int, offsetMax: Int) {
    }

    override fun isSupportHorizontalDrag(): Boolean {
        return false
    }


}