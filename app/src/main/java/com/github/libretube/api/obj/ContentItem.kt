package com.github.libretube.api.obj

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val url: String? = null,
    val type: String? = null,
    val thumbnail: String? = null,
    val uploaderName: String? = null,
    val uploaded: Long? = null,
    val shortDescription: String? = null,
    // Video only attributes
    val title: String? = null,
    val uploaderUrl: String? = null,
    val uploaderAvatar: String? = null,
    val uploadedDate: String? = null,
    val duration: Long = -1,
    val views: Long = -1,
    val isShort: Boolean? = null,
    val uploaderVerified: Boolean? = null,
    // Channel and Playlist attributes
    val name: String? = null,
    val description: String? = null,
    val subscribers: Long = -1,
    val videos: Long = -1,
    val verified: Boolean? = null
)
