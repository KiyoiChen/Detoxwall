package com.example.enforcement

import android.content.Context
import android.graphics.PixelFormat
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Full-screen `TYPE_ACCESSIBILITY_OVERLAY` window hosting Compose content. It needs no overlay
 * permission and isn't tied to an activity, so Home/Back/Recents can't dismiss it.
 * [context] must be the accessibility service.
 */
class LockOverlay(
    private val context: Context,
    private val content: @Composable (Decision) -> Unit,
) : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val current = mutableStateOf<Decision?>(null)
    private var view: ComposeView? = null

    init {
        savedState.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun show(decision: Decision) {
        current.value = decision
        if (view != null) return
        val v = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@LockOverlay)
            setViewTreeSavedStateRegistryOwner(this@LockOverlay)
            setContent { current.value?.let { content(it) } }
        }
        val params = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { windowManager.addView(v, params); view = v }
    }

    fun hide() {
        val v = view ?: return
        view = null
        current.value = null
        runCatching { windowManager.removeView(v) }
        v.disposeComposition()
    }

    fun destroy() {
        hide()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
