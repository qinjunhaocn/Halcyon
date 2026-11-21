package com.voxyn.halcyon.ui.components

import com.voxyn.halcyon.data.model.LyricLine

internal fun miniPlayerBackingText(line: LyricLine?): String? =
    line?.backgroundText?.takeIf { text -> text.isNotBlank() && text.any { it.isLetterOrDigit() } }

internal fun miniPlayerAlignEnd(agent: String?): Boolean = agent.equals("v2", true)
