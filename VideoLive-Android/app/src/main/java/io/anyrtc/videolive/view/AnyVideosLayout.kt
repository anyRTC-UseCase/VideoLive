package io.anyrtc.videolive.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import io.anyrtc.videolive.R
import java.util.*

class AnyVideosLayout
@JvmOverloads
constructor(
    ctx: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): ViewGroup(ctx, attrs, defStyleAttr), ViewTreeObserver.OnGlobalLayoutListener {

    companion object {
        const val MODE_TOPIC = true
        const val MODE_EQUALLY = false

        private const val TYPE_REMOVE = 1
        private const val TYPE_TOGGLE = 2
        private const val TYPE_ADD = 3
    }

    private var multiViewWidth = 0
    private var multiViewHeight = 0
    private var smallViewWidth = 0
    private var smallViewHeight = 0

    private var animRunning = false
        @Synchronized
        set

    private var defMultipleVideosTopPadding = 0

    private var layoutTopicMode = MODE_TOPIC
        @Synchronized
        set

    private var onDrawDone = false

    private val taskLink: LinkedList<TaskData> by lazy {
        LinkedList<TaskData>()
    }

    /**
     * @param multipleWidgetPadding : 2 - 3 个控件时读取
     * @param maxWidgetPadding : only 4 widget to available
     * @param defMultipleVideosTopPadding : multiple ]top padding
     */
    private var multipleWidgetPadding = 0
    private var maxWidgetPadding = 0

    init {
        viewTreeObserver.addOnGlobalLayoutListener(this)
        attrs?.let {
            val typedArray = resources.obtainAttributes(it, R.styleable.AnyVideoGroup)
            multipleWidgetPadding = typedArray.getDimensionPixelOffset(
                R.styleable.AnyVideoGroup_between23viewsPadding, 0
            )
            maxWidgetPadding = typedArray.getDimensionPixelOffset(
                R.styleable.AnyVideoGroup_at4smallViewsPadding, 0
            )
            defMultipleVideosTopPadding = typedArray.getDimensionPixelOffset(
                R.styleable.AnyVideoGroup_defMultipleVideosTopPadding, 0
            )
            layoutTopicMode = typedArray.getBoolean(
                R.styleable.AnyVideoGroup_initTopicMode, layoutTopicMode
            )
            typedArray.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        multiViewWidth = widthSize.shr(1)
        multiViewHeight = (multiViewWidth.toFloat() * 1.33334f).toInt()
        smallViewWidth = (widthSize * 0.3125f).toInt()
        smallViewHeight = (smallViewWidth.toFloat() * 1.33334f).toInt()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val info = child.tag as ViewLayoutInfo
            child.measure(
                MeasureSpec.makeMeasureSpec((info.right - info.left).toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((info.bottom - info.top).toInt(), MeasureSpec.EXACTLY)
            )
        }

        setMeasuredDimension(
            MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (childCount == 0)
            return

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val layoutInfo = child.tag as ViewLayoutInfo
            child.layout(
                layoutInfo.left.toInt(),
                layoutInfo.top.toInt(),
                layoutInfo.right.toInt(),
                layoutInfo.bottom.toInt()
            )
            if (layoutInfo.isAlpha) {
                val progress = if (layoutInfo.isConverted)
                    1.0f - layoutInfo.progress
                else
                    layoutInfo.progress

                child.alpha = progress
            }
        }
    }

    fun addVideoView(view: View) {
        if (childCount > 3) {
            return
        }
        if (animRunning || !onDrawDone) {
            taskLink.add(TaskData(TYPE_ADD, view))
            return
        }

        if (childCount == 0) {
            view.tag = ViewLayoutInfo(
                0, 0, measuredWidth, measuredHeight,
                toRight = measuredWidth, toBottom = measuredHeight
            )
            addView(view)
            return
        }

        if (layoutTopicMode) {
            for (i in 1 until childCount) {
                val childInfo = getChildAt(i).tag as ViewLayoutInfo
                childInfo.run {
                    toLeft = measuredWidth - maxWidgetPadding - smallViewWidth
                    toTop = defMultipleVideosTopPadding + (i - 1) * smallViewHeight + (i - 1) * maxWidgetPadding
                    toRight = measuredWidth - maxWidgetPadding
                    toBottom = toTop + smallViewHeight
                }
            }

            val nTop = defMultipleVideosTopPadding + (childCount - 1) * smallViewHeight + (childCount - 1) * maxWidgetPadding
            view.tag = ViewLayoutInfo(
                measuredWidth - maxWidgetPadding - smallViewWidth,
                nTop,
                measuredWidth - maxWidgetPadding,
                nTop + smallViewHeight,
                isAlpha = true
            )
            addView(view)
        } else {
            var posOffset = 0
            var pos = 0
            if (childCount == 2) {
                posOffset = 2
                pos++

                (getChildAt(0).tag as ViewLayoutInfo).run {
                    toLeft = measuredWidth.shr(1) - multiViewWidth.shr(1)
                    toTop = defMultipleVideosTopPadding
                    toRight = measuredWidth.shr(1) + multiViewWidth.shr(1)
                    toBottom = defMultipleVideosTopPadding + multiViewHeight
                }
            }

            for (i in pos until childCount) {
                val topFloor = posOffset / 2
                val leftFloor = posOffset % 2
                (getChildAt(i).tag as ViewLayoutInfo).run {
                    toLeft = leftFloor * measuredWidth.shr(1) + leftFloor * multipleWidgetPadding
                    toTop = topFloor * multiViewHeight + topFloor * multipleWidgetPadding + defMultipleVideosTopPadding
                    toRight = toLeft + multiViewWidth
                    toBottom = toTop + multiViewHeight
                }
                posOffset++
            }

            val topFloor = posOffset / 2
            val leftFloor = posOffset % 2
            val nLeft = leftFloor * measuredWidth.shr(1) + leftFloor * multipleWidgetPadding
            val nTop = topFloor * multiViewHeight + topFloor * multipleWidgetPadding + defMultipleVideosTopPadding
            view.tag = ViewLayoutInfo(
                nLeft, nTop, nLeft + multiViewWidth, nTop + multiViewHeight,
                isAlpha = true
            )
            addView(view)
        }

        post(AnimThread(
            (0 until childCount).map { getChildAt(it).tag as ViewLayoutInfo }.toTypedArray()
        ))
    }

    fun removeVideoView(position: Int) {
        if (childCount == 1 || animRunning || position >= childCount || position <= 0) {
            if (animRunning && (position in 1 until childCount)) {
                taskLink.add(TaskData(TYPE_REMOVE, position))
            }
            return
        }

        val removedInfo = getChildAt(position).tag as ViewLayoutInfo
        removedInfo.run {
            val lastHeight = originalBottom - originalTop
            toLeft = originalLeft
            toTop = originalTop + lastHeight.shr(1)
            toRight = originalRight
            toBottom = originalBottom - lastHeight.shr(1)
            pos = position
            waitingDestroy = true
        }
        if (childCount - 1 > 1) {
            if (layoutTopicMode) {
                var index = 0
                for (i in 1 until childCount) if (i != position) (getChildAt(i).tag as ViewLayoutInfo).run {
                    toLeft = measuredWidth - maxWidgetPadding - smallViewWidth
                    toTop = defMultipleVideosTopPadding + index * smallViewHeight + index * maxWidgetPadding
                    toRight = measuredWidth - maxWidgetPadding
                    toBottom = toTop + smallViewHeight
                    index++
                }
            } else {
                var posOffset = 0
                var pos = 0
                if (childCount == 4) {
                    posOffset = 2
                    pos++

                    (getChildAt(0).tag as ViewLayoutInfo).run {
                        toLeft = measuredWidth.shr(1) - multiViewWidth.shr(1)
                        toTop = defMultipleVideosTopPadding
                        toRight = measuredWidth.shr(1) + multiViewWidth.shr(1)
                        toBottom = defMultipleVideosTopPadding + multiViewHeight
                    }
                }

                for (i in pos until childCount) if (i != position) {
                    val topFloor = posOffset / 2
                    val leftFloor = posOffset % 2
                    (getChildAt(i).tag as ViewLayoutInfo).run {
                        toLeft = leftFloor * measuredWidth.shr(1) + leftFloor * multipleWidgetPadding
                        toTop = topFloor * multiViewHeight + topFloor * multipleWidgetPadding + defMultipleVideosTopPadding
                        toRight = toLeft + multiViewWidth
                        toBottom = toTop + multiViewHeight
                    }
                    posOffset++
                }
            }
        } else {
            (getChildAt(0).tag as ViewLayoutInfo).run {
                toLeft = 0
                toTop = 0
                toRight = measuredWidth
                toBottom = measuredHeight
            }
        }

        post(AnimThread(
            (0 until childCount).map { getChildAt(it).tag as ViewLayoutInfo }.toTypedArray()
        ))
    }

    fun setVideoLayoutMode(mode: Boolean) {
        if (mode == layoutTopicMode) {
            return
        }
        if (childCount > 1) {
            toggleLayoutMode()
            return
        }
        layoutTopicMode = mode
    }

    fun getVideoLayoutMode() = this.layoutTopicMode

    fun toggleLayoutMode() {
        if (childCount < 2) {
            return
        }
        if (animRunning) {
            taskLink.add(TaskData(TYPE_TOGGLE, null))
            return
        }

        if (layoutTopicMode) {
            var posOffset = 0
            var pos = 0
            if (childCount == 3) {
                posOffset = 2
                pos++

                (getChildAt(0).tag as ViewLayoutInfo).run {
                    toLeft = measuredWidth.shr(1) - multiViewWidth.shr(1)
                    toTop = defMultipleVideosTopPadding
                    toRight = measuredWidth.shr(1) + multiViewWidth.shr(1)
                    toBottom = defMultipleVideosTopPadding + multiViewHeight
                }
            }

            for (i in pos until childCount) {
                val topFloor = posOffset / 2
                val leftFloor = posOffset % 2
                (getChildAt(i).tag as ViewLayoutInfo).run {
                    toLeft = leftFloor * measuredWidth.shr(1) + leftFloor * multipleWidgetPadding
                    toTop = topFloor * multiViewHeight + topFloor * multipleWidgetPadding + defMultipleVideosTopPadding
                    toRight = toLeft + multiViewWidth
                    toBottom = toTop + multiViewHeight
                }
                posOffset++
            }
        } else {
            val firstInfo = getChildAt(0).tag as ViewLayoutInfo
            firstInfo.run {
                toLeft = 0
                toTop = 0
                toRight = measuredWidth
                toBottom = measuredHeight
            }

            for (i in 1 until childCount) {
                val childInfo = getChildAt(i).tag as ViewLayoutInfo
                childInfo.run {
                    toLeft = measuredWidth - maxWidgetPadding - smallViewWidth
                    toTop = defMultipleVideosTopPadding + (i - 1) * smallViewHeight + (i - 1) * maxWidgetPadding
                    toRight = measuredWidth - maxWidgetPadding
                    toBottom = toTop + smallViewHeight
                }
            }
        }

        post(AnimThread(
            (0 until childCount).map { getChildAt(it).tag as ViewLayoutInfo }.toTypedArray()
        ))
        layoutTopicMode = !layoutTopicMode
    }

    override fun post(action: Runnable?): Boolean {
        animRunning = true
        return super.post(action)
    }

    private data class ViewLayoutInfo(
        var originalLeft: Int = 0,
        var originalTop: Int = 0,
        var originalRight: Int = 0,
        var originalBottom: Int = 0,
        var left: Float = 0.0f,
        var top: Float = 0.0f,
        var right: Float = 0.0f,
        var bottom: Float = 0.0f,
        var toLeft: Int = 0,
        var toTop: Int = 0,
        var toRight: Int = 0,
        var toBottom: Int = 0,
        var progress: Float = 0.0f,
        var isAlpha: Boolean = false,
        var isConverted: Boolean = false,
        var waitingDestroy: Boolean = false,
        var pos: Int = 0
    ) {
        init {
            left = originalLeft.toFloat()
            top = originalTop.toFloat()
            right = originalRight.toFloat()
            bottom = originalBottom.toFloat()
        }
    }

    private data class TaskData(
        val waitingType: Int,
        val obj: Any?
    )

    private inner class AnimThread(
        private val viewInfoList: Array<ViewLayoutInfo>,
        private var duration: Float = 180.0f,
        private var processing: Float = 0.0f
    ) : Runnable {
        private val waitingTime = 9L

        override fun run() {
            var progress = processing / duration
            if (progress > 1.0f) {
                progress = 1.0f
            }

            for (viewInfo in viewInfoList) {
                if (viewInfo.isAlpha) {
                    viewInfo.progress = progress
                } else viewInfo.run {
                    val diffLeft = (toLeft - originalLeft) * progress
                    val diffTop = (toTop - originalTop) * progress
                    val diffRight = (toRight - originalRight) * progress
                    val diffBottom = (toBottom - originalBottom) * progress

                    left = originalLeft + diffLeft
                    top = originalTop + diffTop
                    right = originalRight + diffRight
                    bottom = originalBottom + diffBottom
                }
            }
            requestLayout()

            if (progress < 1.0f) {
                if (progress > 0.8f) {
                    var offset = ((progress - 0.7f) / 0.25f)
                    if (offset > 1.0f)
                        offset = 1.0f
                    processing += waitingTime - waitingTime * progress * 0.95f * offset
                } else {
                    processing += waitingTime
                }
                postDelayed(this@AnimThread, waitingTime)
            } else {
                for (viewInfo in viewInfoList) {
                    if (viewInfo.waitingDestroy) {
                        removeViewAt(viewInfo.pos)
                    } else viewInfo.run {
                        processing = 0.0f
                        duration = 0.0f
                        originalLeft = left.toInt()
                        originalTop = top.toInt()
                        originalRight = right.toInt()
                        originalBottom = bottom.toInt()
                        isAlpha = false
                        isConverted = false
                    }
                }
                animRunning = false
                processing = duration
                if (!taskLink.isEmpty()) {
                    invokeLinkedTask()
                }
            }
        }
    }

    private fun invokeLinkedTask() {
        val pop = taskLink.pop()
        when (pop.waitingType) {
            TYPE_ADD -> {
                addVideoView(pop.obj as View)
            }
            TYPE_REMOVE -> {
                val removePos = pop.obj as Int
                removeVideoView(if (removePos >= childCount) childCount - 1 else removePos)
            }
            TYPE_TOGGLE -> {
                toggleLayoutMode()
            }
        }
    }

    override fun onGlobalLayout() {
        onDrawDone = true
        if (taskLink.isNotEmpty()) {
            invokeLinkedTask()
        }
        viewTreeObserver.removeOnGlobalLayoutListener(this)
    }
}
