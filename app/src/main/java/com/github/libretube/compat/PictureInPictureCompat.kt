package com.github.libretube.compat

import android.content.Context
import android.content.pm.PackageManager

object PictureInPictureCompat {
    fun isPictureInPictureAvailable(context: Context) =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}
