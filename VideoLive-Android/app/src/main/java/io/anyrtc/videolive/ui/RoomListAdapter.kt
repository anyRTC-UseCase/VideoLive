package io.anyrtc.videolive.ui

import android.widget.ImageView
import coil.load
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.bean.RoomListBean
import io.anyrtc.videolive.utils.Constans

class RoomListAdapter :BaseQuickAdapter<RoomListBean.DataBean.ListBean,BaseViewHolder>(R.layout.layout_room_list) {


    override fun convert(holder: BaseViewHolder, item: RoomListBean.DataBean.ListBean) {
        holder.setText(R.id.tv_room_name,item.roomName)
        holder.setText(R.id.tv_look_num,"${item.userNum.toString()}人在看")
        val imageView = holder.getView<ImageView>(R.id.iv_bg)
        imageView.load(item.imageUrl)
    }
}