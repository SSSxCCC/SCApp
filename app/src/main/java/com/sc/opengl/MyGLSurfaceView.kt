package com.sc.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class MyGLSurfaceView(context: Context, attrs: AttributeSet) : GLSurfaceView(context, attrs) {

    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)

        renderer = MyGLRenderer()
        setRenderer(renderer)

        renderMode = RENDERMODE_WHEN_DIRTY
    }
}