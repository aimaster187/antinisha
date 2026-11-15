package com.example.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Strictly opt-in Accessibility Service.
 * Never monitors or records screens passively; only captures visible text snapshot
 * on-demand when the user taps "Analyze Screen" inside NISHA AI.
 */
class NishaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally passive: no background recording or tracking.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun readCurrentScreenSnapshot(): String {
        val root = rootInActiveWindow ?: return ""
        val builder = StringBuilder()
        collectVisibleText(root, builder, depth = 0)
        return builder.toString().trim().take(3500)
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo?, out: StringBuilder, depth: Int) {
        if (node == null || depth > 14 || out.length >= 3500) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrBlank()) out.appendLine(text)
        else if (!desc.isNullOrBlank()) out.appendLine(desc)
        for (i in 0 until node.childCount) {
            collectVisibleText(node.getChild(i), out, depth + 1)
        }
    }

    companion object {
        @Volatile
        private var instance: NishaAccessibilityService? = null

        fun isEnabled(): Boolean = instance != null

        fun captureScreenOnDemand(): String? = instance?.readCurrentScreenSnapshot()
    }
}
