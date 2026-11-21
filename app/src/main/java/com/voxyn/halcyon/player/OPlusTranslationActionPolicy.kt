package com.voxyn.halcyon.player

import com.voxyn.halcyon.data.SettingsManager

internal object OPlusTranslationActionPolicy {
    fun shouldPublish(
        colorOsLyricEnabled: Boolean,
        deliveryMode: Int,
        lyricInfoJson: String?
    ): Boolean {
        return colorOsLyricEnabled &&
            deliveryMode == SettingsManager.OPLUS_LYRIC_MODE_MODULE &&
            OPlusLyricPayload.hasTranslation(lyricInfoJson)
    }
}
