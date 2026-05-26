package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChannelRepository
import com.example.data.Evento
import com.example.data.HlsRecorder
import com.example.data.RecordingFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class VidexViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Eventos UI States
    private val _eventsState = MutableStateFlow<List<Evento>>(emptyList())
    val eventsState: StateFlow<List<Evento>> = _eventsState.asStateFlow()

    private val _isLoadingEvents = MutableStateFlow(false)
    val isLoadingEvents: StateFlow<Boolean> = _isLoadingEvents.asStateFlow()

    // Grabaciones UI States
    private val _recordingsState = MutableStateFlow<List<RecordingFile>>(emptyList())
    val recordingsState: StateFlow<List<RecordingFile>> = _recordingsState.asStateFlow()

    // Recording Controller States
    private val _isCurrentlyRecording = MutableStateFlow(false)
    val isCurrentlyRecording: StateFlow<Boolean> = _isCurrentlyRecording.asStateFlow()

    private val _recordedBytes = MutableStateFlow(0L) // Tracks sizing in realtime
    val recordedBytes: StateFlow<Long> = _recordedBytes.asStateFlow()

    // Preferences
    private val sharedPrefs = context.getSharedPreferences("videx_prefs", Context.MODE_PRIVATE)

    // Favorites UI States
    private val _favoritesState = MutableStateFlow<Set<String>>(
        sharedPrefs.getStringSet("pref_favorites", emptySet()) ?: emptySet()
    )
    val favoritesState: StateFlow<Set<String>> = _favoritesState.asStateFlow()

    fun toggleFavorite(channelName: String) {
        val current = _favoritesState.value.toMutableSet()
        if (current.contains(channelName)) {
            current.remove(channelName)
        } else {
            current.add(channelName)
        }
        _favoritesState.value = current
        sharedPrefs.edit().putStringSet("pref_favorites", current).apply()
    }

    private val _defaultQuality = MutableStateFlow(sharedPrefs.getString("pref_quality", "Auto") ?: "Auto")
    val defaultQuality: StateFlow<String> = _defaultQuality.asStateFlow()

    private val _networkBuffer = MutableStateFlow(sharedPrefs.getString("pref_buffer", "30s") ?: "30s")
    val networkBuffer: StateFlow<String> = _networkBuffer.asStateFlow()

    private val _autoPlay = MutableStateFlow(sharedPrefs.getBoolean("pref_autoplay", true))
    val autoPlay: StateFlow<Boolean> = _autoPlay.asStateFlow()

    init {
        loadEvents()
        refreshRecordings()
    }

    fun loadEvents() {
        viewModelScope.launch {
            _isLoadingEvents.value = true
            val fetched = ChannelRepository.fetchLiveEvents()
            _eventsState.value = fetched
            _isLoadingEvents.value = false
        }
    }

    fun refreshRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.getExternalFilesDir(null), "VIDEX/Grabaciones")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val filesList = dir.listFiles()
                ?.filter { it.extension == "mp4" || it.extension == "ts" }
                ?.sortedByDescending { it.lastModified() }
                ?.map { file ->
                    RecordingFile(
                        name = file.name,
                        absolutePath = file.absolutePath,
                        lastModified = file.lastModified(),
                        length = file.length()
                    )
                } ?: emptyList()

            withContext(Dispatchers.Main) {
                _recordingsState.value = filesList
            }
        }
    }

    fun startRecordingStream(m3u8Url: String, customTitle: String) {
        if (_isCurrentlyRecording.value) return
        _isCurrentlyRecording.value = true
        _recordedBytes.value = 0L

        viewModelScope.launch(Dispatchers.IO) {
            val cleanTitle = customTitle.replace(Regex("[^a-zA-Z0-9]"), "_")
            val dir = File(context.getExternalFilesDir(null), "VIDEX/Grabaciones")
            val recordFile = File(dir, "grabacion_${cleanTitle}_${System.currentTimeMillis()}.ts")

            HlsRecorder.record(context, m3u8Url, recordFile) { bytes ->
                _recordedBytes.value = bytes
            }
            // Once recording finishes or gets stopped, refresh the list
            withContext(Dispatchers.Main) {
                _isCurrentlyRecording.value = false
                refreshRecordings()
            }
        }
    }

    fun stopRecordingStream() {
        HlsRecorder.stop()
        _isCurrentlyRecording.value = false
        refreshRecordings()
    }

    fun deleteRecording(filePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists() && file.delete()) {
                refreshRecordings()
            }
        }
    }

    fun setQuality(quality: String) {
        _defaultQuality.value = quality
        sharedPrefs.edit().putString("pref_quality", quality).apply()
    }

    fun setNetworkBuffer(buffer: String) {
        _networkBuffer.value = buffer
        sharedPrefs.edit().putString("pref_buffer", buffer).apply()
    }

    fun setAutoPlay(enabled: Boolean) {
        _autoPlay.value = enabled
        sharedPrefs.edit().putBoolean("pref_autoplay", enabled).apply()
    }
}
