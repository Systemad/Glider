package nl.viter.glider.shared.data

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import org.json.JSONArray

/** Reads the legacy plugin's private preferences once; leaves originals untouched. */
fun importFlutterPreferences(context: Context): Preferences {
    val legacy = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE).all
    fun value(key: String) = legacy["flutter.$key"] ?: legacy[key]
    fun bool(key: String, default: Boolean) = value(key) as? Boolean ?: default
    fun string(key: String, default: String) = value(key) as? String ?: default
    fun strings(key: String): Set<String> {
        val raw = value(key) ?: return emptySet()
        if (raw is Set<*>) return raw.filterIsInstance<String>().toSet()
        if (raw !is String) return emptySet()
        return when {
            raw.startsWith("VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu") -> {
                val bytes = Base64.decode(
                    raw.removePrefix("VGhpcyBpcyB0aGUgcHJlZml4IGZvciBhIGxpc3Qu"),
                    Base64.DEFAULT
                )
                ObjectInputStream(ByteArrayInputStream(bytes)).use {
                    (it.readObject() as List<*>).filterIsInstance<String>().toSet()
                }
            }

            raw.startsWith("[") -> JSONArray(raw).let { array ->
                (0 until array.length()).map { array.getString(it) }.toSet()
            }

            else -> emptySet()
        }
    }
    fun ids(key: String) = strings(key).mapNotNull { it.toLongOrNull() }.toSet()
    return Preferences(
        theme = string(
            "theme_mode",
            "system"
        ),
        dynamic = bool("use_dynamic_theme", true), pure = bool("use_pure_background", false),
        large = bool(
            "use_large_story_style",
            true
        ),
        avatars = bool("show_user_avatars", true), metadata = bool("show_story_metadata", true),
        favicons = bool(
            "show_favicons",
            true
        ),
        actions = bool("use_action_buttons", false), jobs = bool("show_jobs", true),
        threadNavigation = bool(
            "use_thread_navigation",
            true
        ),
        downvoting = bool("enable_downvoting", false),
        customTabs = bool(
            "use_in_app_browser",
            false
        ),
        drawer = bool("use_navigation_drawer", false),
        lines = (value("story_lines") as? Number)?.toInt() ?: 2, font = string("font", "Noto Sans"),
        variant =
            mapOf(
                "tonalSpot" to "Tonal spot",
                "fruitSalad" to "Fruit salad"
            )[string("theme_variant", "tonalSpot")]
                ?: string("theme_variant", "tonalSpot").replaceFirstChar { it.uppercase() },
        seed = (value("theme_color") as? Number)?.toLong() ?: 0xff6750a4,
        words = strings(
            "word_filters"
        ),
        domains = strings(
            "domain_filters"
        ),
        favorites = ids("favorited"), visited = ids("visited"),
        blocked = strings(
            "blocked"
        ),
        upvoted = ids("upvoted"), downvoted = ids("downvoted"), flagged = ids("flagged")
    )
}
