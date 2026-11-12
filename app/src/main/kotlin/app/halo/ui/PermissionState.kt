package app.halo.ui

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.halo.notify.HaloListenerService

data class PermissionState(val listener: Boolean, val overlay: Boolean, val batteryExempt: Boolean)

fun readPermissionState(context: Context) = PermissionState(
    listener = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName),
    overlay = Settings.canDrawOverlays(context),
    batteryExempt = context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
)

/** Permission state, re-read every time Halo comes back to the foreground from Settings. */
@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(readPermissionState(context)) }
    LifecycleResumeEffect(Unit) {
        state = readPermissionState(context)
        onPauseOrDispose { }
    }
    return state
}

private fun Context.packageUri(pkg: String = packageName): Uri = Uri.fromParts("package", pkg, null)

private fun Context.launch(intent: Intent, fallback: Intent? = null) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        fallback?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

fun Context.openListenerSettings() = launch(
    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
        ComponentName(this, HaloListenerService::class.java).flattenToString(),
    ),
    fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
)

fun Context.openOverlaySettings() = launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri()))

fun Context.openBatterySettings() = launch(
    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri()),
    fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
)

fun Context.openAppInfo() = launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()))

fun Context.openAppNotificationSettings(pkg: String) = launch(
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg),
    fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(pkg)),
)

fun Context.openUrl(url: String) = launch(Intent(Intent.ACTION_VIEW, url.toUri()))

const val RELEASES_URL = "https://github.com/santhoshunt/halo/releases/latest"
