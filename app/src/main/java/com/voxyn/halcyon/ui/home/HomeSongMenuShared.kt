package com.voxyn.halcyon.ui.home

import androidx.compose.runtime.Composable
import com.voxyn.halcyon.ui.components.EllaMiuixMenuItem
import com.voxyn.halcyon.ui.components.EllaMiuixSheetHandle

@Composable
internal fun SheetHandle() {
    EllaMiuixSheetHandle()
}

@Composable
internal fun LibraryMenuItem(
    text: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    danger: Boolean = false
) {
    EllaMiuixMenuItem(text = text, onClick = onClick, subtitle = subtitle, danger = danger)
}
