package nl.viter.glider.shared.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun openLink(context: Context, url: String, customTabs: Boolean) {
    val uri = Uri.parse(url)
    if (uri.scheme !in listOf("http", "https")) return
    if (customTabs) {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
