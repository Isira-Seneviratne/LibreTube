package com.github.libretube.api

import android.content.Context
import com.github.libretube.R
import com.github.libretube.api.obj.ChapterSegment
import com.github.libretube.api.obj.Message
import com.github.libretube.api.obj.MetaInfo
import com.github.libretube.api.obj.PipedStream
import com.github.libretube.api.obj.PreviewFrames
import com.github.libretube.api.obj.StreamItem
import com.github.libretube.api.obj.Streams
import com.github.libretube.api.obj.Subtitle
import com.github.libretube.extensions.toID
import com.github.libretube.helpers.PlayerHelper
import com.github.libretube.ui.dialogs.ShareDialog.Companion.YOUTUBE_FRONTEND_URL
import com.github.libretube.util.deArrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.datetime.toKotlinInstant
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.VideoStream
import retrofit2.HttpException
import java.io.IOException

fun VideoStream.toPipedStream() = PipedStream(
    url = content,
    codec = codec,
    format = format?.toString(),
    height = height,
    width = width,
    quality = getResolution(),
    mimeType = format?.mimeType,
    bitrate = bitrate,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    fps = fps,
    contentLength = itagItem?.contentLength ?: 0L
)

fun AudioStream.toPipedStream() = PipedStream(
    url = content,
    format = format?.toString(),
    quality = "$averageBitrate bits",
    bitrate = bitrate,
    mimeType = format?.mimeType,
    initStart = initStart,
    initEnd = initEnd,
    indexStart = indexStart,
    indexEnd = indexEnd,
    contentLength = itagItem?.contentLength ?: 0L,
    codec = codec,
    audioTrackId = audioTrackId,
    audioTrackName = audioTrackName,
    audioTrackLocale = audioLocale?.toLanguageTag(),
    audioTrackType = audioTrackType?.name,
    videoOnly = false
)

fun StreamInfoItem.toStreamItem(
    uploaderAvatarUrl: String? = null
) = StreamItem(
    type = StreamItem.TYPE_STREAM,
    url = url.toID(),
    title = name,
    uploaded = uploadDate?.offsetDateTime()?.toEpochSecond()?.times(1000) ?: -1,
    uploadedDate = textualUploadDate ?: uploadDate?.offsetDateTime()?.toLocalDateTime()
        ?.toLocalDate()
        ?.toString(),
    uploaderName = uploaderName,
    uploaderUrl = uploaderUrl.toID(),
    uploaderAvatar = uploaderAvatarUrl ?: uploaderAvatars.maxByOrNull { it.height }?.url,
    thumbnail = thumbnails.maxByOrNull { it.height }?.url,
    duration = duration,
    views = viewCount,
    uploaderVerified = isUploaderVerified,
    shortDescription = shortDescription,
    isShort = isShortFormContent
)

object StreamsExtractor {
    suspend fun extractStreams(videoId: String): Streams = withContext(Dispatchers.IO) {
        if (!PlayerHelper.disablePipedProxy || !PlayerHelper.localStreamExtraction) {
            return@withContext RetrofitInstance.api.getStreams(videoId).deArrow(videoId)
        }

        val respAsync = async {
            StreamInfo.getInfo("$YOUTUBE_FRONTEND_URL/watch?v=$videoId")
        }
        val dislikesAsync = async {
            if (PlayerHelper.localRYD) runCatching {
                RetrofitInstance.externalApi.getVotes(videoId).dislikes
            }.getOrElse { -1 } else -1
        }
        val (resp, dislikes) = Pair(respAsync.await(), dislikesAsync.await())

        Streams(
            title = resp.name,
            description = resp.description.content,
            uploader = resp.uploaderName,
            uploaderAvatar = resp.uploaderAvatars.maxBy { it.height }.url,
            uploaderUrl = resp.uploaderUrl.toID(),
            uploaderVerified = resp.isUploaderVerified,
            uploaderSubscriberCount = resp.uploaderSubscriberCount,
            category = resp.category,
            views = resp.viewCount,
            likes = resp.likeCount,
            dislikes = dislikes,
            license = resp.licence,
            hls = resp.hlsUrl,
            dash = resp.dashMpdUrl,
            tags = resp.tags,
            metaInfo = resp.metaInfo.map {
                MetaInfo(
                    it.title,
                    it.content.content,
                    it.urls.map { url -> url.toString() },
                    it.urlTexts
                )
            },
            visibility = resp.privacy.name.lowercase(),
            duration = resp.duration,
            uploadTimestamp = resp.uploadDate.offsetDateTime().toInstant().toKotlinInstant(),
            uploaded = resp.uploadDate.offsetDateTime().toEpochSecond() * 1000,
            thumbnailUrl = resp.thumbnails.maxBy { it.height }.url,
            relatedStreams = resp.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .map { item -> item.toStreamItem() },
            chapters = resp.streamSegments.map {
                ChapterSegment(
                    title = it.title,
                    image = it.previewUrl.orEmpty(),
                    start = it.startTimeSeconds.toLong()
                )
            },
            audioStreams = resp.audioStreams.map { it.toPipedStream() },
            videoStreams = resp.videoOnlyStreams.map { it.toPipedStream().copy(videoOnly = true) } +
                    resp.videoStreams.map { it.toPipedStream().copy(videoOnly = false) },
            previewFrames = resp.previewFrames.map {
                PreviewFrames(
                    it.urls,
                    it.frameWidth,
                    it.frameHeight,
                    it.totalCount,
                    it.durationPerFrame.toLong(),
                    it.framesPerPageX,
                    it.framesPerPageY
                )
            },
            subtitles = resp.subtitles.map {
                Subtitle(
                    it.content,
                    it.format?.mimeType,
                    it.displayLanguageName,
                    it.languageTag,
                    it.isAutoGenerated
                )
            }
        ).deArrow(videoId)
    }

    fun getExtractorErrorMessageString(context: Context, exception: Exception): String {
        return when (exception) {
            is IOException -> context.getString(R.string.unknown_error)
            is HttpException -> exception.response()?.errorBody()?.string()?.runCatching {
                JsonHelper.json.decodeFromString<Message>(this).message
            }?.getOrNull() ?: context.getString(R.string.server_error)

            else -> exception.localizedMessage.orEmpty()
        }
    }
}