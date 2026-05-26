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

                val lines = bodyString.lineSequence()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue
                    }
                    // This line is a segment URL
                    val fullUrl = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                        trimmed
                    } else if (trimmed.startsWith("/")) {
                        val domainUrl = m3u8Url.substringBefore("//") + "//" + m3u8Url.substringAfter("//").substringBefore("/")
                        "$domainUrl$trimmed"
                    } else {
                        "$baseUri/$trimmed"
                    }
                    urls.add(fullUrl)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching manifest from $m3u8Url", e)
        }
        return urls
    }
}
