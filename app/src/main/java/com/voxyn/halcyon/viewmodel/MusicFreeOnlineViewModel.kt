package com.voxyn.halcyon.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.voxyn.halcyon.data.musicfree.MusicFreeOnlineSong

class MusicFreeOnlineViewModel : ViewModel() {
    internal val searchRequests = com.voxyn.halcyon.ui.online.OnlineProviderSearchRequests()
    internal var observedPluginId: String? = null
    var importUrl by mutableStateOf("")
    var searchQuery by mutableStateOf("")
    var importExpanded by mutableStateOf(false)
    var isBusy by mutableStateOf(false)
    var results by mutableStateOf<List<MusicFreeOnlineSong>>(emptyList())
    var message by mutableStateOf("")

    fun clearResults(message: String) {
        results = emptyList()
        this.message = message
    }
}
