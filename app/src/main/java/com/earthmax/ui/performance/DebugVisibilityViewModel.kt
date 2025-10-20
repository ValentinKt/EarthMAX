package com.earthmax.ui.performance

import androidx.lifecycle.ViewModel
import com.earthmax.core.debug.DebugVisibilityManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DebugVisibilityViewModel @Inject constructor(
    val debugVisibilityManager: DebugVisibilityManager
) : ViewModel()