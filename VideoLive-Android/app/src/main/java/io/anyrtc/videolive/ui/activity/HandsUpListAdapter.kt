package io.anyrtc.videolive.ui.activity

import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.bean.GuestUserInfo

class HandsUpListAdapter :
    BaseQuickAdapter<GuestUserInfo, BaseViewHolder>(R.layout.rtc_host_bottom_sheet_item) {

    private companion object {
        val obj = Any()
    }

    var acceptNum = 0
        set(value) {
            synchronized(obj) {
                if (field >= 3 && value < 3) {
                    field = value
                    notifyItemRangeChanged(0, data.size)
                } else if (field < 3 && value >= 3) {
                    field = value
                    notifyItemRangeChanged(0, data.size)
                } else {
                    field = value
                }
            }
        }
        get() {
            return synchronized(obj) {
                field
            }
        }

    override fun convert(holder: BaseViewHolder, item: GuestUserInfo) {
        holder.getView<ImageView>(R.id.avatar).load(item.avatar)
        holder.setText(R.id.nickname, item.nickname)

        holder.setBackgroundResource(
            R.id.agree,
            if (acceptNum >= 3) R.drawable.rtc_host_bottom_sheet_agree_disabled_bg
            else R.drawable.rtc_host_bottom_sheet_agree_bg
        )
    }
}