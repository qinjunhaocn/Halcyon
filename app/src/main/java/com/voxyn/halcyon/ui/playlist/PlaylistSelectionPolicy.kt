package com.voxyn.halcyon.ui.playlist

internal fun shouldSelectPlaylistOnLongPress(
    selectionMode: Boolean,
    alreadySelected: Boolean
): Boolean = !selectionMode || !alreadySelected
