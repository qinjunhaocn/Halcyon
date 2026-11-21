package com.voxyn.halcyon.ui.player

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.voxyn.halcyon.R
import com.voxyn.halcyon.data.exception.WritePermissionRequiredException
import com.voxyn.halcyon.data.model.Song
import com.voxyn.halcyon.ui.components.ConfirmDangerDialog
import com.voxyn.halcyon.ui.components.EllaMiuixBottomSheet
import com.voxyn.halcyon.ui.components.RatingSheet
import com.voxyn.halcyon.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun PlayerLibraryActionSheets(
    context: Context,
    scope: CoroutineScope,
    mainViewModel: MainViewModel,
    ratingSheetSong: Song?,
    onRatingSheetSongChange: (Song?) -> Unit,
    deleteConfirmSong: Song?,
    onDeleteConfirmSongChange: (Song?) -> Unit,
    onWritePermissionRequired: (WritePermissionRequiredException, suspend () -> Unit) -> Unit
) {
    ratingSheetSong?.let { currentSong ->
        EllaMiuixBottomSheet(
            show = true,
            enableNestedScroll = false,
            title = stringResource(R.string.song_more_rating_title),
            onDismissRequest = { onRatingSheetSongChange(null) }
        ) {
            RatingSheet(
                currentRating = mainViewModel.getSongRating(currentSong),
                onRatingSelected = { rating ->
                    // The save button commits the staged star selection before closing the sheet.
                    onRatingSheetSongChange(null)
                    scope.launch {
                        val result = mainViewModel.writeSongRating(currentSong, rating)
                        if (result.isSuccess) {
                            Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                        } else {
                            val error = result.exceptionOrNull()
                            if (error is WritePermissionRequiredException) {
                                onWritePermissionRequired(error) {
                                    val retryResult = mainViewModel.writeSongRating(currentSong, rating)
                                    if (retryResult.isSuccess) {
                                        Toast.makeText(context, context.getString(R.string.song_more_rating_saved), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            retryResult.exceptionOrNull()?.localizedMessage
                                                ?: context.getString(R.string.song_more_rating_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(
                                    context,
                                    error?.localizedMessage ?: context.getString(R.string.song_more_rating_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            )
        }
    }

    ConfirmDangerDialog(
        show = deleteConfirmSong != null,
        title = stringResource(R.string.song_more_delete_song_title),
        message = deleteConfirmSong?.let {
            context.getString(
                R.string.song_more_delete_song_message,
                it.title.ifBlank { it.fileName.ifBlank { context.getString(R.string.common_this_song) } }
            )
        }.orEmpty(),
        confirmText = stringResource(R.string.song_more_delete_permanently),
        onDismiss = { onDeleteConfirmSongChange(null) },
        onConfirm = {
            val currentSong = deleteConfirmSong ?: return@ConfirmDangerDialog
            onDeleteConfirmSongChange(null)
            scope.launch {
                val result = mainViewModel.deleteSongsResult(listOf(currentSong))
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                } else {
                    val error = result.exceptionOrNull()
                    if (error is WritePermissionRequiredException) {
                        onWritePermissionRequired(error) {
                            mainViewModel.removeSongsFromLibrary(listOf(currentSong))
                            Toast.makeText(context, context.getString(R.string.library_deleted_songs, 1), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            error?.localizedMessage ?: context.getString(R.string.song_more_metadata_save_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    )
}
