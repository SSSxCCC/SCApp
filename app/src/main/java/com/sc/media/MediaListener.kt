package com.sc.media

import android.media.MediaPlayer

interface MediaListener : MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnCompletionListener {
    fun onProgress(position: Int, duration: Int)
}