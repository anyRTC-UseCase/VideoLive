package io.anyrtc.videolive.ui.activity

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import io.anyrtc.videolive.R
import io.anyrtc.videolive.api.bean.ChatMessageData

class ChatMessageListAdapter :
    BaseQuickAdapter<ChatMessageData, BaseViewHolder>(R.layout.item_chat) {

    override fun convert(holder: BaseViewHolder, item: ChatMessageData) {
        val ss = SpannableString("${item.nickname} ${item.content}")
        ss.setSpan(
            ForegroundColorSpan(Color.parseColor(if (item.yourself) "#FFBB8D" else "#8DAEFF")),
            0,
            item.nickname.length,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        holder.setText(R.id.message, ss)
    }

    override fun addData(data: ChatMessageData) {
        super.addData(data)
        if (this.data.size > 50) {
            this.removeAt(0)
        }
        recyclerView.scrollToPosition(this.data.size - 1)
    }
}