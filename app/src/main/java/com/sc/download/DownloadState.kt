package com.sc.download

class DownloadState (  // 不包括url
        var downloadedLength: Long,
        var contentLength: Long,
        var state: Int,
        var time: Long) : Comparable<DownloadState> {
    lateinit var url: String // 下载的地址，不参与存储！

    override fun toString(): String {  // 不包括url
        return "$downloadedLength $contentLength $state $time"
    }

    override fun compareTo(other: DownloadState): Int {
        return other.time.compareTo(time)
    }

    companion object {
        const val DOWNLOAD_SUCCESS = 0
        const val DOWNLOAD_FAILED = 1
        const val DOWNLOAD_PAUSED = 2
        const val DOWNLOAD_CANCELED = 3
        const val DOWNLOADING = 4

        fun fromString(string: String?): DownloadState {  // 不包括url
            return try {
                val strings = string!!.split(" ").toTypedArray()
                DownloadState(strings[0].toLong(), strings[1].toLong(), strings[2].toInt(), strings[3].toLong())
            } catch (e: Exception) {
                DownloadState(-1, -1, -1, System.currentTimeMillis())
            }
        }
    }
}