package com.github.libretube.extensions

import android.app.PictureInPictureParams
import android.util.Rational
import androidx.media3.common.VideoSize

fun PictureInPictureParams.Builder.setAspectRatio(videoSize: VideoSize) = apply {
    val ratio = (videoSize.width.toFloat() / videoSize.height)
    val rational = when {
        ratio.isNaN() -> Rational(4, 3)
        ratio <= 0.418410 -> Rational(41841, 100000)
        ratio >= 2.390000 -> Rational(239, 100)
        else -> Rational(videoSize.width, videoSize.height)
    }
    setAspectRatio(rational)
}
