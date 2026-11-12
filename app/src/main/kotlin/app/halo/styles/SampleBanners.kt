package app.halo.styles

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import app.halo.MainActivity
import app.halo.R
import app.halo.notify.Banner
import app.halo.notify.BannerAction

/** Sample banners for previews. Their intents open Halo, so previews run the real code path. */
enum class Sample(val label: String) { MESSAGE("Message"), CALL("Call"), FLIGHT("Flight") }

fun sampleBanner(context: Context, sample: Sample): Banner = when (sample) {
    Sample.MESSAGE -> sample(
        context, "halo.sample.message", "Messages", "Sam",
        "Table's booked for 8. I'm running ten minutes late, order without me.",
        listOf(
            BannerAction("Reply", openHalo(context), RemoteInput.Builder("halo_reply").setLabel("Reply").build()),
            BannerAction("Mark as read", openHalo(context), null),
        ),
        accent = 0xFF1D9E75.toInt(),
    )
    Sample.CALL -> sample(
        context, "halo.sample.call", "Phone", "Priya Nair", "Incoming voice call",
        listOf(BannerAction("Decline", openHalo(context), null), BannerAction("Answer", openHalo(context), null)),
        accent = 0xFF378ADD.toInt(),
    )
    Sample.FLIGHT -> sample(
        context, "halo.sample.flight", "Trips", "Flight HL 204 to Lisbon",
        "Boarding now at gate 12. Your boarding pass is ready.",
        listOf(BannerAction("View pass", openHalo(context), null), BannerAction("Dismiss", openHalo(context), null)),
        accent = 0xFF7F77DD.toInt(),
    )
}

private fun sample(
    context: Context,
    key: String,
    app: String,
    title: String,
    text: String,
    actions: List<BannerAction>,
    accent: Int,
) = Banner(
    key = key,
    packageName = context.packageName,
    appLabel = app,
    title = title,
    text = text,
    icon = context.getDrawable(R.mipmap.ic_launcher)!!,
    actions = actions,
    contentIntent = openHalo(context),
    autoCancel = false,
    sticky = false,
    accentColor = accent,
)

private fun openHalo(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
    PendingIntent.FLAG_IMMUTABLE,
)
