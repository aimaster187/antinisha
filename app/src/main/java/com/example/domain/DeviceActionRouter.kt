package com.example.domain

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import java.net.URLEncoder

data class BatteryTelemetry(
    val levelPercent: Int,
    val isCharging: Boolean,
    val statusLabel: String
)

data class PendingSensitiveAction(
    val title: String,
    val description: String,
    val actionType: String,
    val target: String,
    val payload: String
)

class DeviceActionRouter(private val context: Context) {

    fun readBatteryTelemetry(): BatteryTelemetry {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        val charging = bm?.isCharging ?: false
        return BatteryTelemetry(
            levelPercent = level,
            isCharging = charging,
            statusLabel = if (charging) "$level% • Charging" else "$level%"
        )
    }

    fun listInstalledLauncherApps(): List<Pair<String, String>> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return apps.mapNotNull { resolve ->
            val pkg = resolve.activityInfo?.packageName ?: return@mapNotNull null
            val label = resolve.loadLabel(pm)?.toString() ?: pkg
            label to pkg
        }.distinctBy { it.second }.sortedBy { it.first.lowercase() }
    }

    fun openInstalledApp(appQuery: String): Result<String> = runCatching {
        val pm = context.packageManager
        val match = listInstalledLauncherApps().firstOrNull { (label, pkg) ->
            label.contains(appQuery, ignoreCase = true) || pkg.contains(appQuery, ignoreCase = true)
        } ?: error("App '$appQuery' not found among installed launcher apps.")

        val launchIntent = pm.getLaunchIntentForPackage(match.second)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            ?: error("Unable to launch ${match.first}.")
        context.startActivity(launchIntent)
        "Opened ${match.first}"
    }

    fun proposeVoiceOrDeviceAction(commandText: String): PendingSensitiveAction? {
        val lower = commandText.trim().lowercase()
        return when {
            lower.startsWith("call ") -> {
                val target = commandText.substring(5).trim()
                PendingSensitiveAction(
                    title = "Confirm Phone Dialer",
                    description = "Open Android Dialer prepared for '$target'. You still tap call inside the Dialer.",
                    actionType = "DIAL",
                    target = target,
                    payload = ""
                )
            }
            lower.startsWith("whatsapp ") || "send whatsapp" in lower -> {
                PendingSensitiveAction(
                    title = "Confirm WhatsApp Composer",
                    description = "Prepare a WhatsApp message composer with your text. No message is sent automatically.",
                    actionType = "WHATSAPP",
                    target = "",
                    payload = commandText.replaceFirst(Regex("(?i)(send )?whatsapp\\s*"), "").trim()
                )
            }
            lower.startsWith("sms ") || "send sms" in lower -> {
                PendingSensitiveAction(
                    title = "Confirm SMS Composer",
                    description = "Open Android Messages composer with your drafted text.",
                    actionType = "SMS",
                    target = "",
                    payload = commandText.replaceFirst(Regex("(?i)(send )?sms\\s*"), "").trim()
                )
            }
            "wifi settings" in lower || "bluetooth settings" in lower || "open settings" in lower -> {
                val action = when {
                    "wifi" in lower -> Settings.ACTION_WIFI_SETTINGS
                    "bluetooth" in lower -> Settings.ACTION_BLUETOOTH_SETTINGS
                    else -> Settings.ACTION_SETTINGS
                }
                PendingSensitiveAction(
                    title = "Open System Settings",
                    description = "Open official Android Settings panel so you can adjust device settings safely.",
                    actionType = "SETTINGS",
                    target = action,
                    payload = ""
                )
            }
            "set timer" in lower -> {
                val mins = Regex("(\\d+)").find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 5
                PendingSensitiveAction(
                    title = "Confirm Timer",
                    description = "Create a $mins-minute timer in Android Clock.",
                    actionType = "TIMER",
                    target = "$mins",
                    payload = "NISHA AI Timer"
                )
            }
            else -> null
        }
    }

    fun executeConfirmedAction(action: PendingSensitiveAction): Result<String> = runCatching {
        when (action.actionType) {
            "DIAL" -> {
                val sanitized = action.target.filter { it.isDigit() || it == '+' }
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opened Dialer for ${action.target}"
            }
            "SMS" -> {
                val uri = Uri.parse("smsto:${Uri.encode(action.target)}")
                val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                    putExtra("sms_body", action.payload)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opened SMS composer"
            }
            "WHATSAPP" -> {
                launchWhatsAppShareOrChat(action.target, action.payload)
            }
            "TIMER" -> {
                val minutes = action.target.toIntOrNull()?.coerceAtLeast(1) ?: 5
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                    putExtra(AlarmClock.EXTRA_MESSAGE, action.payload.ifBlank { "NISHA AI Timer" })
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Timer ($minutes min) sent to Clock"
            }
            "SETTINGS" -> {
                val intent = Intent(action.target.ifBlank { Settings.ACTION_SETTINGS }).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opened System Settings"
            }
            else -> error("Unsupported action type: ${action.actionType}")
        }
    }

    fun launchWhatsAppShareOrChat(phoneDigits: String, message: String): String {
        val cleanPhone = phoneDigits.filter { it.isDigit() }
        val encoded = URLEncoder.encode(message, "UTF-8")
        if (cleanPhone.isNotBlank()) {
            val waIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(waIntent)
            return "Prepared WhatsApp chat for +$cleanPhone"
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(shareIntent)
            "Opened WhatsApp composer"
        } catch (_: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                },
                "Share message via"
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
            "Opened Android Share Sheet (WhatsApp not installed)"
        }
    }

    fun openExternalUrl(url: String): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openOverlayPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
