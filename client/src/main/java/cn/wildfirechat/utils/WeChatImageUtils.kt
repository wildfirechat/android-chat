package cn.wildfirechat.utils

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import cn.wildfirechat.message.VideoMessageContent
import cn.wildfirechat.model.VideoParam
import java.io.File


/**
 * @ClassName WechatImageUtils
 * @Description 高仿微信的一种 图片宽高比
 * @Author dhl
 * @Date 2020/12/31 11:07
 * @Version 1.0
 */
object WeChatImageUtils {


    @JvmStatic
    fun getImageSizeByOrgSizeToWeChat( outWidth:Int,  outHeight:Int): IntArray? {
        var imageWidth = 300
        var imageHeight = 300
        val maxWidth = 400
        val maxHeight = 400
        val minWidth = 300
        val minHeight = 250

        if(outWidth == 0 && outHeight == 0){
            return intArrayOf(imageWidth,imageHeight)
        }
        if (outWidth / maxWidth > outHeight / maxHeight) { //
            if (outWidth >= maxWidth) { //
                imageWidth = maxWidth
                imageHeight = outHeight * maxWidth / outWidth
            } else {
                imageWidth = outWidth
                imageHeight = outHeight
            }
            if (outHeight < minHeight) {
                imageHeight = minHeight
                val width: Int = outWidth * minHeight / outHeight
                imageWidth = if (width > maxWidth) {
                    maxWidth
                } else {
                    width
                }
            }
        } else {
            if (outHeight >= maxHeight) {
                imageHeight = maxHeight
                imageWidth = if (outHeight / maxHeight > 10) {
                    outWidth * 5 * maxHeight / outHeight
                } else {
                    outWidth * maxHeight / outHeight
                }
            } else {
                imageHeight = outHeight
                imageWidth = outWidth
            }
            if (outWidth < minWidth) {
                imageWidth = minWidth
                val height: Int = outHeight * minWidth / outWidth
                imageHeight = if (height > maxHeight) {
                    maxHeight
                } else {
                    height
                }
            }
        }

        return intArrayOf(imageWidth,imageHeight)
    }

    /**
     * 获取图片的尺寸，只获取尺寸，不加载bitmap 不会耗时
     *
     */
    @JvmStatic
    fun getSize(file: File?): IntArray? {
        if (file == null) return intArrayOf(0, 0)
        val opts = BitmapFactory.Options()
        opts.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, opts)
        return intArrayOf(opts.outWidth, opts.outHeight)
    }
    /**
     * 获取小视频的参数的尺寸，时长
     *
     */
    @JvmStatic
    fun getVideoParam(path: String): VideoParam? {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        var videoParam: VideoParam? = null
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        val bitmap = retriever.frameAtTime
        if (bitmap != null) {
            val width = bitmap.width
            val height = bitmap.height
            videoParam = VideoParam(width, height, duration)
        }
        retriever.release()
        return videoParam
    }
}