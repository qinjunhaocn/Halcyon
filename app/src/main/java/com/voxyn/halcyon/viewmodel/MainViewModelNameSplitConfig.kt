package com.voxyn.halcyon.viewmodel

import com.voxyn.halcyon.data.NameSplitConfigStore
import com.voxyn.halcyon.data.SettingsManager
import com.voxyn.halcyon.data.parseNameSeparatorSetting
import com.voxyn.halcyon.data.parseNameSplitSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchNameSplitConfigObservers(
    settingsManager: SettingsManager,
    onNameSplitConfigChanged: () -> Unit,
    onAlbumIdentityConfigChanged: suspend () -> Unit
) {
    launch {
        settingsManager.artistSeparators.distinctUntilChanged().collect {
            NameSplitConfigStore.artistCustomSeparators = parseNameSeparatorSetting(it)
            onNameSplitConfigChanged()
            // Bump after caches were cleared so re-keyed UI groupings read the new rules (#675).
            NameSplitConfigStore.notifyChanged()
        }
    }
    launch {
        settingsManager.artistProtectedNames.distinctUntilChanged().collect {
            NameSplitConfigStore.artistProtectedNames = parseNameSplitSetting(it)
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
        }
    }
    launch {
        settingsManager.genreSeparators.distinctUntilChanged().collect {
            NameSplitConfigStore.genreCustomSeparators = parseNameSeparatorSetting(it)
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
        }
    }
    launch {
        settingsManager.genreProtectedNames.distinctUntilChanged().collect {
            NameSplitConfigStore.genreProtectedNames = parseNameSplitSetting(it)
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
        }
    }
    launch {
        settingsManager.tagIgnoreCase.distinctUntilChanged().collect {
            NameSplitConfigStore.tagIgnoreCase = it
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
            // Album identity is the only derived library model affected by this setting.
            // Do not re-aggregate the entire library for unrelated preference writes.
            onAlbumIdentityConfigChanged()
        }
    }
    launch {
        settingsManager.albumMergeFields.distinctUntilChanged().collect { raw ->
            NameSplitConfigStore.albumMergeFields =
                com.voxyn.halcyon.data.CustomMetadataCategory.ALBUM_MERGE_FIELD_ORDER.filter { field ->
                    field == com.voxyn.halcyon.data.CustomMetadataCategory.MERGE_ALBUM || field in raw.split(',')
                }
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
            onAlbumIdentityConfigChanged()
        }
    }
    launch {
        settingsManager.parseFeaturedArtists.distinctUntilChanged().collect {
            NameSplitConfigStore.parseFeaturedArtists = it
            onNameSplitConfigChanged()
            NameSplitConfigStore.notifyChanged()
        }
    }
}
