package io.anyrtc.videolive.api.bean

data class CDNStreamLayoutInfo(
    val uid: String,
    var width: Int = 360,
    var height: Int = 640,
    var top: Int = 0,
    var left: Int = 0
) {
    companion object {
        val RESOLUTION_NORMAL = CDNStreamLayoutInfo("", width = 360, height = 640)
        val RESOLUTION_HIGH = CDNStreamLayoutInfo("", width = 540, height = 960)
        val RESOLUTION_ULTRA = CDNStreamLayoutInfo("", width = 720, height = 1280)

        const val MULTIPLE_PADDING = 2//pixel
        //const val TOP_PADDING = 0.1379f// percent of screen(height)
        const val TOP_PADDING = 0.0379f// percent of screen(height)
        const val TOPIC_MULTIPLE_PADDING = 0.01f// percent of screen(height)
        const val TOPIC_WIDTH = 0.2933f// percent of screen(width)
        const val HEIGHT_PERCENT = 1.777777778f
    }
}