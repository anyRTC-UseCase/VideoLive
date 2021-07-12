package io.anyrtc.videolive.view

import android.content.Context
import android.util.AttributeSet
import com.scwang.smart.refresh.layout.SmartRefreshLayout

class SmartRefreshRewrite
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null): SmartRefreshLayout(context, attrs) {

    fun setStateRefresh(notify: Boolean) {
        super.setStateRefreshing(notify)
    }
}