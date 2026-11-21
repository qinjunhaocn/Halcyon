package com.voxyn.halcyon.data.exception

import android.content.IntentSender

class WritePermissionRequiredException(val intentSender: IntentSender) : Exception("Write permission required")
