package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object HlsRecorder {
    private const val TAG = "HlsRecorder"
    private val client = OkHttpClient()

    @Volatile
    private var isRecording = false

    fun stop() {
        isRecording = false
    }

    suspend fun record(
        context: Context,
        m3u8Url: String,
        outputFile: File,
        onProgress: (Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        isRecording = true
        var fileOutputStream: FileOutputStream? = null
        val downloadedSegments = mutableSetOf<String>()

        try {
            outputFile.parentFile?.mkdirs()
            fileOutputStream = FileOutputStream(outputFile, true)
            var totalBytesWritten = 0L

            while (isRecording) {
                val segments = fetchSegmentUrls(m3u8Url)
                if (segments.isEmpty()) {
                    // Fallback to downloading direct m3u8 stream bytes if no segments parsed
                    val rawRequest = Request.Builder().url(m3u8Url).build()
                    client.newCall(rawRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val bodyBytes = response.body?.bytes()
                            if (bodyBytes != null) {
                                fileOutputStream.write(bodyBytes)
                                totalBytesWritten += bodyBytes.size
                                withContext(Dispatchers.Main) { onProgress(totalBytesWritten) }
                            }
                        }
                    }
                    delay(3000)
                    continue
                }

                for (segmentUrl in segments) {
                    if (!isRecording) break
                    if (downloadedSegments.contains(segmentUrl)) {
                        continue
                    }

                    try {
                        val segmentRequest = Request.Builder().url(segmentUrl).build()
                        client.newCall(segmentRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body
                                if (body != null) {
                                    val bytes = body.bytes()
                                    fileOutputStream.write(bytes)
                                    fileOutputStream.flush()
                                    totalBytesWritten += bytes.size
                                    downloadedSegments.add(segmentUrl)
                                    withContext(Dispatchers.Main) { onProgress(totalBytesWritten) }
                                    Log.d(TAG, "Downloaded segment: $segmentUrl. Written: ${bytes.size} bytes.")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error downloading segment: $segmentUrl", e)
                    }
                }
                // HLS chunks are typically 2-6 seconds long, poll matching intervals
                delay(4000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording failure", e)
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            isRecording = false
        }
    }

    private fun fetchSegmentUrls(m3u8Url: String): List<String> {
        val urls = mutableListOf<String>()
        try {
            val request = Request.Builder().url(m3u8Url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyString = response.body?.string() ?: return emptyList()
                val baseUri = m3u8Url.substringBeforeLast("/")

                val lines = bodyString.lineSequence().map { it.trim() }.toList()

                // Check if it's a Master Playlist (contains other .m3u8 files)
                val m3u8Links = lines.filter { line ->
                    line.isNotEmpty() && !line.startsWith("#") && line.contains(".m3u8", ignoreCase = true)
                }

                if (m3u8Links.isNotEmpty()) {
                    // It's a master playlist! Resolve the first child playlist
                    val childLink = m3u8Links.first()
                    val fullChildUrl = if (childLink.startsWith("http://") || childLink.startsWith("https://")) {
                        childLink
                    } else if (childLink.startsWith("/")) {
                        val domainUrl = m3u8Url.substringBefore("//") + "//" + m3u8Url.substringAfter("//").substringBefore("/")
                        "$domainUrl$childLink"
                    } else {
                        "$baseUri/$childLink"
                    }
                    // Recursive call to fetch actual segments from the child m3u8
                    return fetchSegmentUrls(fullChildUrl)
                }

                for (line in lines) {
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue
                    }
                    // This line is a segment URL
                    val fullUrl = if (line.startsWith("http://") || line.startsWith("https://")) {
                        line
                    } else if (line.startsWith("/")) {
                        val domainUrl = m3u8Url.substringBefore("//") + "//" + m3u8Url.substringAfter("//").substringBefore("/")
                        "$domainUrl$line"
                    } else {
                        "$baseUri/$line"
                    }
                    if (!fullUrl.contains(".m3u8", ignoreCase = true)) {
                        urls.add(fullUrl)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching manifest from $m3u8Url", e)
        }
        return urls
    }
}
